package ctbrec.sites.camsoda;

import static ctbrec.Model.State.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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

import ctbrec.AbstractModel;
import ctbrec.Config;
import ctbrec.io.HttpException;
import ctbrec.recorder.download.StreamSource;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CamsodaModel extends AbstractModel {

    private static final transient Logger LOG = LoggerFactory.getLogger(CamsodaModel.class);
    private String streamUrl;
    private List<StreamSource> streamSources = null;
    private float sortOrder = 0;
    int[] resolution = new int[2];

    public String getStreamUrl() throws IOException {
        if(streamUrl == null) {
            // load model
            loadModel();
        }
        return streamUrl;
    }

    private void loadModel() throws IOException {
        String modelUrl = site.getBaseUrl() + "/api/v1/user/" + getName();
        Request req = new Request.Builder().url(modelUrl).build();
        Response response = site.getHttpClient().execute(req);
        try {
            JSONObject result = new JSONObject(response.body().string());
            if(result.getBoolean("status")) {
                JSONObject chat = result.getJSONObject("user").getJSONObject("chat");
                String status = chat.getString("status");
                setOnlineStateByStatus(status);
                if(chat.has("edge_servers")) {
                    String edgeServer = chat.getJSONArray("edge_servers").getString(0);
                    String streamName = chat.getString("stream_name");
                    streamUrl = "https://" + edgeServer + "/cam/mp4:" + streamName + "_h264_aac_480p/playlist.m3u8";
                }

            } else {
                throw new IOException("Result was not ok");
            }
        } finally {
            response.close();
        }
    }

    public void setOnlineStateByStatus(String status) {
        switch(status) {
        case "online":
            onlineState = ONLINE;
            break;
        case "offline":
            onlineState = OFFLINE;
            break;
        case "connected":
            onlineState = AWAY;
            break;
        case "private":
            onlineState = PRIVATE;
            break;
        case "limited":
            onlineState = GROUP;
            break;
        default:
            LOG.debug("Unknown show type {}", status);
            onlineState = UNKNOWN;
        }
    }

    @Override
    public boolean isOnline(boolean ignoreCache) throws IOException, ExecutionException, InterruptedException {
        if(ignoreCache || onlineState == UNKNOWN) {
            loadModel();
        }
        return onlineState == ONLINE;
    }

    @Override
    public State getOnlineState(boolean failFast) throws IOException, ExecutionException {
        if(failFast) {
            return onlineState;
        } else {
            if(onlineState == UNKNOWN) {
                loadModel();
            }
            return onlineState;
        }
    }

    @Override
    public List<StreamSource> getStreamSources() throws IOException, ExecutionException, ParseException, PlaylistException {
        String streamUrl = getStreamUrl();
        if(streamUrl == null) {
            return Collections.emptyList();
        }
        Request req = new Request.Builder().url(streamUrl).build();
        Response response = site.getHttpClient().execute(req);
        try {
            InputStream inputStream = response.body().byteStream();
            PlaylistParser parser = new PlaylistParser(inputStream, Format.EXT_M3U, Encoding.UTF_8, ParsingMode.LENIENT);
            Playlist playlist = parser.parse();
            MasterPlaylist master = playlist.getMasterPlaylist();
            PlaylistData playlistData = master.getPlaylists().get(0);
            StreamSource streamsource = new StreamSource();
            streamsource.mediaPlaylistUrl = streamUrl.replace("playlist.m3u8", playlistData.getUri());
            if(playlistData.hasStreamInfo()) {
                StreamInfo info = playlistData.getStreamInfo();
                streamsource.bandwidth = info.getBandwidth();
                streamsource.width = info.hasResolution() ? info.getResolution().width : 0;
                streamsource.height = info.hasResolution() ? info.getResolution().height : 0;
            } else {
                streamsource.bandwidth = 0;
                streamsource.width = 0;
                streamsource.height = 0;
            }
            streamSources = Collections.singletonList(streamsource);
        } finally {
            response.close();
        }
        return streamSources;
    }

    @Override
    public void invalidateCacheEntries() {
        streamSources = null;
    }

    @Override
    public int[] getStreamResolution(boolean failFast) throws ExecutionException {
        if(failFast) {
            return resolution;
        } else {
            if(failFast) {
                return new int[] {0,0};
            } else {
                try {
                    List<StreamSource> streamSources = getStreamSources();
                    if(streamSources.isEmpty()) {
                        return new int[] {0,0};
                    } else {
                        StreamSource src = streamSources.get(0);
                        resolution = new int[] {src.width, src.height};
                        return resolution;
                    }
                } catch (IOException | ParseException | PlaylistException e) {
                    throw new ExecutionException(e);
                }
            }
        }
    }

    @Override
    public void receiveTip(int tokens) throws IOException {
        String csrfToken = ((CamsodaHttpClient)site.getHttpClient()).getCsrfToken();
        String url = site.getBaseUrl() + "/api/v1/tip/" + getName();
        if (!Objects.equals(System.getenv("CTBREC_DEV"), "1")) {
            LOG.debug("Sending tip {}", url);
            RequestBody body = new FormBody.Builder()
                    .add("amount", Integer.toString(tokens))
                    .add("comment", "")
                    .build();
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Referer", Camsoda.BASE_URI + '/' + getName())
                    .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                    .addHeader("Accept", "application/json, text/plain, */*")
                    .addHeader("Accept-Language", "en")
                    .addHeader("X-CSRF-Token", csrfToken)
                    .build();
            try(Response response = site.getHttpClient().execute(request)) {
                if(!response.isSuccessful()) {
                    throw new HttpException(response.code(), response.message());
                }
            }
        }
    }

    @Override
    public boolean follow() throws IOException {
        String url = Camsoda.BASE_URI + "/api/v1/follow/" + getName();
        LOG.debug("Sending follow request {}", url);
        String csrfToken = ((CamsodaHttpClient)site.getHttpClient()).getCsrfToken();
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(null, ""))
                .addHeader("Referer", Camsoda.BASE_URI + '/' + getName())
                .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                .addHeader("Accept", "application/json, text/plain, */*")
                .addHeader("Accept-Language", "en")
                .addHeader("X-CSRF-Token", csrfToken)
                .build();
        try(Response response = site.getHttpClient().execute(request)) {
            if (response.isSuccessful()) {
                return true;
            } else {
                throw new HttpException(response.code(), response.message());
            }
        }
    }

    @Override
    public boolean unfollow() throws IOException {
        String url = Camsoda.BASE_URI + "/api/v1/unfollow/" + getName();
        LOG.debug("Sending follow request {}", url);
        String csrfToken = ((CamsodaHttpClient)site.getHttpClient()).getCsrfToken();
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(null, ""))
                .addHeader("Referer", Camsoda.BASE_URI + '/' + getName())
                .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                .addHeader("Accept", "application/json, text/plain, */*")
                .addHeader("Accept-Language", "en")
                .addHeader("X-CSRF-Token", csrfToken)
                .build();
        try (Response response = site.getHttpClient().execute(request)) {
            if (response.isSuccessful()) {
                return true;
            } else {
                throw new HttpException(response.code(), response.message());
            }
        }
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    public float getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(float sortOrder) {
        this.sortOrder = sortOrder;
    }
}
