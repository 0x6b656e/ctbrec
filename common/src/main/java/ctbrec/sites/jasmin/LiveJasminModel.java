package ctbrec.sites.jasmin;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.json.JSONObject;
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
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import ctbrec.AbstractModel;
import ctbrec.Config;
import ctbrec.io.HttpException;
import ctbrec.recorder.download.Download;
import ctbrec.recorder.download.HlsDownload;
import ctbrec.recorder.download.StreamSource;
import okhttp3.Request;
import okhttp3.Response;

public class LiveJasminModel extends AbstractModel {

    private static final transient Logger LOG = LoggerFactory.getLogger(LiveJasminModel.class);
    private String id;
    private boolean online = false;
    private int[] resolution;

    @Override
    public boolean isOnline(boolean ignoreCache) throws IOException, ExecutionException, InterruptedException {
        if(ignoreCache) {
            loadModelInfo();
        }
        return online;
    }

    protected void loadModelInfo() throws IOException {
        String url = "https://m.livejasmin.com/en/chat-html5/" + getName();
        Request req = new Request.Builder().url(url)
                .header("User-Agent", "Mozilla/5.0 (iPhone; CPU OS 10_14 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/11.1.1 Mobile/14E304 Safari/605.1.15")
                .header("Accept", "application/json,*/*")
                .header("Accept-Language", "en")
                .header("Referer", getSite().getBaseUrl())
                .header("X-Requested-With", "XMLHttpRequest")
                .build();
        try(Response response = getSite().getHttpClient().execute(req)) {
            if(response.isSuccessful()) {
                String body = response.body().string();
                JSONObject json = new JSONObject(body);
                //LOG.debug(json.toString(2));
                if(json.optBoolean("success")) {
                    JSONObject data = json.getJSONObject("data");
                    JSONObject config = data.getJSONObject("config");
                    JSONObject chatRoom = config.getJSONObject("chatRoom");
                    setId(chatRoom.getString("p_id"));
                    if(chatRoom.has("profile_picture_url")) {
                        setPreview(chatRoom.getString("profile_picture_url"));
                    }
                    int status = chatRoom.optInt("status", -1);
                    onlineState = mapStatus(status);
                    if(chatRoom.optInt("is_on_private", 0) == 1) {
                        onlineState = State.PRIVATE;
                    }
                    resolution = new int[2];
                    resolution[0] = config.optInt("streamWidth");
                    resolution[1] = config.optInt("streamHeight");
                    online = onlineState == State.ONLINE;
                    LOG.trace("{} - status:{} {} {} {}", getName(), online, onlineState, Arrays.toString(resolution), getUrl());
                } else {
                    throw new IOException("Response was not successful: " + body);
                }
            } else {
                throw new HttpException(response.code(), response.message());
            }
        }
    }

    private State mapStatus(int status) {
        switch(status) {
        case 0:
            return State.OFFLINE;
        case 1:
            return State.ONLINE;
        case 2:
        case 3:
            return State.PRIVATE;
        default:
            LOG.debug("Unkown state {} {}", status, getUrl());
            return State.UNKNOWN;
        }
    }

    @Override
    public List<StreamSource> getStreamSources() throws IOException, ExecutionException, ParseException, PlaylistException {
        String masterUrl = getMasterPlaylistUrl();
        List<StreamSource> streamSources = new ArrayList<>();
        Request req = new Request.Builder().url(masterUrl).build();
        try(Response response = site.getHttpClient().execute(req)) {
            if(response.isSuccessful()) {
                InputStream inputStream = response.body().byteStream();
                PlaylistParser parser = new PlaylistParser(inputStream, Format.EXT_M3U, Encoding.UTF_8, ParsingMode.LENIENT);
                Playlist playlist = parser.parse();
                MasterPlaylist master = playlist.getMasterPlaylist();
                streamSources.clear();
                for (PlaylistData playlistData : master.getPlaylists()) {
                    StreamSource streamsource = new StreamSource();
                    String baseUrl = masterUrl.toString();
                    baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf('/') + 1);
                    streamsource.mediaPlaylistUrl = baseUrl + playlistData.getUri();
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

    private String getMasterPlaylistUrl() throws IOException {
        loadModelInfo();
        String url = site.getBaseUrl() + "/en/stream/hls/free/" + getName();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                .addHeader("Accept", "application/json, text/javascript, */*")
                .addHeader("Accept-Language", "en")
                .addHeader("Referer", site.getBaseUrl())
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .build();
        try (Response response = site.getHttpClient().execute(request)) {
            if (response.isSuccessful()) {
                String body = response.body().string();
                JSONObject json = new JSONObject(body);
                if(json.optBoolean("success")) {
                    JSONObject data = json.getJSONObject("data");
                    JSONObject hlsStream = data.getJSONObject("hls_stream");
                    return hlsStream.getString("url");
                } else {
                    throw new IOException("Response was not successful: " + url + "\n" + body);
                }
            } else {
                throw new HttpException(response.code(), response.message());
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
        if(resolution == null) {
            if(failFast) {
                return new int[2];
            }
            try {
                loadModelInfo();
            } catch (IOException e) {
                throw new ExecutionException(e);
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void readSiteSpecificData(JsonReader reader) throws IOException {
        reader.nextName();
        id = reader.nextString();
    }

    @Override
    public void writeSiteSpecificData(JsonWriter writer) throws IOException {
        if(id == null) {
            try {
                loadModelInfo();
            } catch (IOException e) {
                LOG.error("Couldn't load model ID for {}. This can cause problems with saving / loading the model", getName());
            }
        }
        writer.name("id").value(id);
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    @Override
    public Download createDownload() {
        if(Config.getInstance().getSettings().livejasminSession.isEmpty()) {
            if(Config.isServerMode()) {
                return new HlsDownload(getSite().getHttpClient());
            } else {
                return new LiveJasminMergedHlsDownload(getSite().getHttpClient());
            }
        } else {
            return new LiveJasminWebSocketDownload(getSite().getHttpClient());
        }
    }
}
