package ctbrec.recorder.download;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iheartradio.m3u8.Encoding;
import com.iheartradio.m3u8.Format;
import com.iheartradio.m3u8.ParseException;
import com.iheartradio.m3u8.ParsingMode;
import com.iheartradio.m3u8.PlaylistException;
import com.iheartradio.m3u8.PlaylistParser;
import com.iheartradio.m3u8.data.MediaPlaylist;
import com.iheartradio.m3u8.data.Playlist;
import com.iheartradio.m3u8.data.TrackData;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.io.HttpClient;
import ctbrec.io.HttpException;
import okhttp3.Request;
import okhttp3.Response;

public abstract class AbstractHlsDownload implements Download {

    private static final transient Logger LOG = LoggerFactory.getLogger(AbstractHlsDownload.class);

    ExecutorService downloadThreadPool = Executors.newFixedThreadPool(5);
    HttpClient client;
    volatile boolean running = false;
    volatile boolean alive = true;
    Path downloadDir;
    Instant startTime;
    Model model;

    public AbstractHlsDownload(HttpClient client) {
        this.client = client;
    }

    SegmentPlaylist getNextSegments(String segments) throws IOException, ParseException, PlaylistException {
        URL segmentsUrl = new URL(segments);
        Request request = new Request.Builder().url(segmentsUrl).addHeader("connection", "keep-alive").build();
        try(Response response = client.execute(request)) {
            if(response.isSuccessful()) {
                InputStream inputStream = response.body().byteStream();
                PlaylistParser parser = new PlaylistParser(inputStream, Format.EXT_M3U, Encoding.UTF_8, ParsingMode.LENIENT);
                Playlist playlist = parser.parse();
                if(playlist.hasMediaPlaylist()) {
                    MediaPlaylist mediaPlaylist = playlist.getMediaPlaylist();
                    SegmentPlaylist lsp = new SegmentPlaylist(segments);
                    lsp.seq = mediaPlaylist.getMediaSequenceNumber();
                    lsp.targetDuration = mediaPlaylist.getTargetDuration();
                    List<TrackData> tracks = mediaPlaylist.getTracks();
                    for (TrackData trackData : tracks) {
                        String uri = trackData.getUri();
                        if(!uri.startsWith("http")) {
                            String _url = segmentsUrl.toString();
                            _url = _url.substring(0, _url.lastIndexOf('/') + 1);
                            String segmentUri = _url + uri;
                            lsp.totalDuration += trackData.getTrackInfo().duration;
                            lsp.lastSegDuration = trackData.getTrackInfo().duration;
                            lsp.segments.add(segmentUri);
                        }
                    }
                    return lsp;
                }
                return null;
            } else {
                throw new HttpException(response.code(), response.message());
            }
        }
    }


    String getSegmentPlaylistUrl(Model model) throws IOException, ExecutionException, ParseException, PlaylistException {
        List<StreamSource> streamSources = model.getStreamSources();
        String url = null;
        if(model.getStreamUrlIndex() >= 0 && model.getStreamUrlIndex() < streamSources.size()) {
            url = streamSources.get(model.getStreamUrlIndex()).getMediaPlaylistUrl();
        } else {
            Collections.sort(streamSources);
            // filter out stream resolutions, which are too high
            int maxRes = Config.getInstance().getSettings().maximumResolution;
            if(maxRes > 0) {
                for (Iterator<StreamSource> iterator = streamSources.iterator(); iterator.hasNext();) {
                    StreamSource streamSource = iterator.next();
                    if(streamSource.height > 0 && maxRes < streamSource.height) {
                        LOG.trace("Res too high {} > {}", streamSource.height, maxRes);
                        iterator.remove();
                    }
                }
            }
            if(streamSources.isEmpty()) {
                throw new ExecutionException(new RuntimeException("No stream left in playlist"));
            } else {
                url = streamSources.get(streamSources.size()-1).getMediaPlaylistUrl();
            }
        }
        return url;
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public File getDirectory() {
        return downloadDir.toFile();
    }

    @Override
    public Instant getStartTime() {
        return startTime;
    }

    @Override
    public Model getModel() {
        return model;
    }

    public static class SegmentPlaylist {
        public String url;
        public int seq = 0;
        public float totalDuration = 0;
        public float lastSegDuration = 0;
        public float targetDuration = 0;
        public List<String> segments = new ArrayList<>();

        public SegmentPlaylist(String url) {
            this.url = url;
        }
    }
}
