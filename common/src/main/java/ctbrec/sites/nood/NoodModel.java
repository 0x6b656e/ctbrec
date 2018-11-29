package ctbrec.sites.nood;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iheartradio.m3u8.Encoding;
import com.iheartradio.m3u8.Format;
import com.iheartradio.m3u8.ParseException;
import com.iheartradio.m3u8.ParsingMode;
import com.iheartradio.m3u8.PlaylistException;
import com.iheartradio.m3u8.PlaylistParser;
import com.iheartradio.m3u8.data.MasterPlaylist;
import com.iheartradio.m3u8.data.Playlist;
import com.iheartradio.m3u8.data.PlaylistData;
import com.iheartradio.m3u8.data.StreamInfo;

import ctbrec.AbstractModel;
import ctbrec.Config;
import ctbrec.io.HtmlParser;
import ctbrec.io.HttpException;
import ctbrec.recorder.download.StreamSource;
import okhttp3.Request;
import okhttp3.Response;

public class NoodModel extends AbstractModel {

    private static final transient Logger LOG = LoggerFactory.getLogger(NoodModel.class);
    private int[] resolution;
    private boolean online = false;
    private String streamUrl;
    private String onlineState;

    @Override
    public boolean isOnline(boolean ignoreCache) throws IOException, ExecutionException, InterruptedException {
        if(ignoreCache) {
            loadModelInfo();
        }
        return online;
    }

    private void loadModelInfo() throws IOException {
        Request request = new Request.Builder()
                .url(getUrl())
                .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                .addHeader("Accept-Language", "en")
                .addHeader("Referer", getSite().getBaseUrl())
                .build();
        try(Response response = getSite().getHttpClient().execute(request)) {
            if(response.isSuccessful()) {
                String body = response.body().string();

                // online?
                online = body.contains("data-source");
                onlineState = online ? "online" : "offline";

                // stream url
                Element div = HtmlParser.getTag(body, "div[data-source]");
                JSONArray sources = new JSONArray(div.attr("data-source"));
                for (int i = 0; i < sources.length(); i++) {
                    JSONObject source = sources.getJSONObject(i);
                    String type = source.optString("type");
                    String quality = source.optString("quality");
                    String src = source.getString("src");
                    if(type.equalsIgnoreCase("application/x-mpegURL") && quality.equalsIgnoreCase("auto")) {
                        streamUrl = src;
                    }
                }
            } else {
                throw new HttpException(response.code(), response.message());
            }
        }
    }

    @Override
    public String getOnlineState(boolean failFast) throws IOException, ExecutionException {
        if(failFast) {
            return Optional.ofNullable(online).orElse(false) ? "online" : "offline";
        } else {
            if(onlineState == null) {
                loadModelInfo();
            }
            return onlineState;
        }
    }

    @Override
    public List<StreamSource> getStreamSources() throws IOException, ExecutionException, ParseException, PlaylistException {
        String streamUrl = getStreamUrl();
        if (streamUrl == null) {
            return Collections.emptyList();
        }
        List<StreamSource> streamSources = new ArrayList<>();
        Request req = new Request.Builder()
                .url(streamUrl)
                .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                .addHeader("Accept-Language", "en")
                .addHeader("Referer", getSite().getBaseUrl())
                .addHeader("Origin", getSite().getBaseUrl())
                .build();
        try(Response response = site.getHttpClient().execute(req)) {
            if(response.isSuccessful()) {
                InputStream inputStream = response.body().byteStream();
                PlaylistParser parser = new PlaylistParser(inputStream, Format.EXT_M3U, Encoding.UTF_8, ParsingMode.LENIENT);
                Playlist playlist = parser.parse();
                MasterPlaylist master = playlist.getMasterPlaylist();
                for (PlaylistData playlistData : master.getPlaylists()) {
                    StreamSource streamsource = new StreamSource();
                    String urlBase = streamUrl.substring(0, streamUrl.lastIndexOf('/')+1);
                    streamsource.mediaPlaylistUrl = urlBase + playlistData.getUri();
                    if (playlistData.hasStreamInfo()) {
                        StreamInfo info = playlistData.getStreamInfo();
                        streamsource.bandwidth = info.getBandwidth();
                        streamsource.width = info.hasResolution() ? info.getResolution().width : 0;
                        streamsource.height = info.hasResolution() ? info.getResolution().height : 0;
                    } else {
                        streamsource.bandwidth = 0;
                        streamsource.width = 0;
                        streamsource.height = 0;
                    }
                    streamSources.add(streamsource);
                }
            } else {
                throw new HttpException(response.code(), response.message());
            }
        }
        return streamSources;
    }

    private String getStreamUrl() throws IOException {
        if(streamUrl == null) {
            loadModelInfo();
        }
        return streamUrl;
    }

    @Override
    public void invalidateCacheEntries() {
        resolution = null;
        streamUrl = null;
    }

    @Override
    public void receiveTip(int tokens) throws IOException {
    }

    @Override
    public int[] getStreamResolution(boolean failFast) throws ExecutionException {
        if(resolution == null) {
            if(failFast) {
                return new int[2];
            }
            try {
                if(!isOnline()) {
                    return new int[2];
                }
                List<StreamSource> streamSources = getStreamSources();
                Collections.sort(streamSources);
                StreamSource best = streamSources.get(streamSources.size()-1);
                resolution = new int[] {best.width, best.height};
            } catch (ExecutionException | IOException | ParseException | PlaylistException | InterruptedException e) {
                LOG.warn("Couldn't determine stream resolution for {} - {}", getName(), e.getMessage());
            }
            return resolution;
        } else {
            return resolution;
        }
    }

    @Override
    public boolean follow() throws IOException {
        return false;
    }

    @Override
    public boolean unfollow() throws IOException {
        return false;
    }

}
