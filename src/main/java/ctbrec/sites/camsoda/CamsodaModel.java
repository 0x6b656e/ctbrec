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
        LOG.trace("Loading master playlist {}", streamUrl);
        if(streamSources == null) {
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

    }

    @Override
    public boolean follow() throws IOException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean unfollow() throws IOException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setSite(Site site) {
        this.site = site;
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
