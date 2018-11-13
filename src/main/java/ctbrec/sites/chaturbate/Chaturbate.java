package ctbrec.sites.chaturbate;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.iheartradio.m3u8.Encoding;
import com.iheartradio.m3u8.Format;
import com.iheartradio.m3u8.ParseException;
import com.iheartradio.m3u8.PlaylistException;
import com.iheartradio.m3u8.PlaylistParser;
import com.iheartradio.m3u8.data.MasterPlaylist;
import com.iheartradio.m3u8.data.Playlist;
import com.iheartradio.m3u8.data.PlaylistData;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.io.HttpClient;
import ctbrec.recorder.Recorder;
import ctbrec.sites.AbstractSite;
import ctbrec.sites.ConfigUI;
import ctbrec.ui.HtmlParser;
import ctbrec.ui.TabProvider;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Chaturbate extends AbstractSite {

    private static final transient Logger LOG = LoggerFactory.getLogger(Chaturbate.class);
    public static final String BASE_URI = "https://chaturbate.com";
    public static final String AFFILIATE_LINK = BASE_URI + "/in/?track=default&tour=grq0&campaign=55vTi";
    public static final String REGISTRATION_LINK = BASE_URI + "/in/?track=default&tour=g4pe&campaign=55vTi";
    private Recorder recorder;
    private ChaturbateHttpClient httpClient;
    private ChaturbateTabProvider tabProvider;

    @Override
    public void init() throws IOException {

    }

    @Override
    public String getName() {
        return "Chaturbate";
    }

    @Override
    public String getBaseUrl() {
        return "https://chaturbate.com";
    }

    @Override
    public String getAffiliateLink() {
        return getBaseUrl() + "/in/?track=default&tour=LQps&campaign=55vTi&room=0xb00bface";
    }

    @Override
    public TabProvider getTabProvider() {
        if(tabProvider == null) {
            tabProvider = new ChaturbateTabProvider(this, recorder);
        }
        return tabProvider;
    }

    @Override
    public void setRecorder(Recorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public Model createModel(String name) {
        ChaturbateModel m = new ChaturbateModel(this);
        m.setName(name);
        m.setUrl(getBaseUrl() + '/' + name + '/');
        return m;
    }

    @Override
    public Integer getTokenBalance() throws IOException {
        String username = Config.getInstance().getSettings().username;
        if (username == null || username.trim().isEmpty()) {
            throw new IOException("Not logged in");
        }

        String url = "https://chaturbate.com/p/" + username + "/";
        Request req = new Request.Builder().url(url).build();
        Response resp = getHttpClient().execute(req, true);
        if (resp.isSuccessful()) {
            String profilePage = resp.body().string();
            String tokenText = HtmlParser.getText(profilePage, "span.tokencount");
            int tokens = Integer.parseInt(tokenText);
            return tokens;
        } else {
            throw new IOException("HTTP response: " + resp.code() + " - " + resp.message());
        }
    }

    @Override
    public String getBuyTokensLink() {
        return AFFILIATE_LINK;
    }

    @Override
    public void login() {
        if (credentialsAvailable()) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        getHttpClient().login();
                    } catch (IOException e1) {
                        LOG.warn("Initial login failed", e1);
                    }
                };
            }.start();
        }
    }

    @Override
    public HttpClient getHttpClient() {
        if(httpClient == null) {
            httpClient = new ChaturbateHttpClient();
        }
        return httpClient;
    }

    @Override
    public void shutdown() {
        getHttpClient().shutdown();
    }

    @Override
    public boolean supportsFollow() {
        return true;
    }

    @Override
    public boolean supportsTips() {
        return true;
    }

    @Override
    public boolean isSiteForModel(Model m) {
        return m instanceof ChaturbateModel;
    }

    // #######################
    private long lastRequest = System.currentTimeMillis();

    LoadingCache<String, StreamInfo> streamInfoCache = CacheBuilder.newBuilder()
            .initialCapacity(10_000)
            .maximumSize(10_000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(new CacheLoader<String, StreamInfo> () {
                @Override
                public StreamInfo load(String model) throws Exception {
                    return loadStreamInfo(model);
                }
            });

    LoadingCache<String, int[]> streamResolutionCache = CacheBuilder.newBuilder()
            .initialCapacity(10_000)
            .maximumSize(10_000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(new CacheLoader<String, int[]> () {
                @Override
                public int[] load(String model) throws Exception {
                    return loadResolution(model);
                }
            });

    public void sendTip(String name, int tokens) throws IOException {
        if (!Objects.equals(System.getenv("CTBREC_DEV"), "1")) {
            RequestBody body = new FormBody.Builder()
                    .add("csrfmiddlewaretoken", ((ChaturbateHttpClient)getHttpClient()).getToken())
                    .add("tip_amount", Integer.toString(tokens))
                    .add("tip_room_type", "public")
                    .build();
            Request req = new Request.Builder()
                    .url("https://chaturbate.com/tipping/send_tip/"+name+"/")
                    .post(body)
                    .addHeader("Referer", "https://chaturbate.com/"+name+"/")
                    .addHeader("X-Requested-With", "XMLHttpRequest")
                    .build();
            try(Response response = getHttpClient().execute(req, true)) {
                if(!response.isSuccessful()) {
                    throw new IOException(response.code() + " " + response.message());
                }
            }
        }
    }

    StreamInfo getStreamInfo(String modelName) throws IOException, ExecutionException {
        return streamInfoCache.get(modelName);
    }

    StreamInfo loadStreamInfo(String modelName) throws IOException, InterruptedException {
        throttleRequests();
        RequestBody body = new FormBody.Builder()
                .add("room_slug", modelName)
                .add("bandwidth", "high")
                .build();
        Request req = new Request.Builder()
                .url("https://chaturbate.com/get_edge_hls_url_ajax/")
                .post(body)
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .build();
        Response response = getHttpClient().execute(req);
        try {
            if(response.isSuccessful()) {
                String content = response.body().string();
                LOG.trace("Raw stream info: {}", content);
                Moshi moshi = new Moshi.Builder().build();
                JsonAdapter<StreamInfo> adapter = moshi.adapter(StreamInfo.class);
                StreamInfo streamInfo = adapter.fromJson(content);
                streamInfoCache.put(modelName, streamInfo);
                return streamInfo;
            } else {
                int code = response.code();
                String message = response.message();
                throw new IOException("Server responded with " + code + " - " + message + " headers: [" + response.headers() + "]");
            }
        } finally {
            response.close();
        }
    }

    public int[] getResolution(String modelName) throws ExecutionException {
        return streamResolutionCache.get(modelName);
    }

    private int[] loadResolution(String modelName) throws IOException, ParseException, PlaylistException, ExecutionException, InterruptedException {
        int[] res = new int[2];
        StreamInfo streamInfo = getStreamInfo(modelName);
        if(!streamInfo.url.startsWith("http")) {
            return res;
        }

        EOFException ex = null;
        for(int i=0; i<2; i++) {
            try {
                MasterPlaylist master = getMasterPlaylist(modelName);
                for (PlaylistData playlistData : master.getPlaylists()) {
                    if(playlistData.hasStreamInfo() && playlistData.getStreamInfo().hasResolution()) {
                        int h = playlistData.getStreamInfo().getResolution().height;
                        int w = playlistData.getStreamInfo().getResolution().width;
                        if(w > res[1]) {
                            res[0] = w;
                            res[1] = h;
                        }
                    }
                }
                ex = null;
                break; // this attempt worked, exit loop
            } catch(EOFException e) {
                // the cause might be, that the playlist url in streaminfo is outdated,
                // so let's remove it from cache and retry in the next iteration
                streamInfoCache.invalidate(modelName);
                ex = e;
            }
        }

        if(ex != null) {
            throw ex;
        }

        streamResolutionCache.put(modelName, res);
        return res;
    }

    private void throttleRequests() throws InterruptedException {
        long now = System.currentTimeMillis();
        long diff = now - lastRequest;
        if(diff < 500) {
            Thread.sleep(diff);
        }
        lastRequest = now;
    }

    public MasterPlaylist getMasterPlaylist(String modelName) throws IOException, ParseException, PlaylistException, ExecutionException {
        StreamInfo streamInfo = getStreamInfo(modelName);
        return getMasterPlaylist(streamInfo);
    }

    public MasterPlaylist getMasterPlaylist(StreamInfo streamInfo) throws IOException, ParseException, PlaylistException {
        LOG.trace("Loading master playlist {}", streamInfo.url);
        Request req = new Request.Builder().url(streamInfo.url).build();
        Response response = getHttpClient().execute(req);
        try {
            InputStream inputStream = response.body().byteStream();
            PlaylistParser parser = new PlaylistParser(inputStream, Format.EXT_M3U, Encoding.UTF_8);
            Playlist playlist = parser.parse();
            MasterPlaylist master = playlist.getMasterPlaylist();
            return master;
        } finally {
            response.close();
        }
    }

    @Override
    public ConfigUI getConfigurationGui() {
        return new ChaturbateConfigUi();
    }

    @Override
    public boolean credentialsAvailable() {
        String username = Config.getInstance().getSettings().username;
        return username != null && !username.trim().isEmpty();
    }
}
