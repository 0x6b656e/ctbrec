package ctbrec.recorder.download;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.iheartradio.m3u8.Encoding;
import com.iheartradio.m3u8.Format;
import com.iheartradio.m3u8.ParseException;
import com.iheartradio.m3u8.PlaylistException;
import com.iheartradio.m3u8.PlaylistParser;
import com.iheartradio.m3u8.data.MediaPlaylist;
import com.iheartradio.m3u8.data.Playlist;
import com.iheartradio.m3u8.data.TrackData;

import ctbrec.HttpClient;
import okhttp3.Request;
import okhttp3.Response;

public abstract class AbstractHlsDownload implements Download {

    ExecutorService downloadThreadPool = Executors.newFixedThreadPool(5);
    HttpClient client;
    volatile boolean running = false;
    volatile boolean alive = true;
    Path downloadDir;

    public AbstractHlsDownload(HttpClient client) {
        this.client = client;
    }

    SegmentPlaylist getNextSegments(String segments) throws IOException, ParseException, PlaylistException {
        URL segmentsUrl = new URL(segments);
        Request request = new Request.Builder().url(segmentsUrl).addHeader("connection", "keep-alive").build();
        Response response = client.execute(request);
        try {
            InputStream inputStream = response.body().byteStream();
            PlaylistParser parser = new PlaylistParser(inputStream, Format.EXT_M3U, Encoding.UTF_8);
            Playlist playlist = parser.parse();
            if(playlist.hasMediaPlaylist()) {
                MediaPlaylist mediaPlaylist = playlist.getMediaPlaylist();
                SegmentPlaylist lsp = new SegmentPlaylist();
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
        } finally {
            response.close();
        }
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public File getDirectory() {
        return downloadDir.toFile();
    }

    public static class SegmentPlaylist {
        public int seq = 0;
        public float totalDuration = 0;
        public float lastSegDuration = 0;
        public float targetDuration = 0;
        public List<String> segments = new ArrayList<>();
    }
}
