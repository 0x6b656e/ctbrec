package ctbrec;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
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

import ctbrec.recorder.StreamInfo;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Model {

    private static final transient Logger LOG = LoggerFactory.getLogger(Model.class);

    private String url;
    private String name;
    private String preview;
    private String description;
    private List<String> tags = new ArrayList<>();
    private int streamUrlIndex = -1;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPreview() {
        return preview;
    }

    public void setPreview(String preview) {
        this.preview = preview;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public boolean isOnline() throws IOException, ExecutionException, InterruptedException {
        return isOnline(false);
    }

    public boolean isOnline(boolean ignoreCache) throws IOException, ExecutionException, InterruptedException {
        StreamInfo info;
        if(ignoreCache) {
            info = Chaturbate.INSTANCE.loadStreamInfo(getName());
            LOG.debug("Model {} room status: {}", getName(), info.room_status);
        } else {
            info = Chaturbate.INSTANCE.getStreamInfo(getName());
        }
        return Objects.equals("public", info.room_status);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getStreamUrlIndex() {
        return streamUrlIndex;
    }

    public void setStreamUrlIndex(int streamUrlIndex) {
        this.streamUrlIndex = streamUrlIndex;
    }

    public int[] getStreamResolution(boolean failFast) throws ExecutionException {
        int[] resolution = Chaturbate.INSTANCE.streamResolutionCache.getIfPresent(getName());
        if(resolution != null) {
            return Chaturbate.INSTANCE.getResolution(getName());
        } else {
            return new int[2];
        }
    }

    public int[] getStreamResolution() throws ExecutionException {
        return Chaturbate.INSTANCE.getResolution(getName());
    }

    public String getOnlineState() throws IOException, ExecutionException {
        return getOnlineState(false);
    }

    public String getOnlineState(boolean failFast) throws IOException, ExecutionException {
        StreamInfo info = Chaturbate.INSTANCE.streamInfoCache.getIfPresent(getName());
        return info != null ? info.room_status : "n/a";
    }

    public StreamInfo getStreamInfo() throws IOException, ExecutionException {
        return Chaturbate.INSTANCE.getStreamInfo(getName());
    }
    public MasterPlaylist getMasterPlaylist() throws IOException, ParseException, PlaylistException, ExecutionException {
        return Chaturbate.INSTANCE.getMasterPlaylist(getName());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        result = prime * result + ((getUrl() == null) ? 0 : getUrl().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Model))
            return false;
        Model other = (Model) obj;
        if (getName() == null) {
            if (other.getName() != null)
                return false;
        } else if (!getName().equals(other.getName()))
            return false;
        if (getUrl() == null) {
            if (other.getUrl() != null)
                return false;
        } else if (!getUrl().equals(other.getUrl()))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return getName();
    }

    private static class Chaturbate {
        private static final transient Logger LOG = LoggerFactory.getLogger(Chaturbate.class);

        public static final Chaturbate INSTANCE = new Chaturbate(HttpClient.getInstance());

        private HttpClient client;

        private static long lastRequest = System.currentTimeMillis();

        private LoadingCache<String, StreamInfo> streamInfoCache = CacheBuilder.newBuilder()
                .initialCapacity(10_000)
                .maximumSize(10_000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build(new CacheLoader<String, StreamInfo> () {
                    @Override
                    public StreamInfo load(String model) throws Exception {
                        return loadStreamInfo(model);
                    }
                });

        private LoadingCache<String, int[]> streamResolutionCache = CacheBuilder.newBuilder()
                .initialCapacity(10_000)
                .maximumSize(10_000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build(new CacheLoader<String, int[]> () {
                    @Override
                    public int[] load(String model) throws Exception {
                        return loadResolution(model);
                    }
                });

        public Chaturbate(HttpClient client) {
            this.client = client;
        }

        private StreamInfo getStreamInfo(String modelName) throws IOException, ExecutionException {
            return streamInfoCache.get(modelName);
        }

        private StreamInfo loadStreamInfo(String modelName) throws IOException, InterruptedException {
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
            Response response = client.execute(req);
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
            Response response = client.execute(req);
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
    }
}
