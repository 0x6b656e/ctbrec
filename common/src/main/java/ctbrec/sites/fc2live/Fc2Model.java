package ctbrec.sites.fc2live;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iheartradio.m3u8.Encoding;
import com.iheartradio.m3u8.Format;
import com.iheartradio.m3u8.ParseException;
import com.iheartradio.m3u8.PlaylistException;
import com.iheartradio.m3u8.PlaylistParser;
import com.iheartradio.m3u8.data.MasterPlaylist;
import com.iheartradio.m3u8.data.Playlist;
import com.iheartradio.m3u8.data.PlaylistData;
import com.iheartradio.m3u8.data.StreamInfo;

import ctbrec.AbstractModel;
import ctbrec.Config;
import ctbrec.io.HttpException;
import ctbrec.recorder.download.StreamSource;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Fc2Model extends AbstractModel {
    private static final transient Logger LOG = LoggerFactory.getLogger(Fc2Model.class);
    private String id;
    private int viewerCount;
    private boolean online;
    private String onlineState = "n/a";
    private String version;

    @Override
    public boolean isOnline(boolean ignoreCache) throws IOException, ExecutionException, InterruptedException {
        if(ignoreCache) {
            loadModelInfo();
        }
        return online;
    }

    private void loadModelInfo() throws IOException {
        String url = Fc2Live.BASE_URL + "/api/memberApi.php";
        RequestBody body = new FormBody.Builder()
                .add("channel", "1")
                .add("profile", "1")
                .add("streamid", id)
                .build();
        Request req = new Request.Builder()
                .url(url)
                .method("POST", body)
                .header("Accept", "*/*")
                .header("Accept-Language", "en-US,en;q=0.5")
                .header("Referer", Fc2Live.BASE_URL)
                .header("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                .header("X-Requested-With", "XMLHttpRequest")
                .build();
        LOG.debug("Fetching page {}", url);
        try(Response resp = getSite().getHttpClient().execute(req)) {
            if(resp.isSuccessful()) {
                String msg = resp.body().string();
                JSONObject json = new JSONObject(msg);
                JSONObject data = json.getJSONObject("data");
                JSONObject channelData = data.getJSONObject("channel_data");
                online = channelData.optInt("is_publish") == 1;
                onlineState = online ? "online" : "offline";
                version = channelData.optString("version");
            } else {
                resp.close();
                throw new IOException("HTTP status " + resp.code() + " " + resp.message());
            }
        }
    }

    @Override
    public String getOnlineState(boolean failFast) throws IOException, ExecutionException {
        if(failFast) {
            return onlineState;
        } else if(Objects.equals(onlineState, "n/a")){
            loadModelInfo();
        }
        return onlineState;
    }

    @Override
    public List<StreamSource> getStreamSources() throws IOException, ExecutionException, ParseException, PlaylistException {
        loadModelInfo();
        List<StreamSource> sources = new ArrayList<>();
        getControlToken((token, url) -> {
            url = url + "?control_token=" + token;
            LOG.debug("Session token: {}", token);
            LOG.debug("Getting playlist token over websocket {}", url);
            Fc2WebSocketClient wsClient = new Fc2WebSocketClient(url, getSite().getHttpClient());
            try {
                String playlistUrl = wsClient.getPlaylistUrl();
                LOG.debug("Paylist url {}", playlistUrl);
                sources.addAll(parseMasterPlaylist(playlistUrl));
            } catch (InterruptedException | IOException | ParseException | PlaylistException e) {
                LOG.error("Couldn't fetch stream information", e);
            }
        });
        return sources;
    }

    private List<StreamSource> parseMasterPlaylist(String playlistUrl) throws IOException, ParseException, PlaylistException {
        List<StreamSource> sources = new ArrayList<>();
        Request req = new Request.Builder()
                .url(playlistUrl)
                .header("Accept", "*/*")
                .header("Accept-Language", "en-US,en;q=0.5")
                .header("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                .header("Origin", Fc2Live.BASE_URL)
                .header("Referer", getUrl())
                .build();
        try(Response response = site.getHttpClient().execute(req)) {
            if(response.isSuccessful()) {
                InputStream inputStream = response.body().byteStream();
                PlaylistParser parser = new PlaylistParser(inputStream, Format.EXT_M3U, Encoding.UTF_8);
                Playlist playlist = parser.parse();
                MasterPlaylist master = playlist.getMasterPlaylist();
                sources.clear();
                for (PlaylistData playlistData : master.getPlaylists()) {
                    StreamSource streamsource = new StreamSource();
                    streamsource.mediaPlaylistUrl = playlistData.getUri();
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
                    sources.add(streamsource);
                }
                LOG.debug(sources.toString());
                return sources;
            } else {
                throw new HttpException(response.code(), response.message());
            }
        }
    }

    private void getControlToken(BiConsumer<String, String> callback) throws IOException {
        String url = Fc2Live.BASE_URL + "/api/getControlServer.php";
        RequestBody body = new FormBody.Builder()
                .add("channel_id", id)
                .add("channel_version", version)
                .add("client_app", "browser_hls")
                .add("client_type", "pc")
                .add("client_version", "1.6.0 [1]")
                .add("mode", "play")
                .build();
        Request req = new Request.Builder()
                .url(url)
                .method("POST", body)
                .header("Accept", "*/*")
                .header("Accept-Language", "en-US,en;q=0.5")
                .header("Referer", Fc2Live.BASE_URL)
                .header("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                .header("X-Requested-With", "XMLHttpRequest")
                .build();
        LOG.debug("Fetching page {}", url);
        try(Response resp = getSite().getHttpClient().execute(req)) {
            if(resp.isSuccessful()) {
                String msg = resp.body().string();
                JSONObject json = new JSONObject(msg);
                String wssurl = json.getString("url");
                String token = json.getString("control_token");
                callback.accept(token, wssurl);
            } else {
                resp.close();
                throw new IOException("HTTP status " + resp.code() + " " + resp.message());
            }
        }
    }

    @Override
    public void invalidateCacheEntries() {
    }

    @Override
    public void receiveTip(int tokens) throws IOException {
    }

    @Override
    public int[] getStreamResolution(boolean failFast) throws ExecutionException {
        return new int[2];
    }

    @Override
    public boolean follow() throws IOException {
        return false;
    }

    @Override
    public boolean unfollow() throws IOException {
        return false;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public int getViewerCount() {
        return viewerCount;
    }

    public void setViewerCount(int viewerCount) {
        this.viewerCount = viewerCount;
    }
}
