package ctbrec.sites.camsoda;

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
import com.iheartradio.m3u8.PlaylistException;
import com.iheartradio.m3u8.PlaylistParser;
import com.iheartradio.m3u8.data.MasterPlaylist;
import com.iheartradio.m3u8.data.Playlist;
import com.iheartradio.m3u8.data.PlaylistData;
import com.iheartradio.m3u8.data.StreamInfo;

import ctbrec.AbstractModel;
import ctbrec.recorder.download.StreamSource;
import ctbrec.sites.Site;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CamsodaModel extends AbstractModel {

    private static final transient Logger LOG = LoggerFactory.getLogger(CamsodaModel.class);
    private String streamUrl;
    private Site site;
    private List<StreamSource> streamSources = null;
    private int[] resolution;
    private String status = "n/a";
    private float sortOrder = 0;

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
                status = chat.getString("status");
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

    @Override
    public boolean isOnline(boolean ignoreCache) throws IOException, ExecutionException, InterruptedException {
        if(ignoreCache) {
            loadModel();
        }
        return Objects.equals(status, "online");
    }

    @Override
    public String getOnlineState(boolean failFast) throws IOException, ExecutionException {
        if(failFast) {
            return status;
        } else {
            if(status.equals("n/a")) {
                loadModel();
            }
            return status;
        }
    }

    public void setOnlineState(String state) {
        this.status = state;
    }

    @Override
    public List<StreamSource> getStreamSources() throws IOException, ExecutionException, ParseException, PlaylistException {
        LOG.debug("Loading master playlist {}", streamUrl);
        Request req = new Request.Builder().url(streamUrl).build();
        Response response = site.getHttpClient().execute(req);
        try {
            InputStream inputStream = response.body().byteStream();
            PlaylistParser parser = new PlaylistParser(inputStream, Format.EXT_M3U, Encoding.UTF_8);
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
        resolution = null;
    }

    @Override
    public int[] getStreamResolution(boolean failFast) throws ExecutionException {
        if(resolution != null) {
            return resolution;
        } else {
            if(failFast) {
                return new int[] {0,0};
            } else {
                try {
                    List<StreamSource> streamSources = getStreamSources();
                    StreamSource src = streamSources.get(0);
                    resolution = new int[] {src.width, src.height};
                    return resolution;
                } catch (IOException | ParseException | PlaylistException e) {
                    throw new ExecutionException(e);
                }
            }
        }
    }

    @Override
    public void receiveTip(int tokens) throws IOException {
        // TODO Auto-generated method stub
        /*
              sendTip: function(i, a, r, o, c, d) {
                    if (!s.isAuthenticated()) return s.showRegister(), t.when(!1);
                    var u = t.defer();
                    return e.post("/api/v1/tip/" + i, {
                        amount: a,
                        comment: o,
                        type: r,
                        app_data: c,
                        source_id: d
                    }).then(function(e) {
                        1 == e.data.status ? (s.currentUser.tokens = e.data.total, void 0 != e.data.tipped_performer_last_24hrs && e.data.tipped_performer_last_24hrs >= 25 && (n.$emit("local.allowed_to_rate"), 0 == n.allowedToRate && (n.allowedToRate = !0, l.pop("info", "Voting Unlocked", "You tipped " + i + " 25 tokens in the past 24 hours, you may now vote!"))), u.resolve(e.data)) : (l.pop("error", e.data.error, e.data.message), u.reject(e.data))
                    }), u.promise
                },
         */
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
                .addHeader("User-Agent", " Mozilla/5.0 (X11; Linux x86_64; rv:62.0) Gecko/20100101 Firefox/62.0")
                .addHeader("Accept", " application/json, text/plain, */*")
                .addHeader("Accept-Language", " de,en-US;q=0.7,en;q=0.3")
                .addHeader("X-CSRF-Token", csrfToken)
                .build();
        Response resp = site.getHttpClient().execute(request, true);
        if (resp.isSuccessful()) {
            resp.close();
            return true;
        } else {
            resp.close();
            throw new IOException("HTTP status " + resp.code() + " " + resp.message());
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
                .addHeader("User-Agent", " Mozilla/5.0 (X11; Linux x86_64; rv:62.0) Gecko/20100101 Firefox/62.0")
                .addHeader("Accept", " application/json, text/plain, */*")
                .addHeader("Accept-Language", " de,en-US;q=0.7,en;q=0.3")
                .addHeader("X-CSRF-Token", csrfToken)
                .build();
        Response resp = site.getHttpClient().execute(request, true);
        if (resp.isSuccessful()) {
            resp.close();
            return true;
        } else {
            resp.close();
            throw new IOException("HTTP status " + resp.code() + " " + resp.message());
        }
    }

    @Override
    public void setSite(Site site) {
        if(site instanceof Camsoda) {
            this.site = site;
        } else {
            throw new IllegalArgumentException("Site has to be an instance of Camsoda");
        }
    }

    @Override
    public Site getSite() {
        return site;
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
