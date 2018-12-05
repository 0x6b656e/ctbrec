package ctbrec.sites.bonga;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
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

import ctbrec.AbstractModel;
import ctbrec.Config;
import ctbrec.io.HttpException;
import ctbrec.recorder.download.StreamSource;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BongaCamsModel extends AbstractModel {

    private static final transient Logger LOG = LoggerFactory.getLogger(BongaCamsModel.class);

    private int userId;
    private boolean online = false;
    private List<StreamSource> streamSources = new ArrayList<>();
    private int[] resolution;

    @Override
    public boolean isOnline(boolean ignoreCache) throws IOException, ExecutionException, InterruptedException {
        if(ignoreCache) {
            String url = getStreamUrl();
            Request req = new Request.Builder().url(url).build();
            try(Response resp = site.getHttpClient().execute(req)) {
                online = resp.isSuccessful();
            }
        }
        return online;
    }

    private JSONObject getRoomData() throws IOException {
        String url = BongaCams.BASE_URL + "/tools/amf.php";
        RequestBody body = new FormBody.Builder()
                .add("method", "getRoomData")
                .add("args[]", getName())
                .add("args[]", "false")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                .addHeader("Accept", "application/json, text/javascript, */*")
                .addHeader("Accept-Language", "en")
                .addHeader("Referer", BongaCams.BASE_URL)
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .post(body)
                .build();
        try(Response response = site.getHttpClient().execute(request)) {
            if(response.isSuccessful()) {
                JSONObject json = new JSONObject(response.body().string());
                return json;
            } else {
                throw new IOException(response.code() + " " + response.message());
            }
        }
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    @Override
    public STATUS getOnlineState(boolean failFast) throws IOException, ExecutionException {
        return onlineState;
    }

    public void setOnlineState(STATUS onlineState) {
        this.onlineState = onlineState;
    }

    @Override
    public List<StreamSource> getStreamSources() throws IOException, ExecutionException, ParseException, PlaylistException {
        String streamUrl = getStreamUrl();
        if (streamUrl == null) {
            return Collections.emptyList();
        }
        Request req = new Request.Builder().url(streamUrl).build();
        try(Response response = site.getHttpClient().execute(req)) {
            if(response.isSuccessful()) {
                InputStream inputStream = response.body().byteStream();
                PlaylistParser parser = new PlaylistParser(inputStream, Format.EXT_M3U, Encoding.UTF_8, ParsingMode.LENIENT);
                Playlist playlist = parser.parse();
                MasterPlaylist master = playlist.getMasterPlaylist();
                streamSources.clear();
                for (PlaylistData playlistData : master.getPlaylists()) {
                    StreamSource streamsource = new StreamSource();
                    streamsource.mediaPlaylistUrl = streamUrl.replace("playlist.m3u8", playlistData.getUri());
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
        JSONObject roomData = getRoomData();
        if(roomData.optString("status").equals("success")) {
            JSONObject localData = roomData.getJSONObject("localData");
            String server = localData.getString("videoServerUrl");
            return "https:" + server + "/hls/stream_" + getName() + "/playlist.m3u8";
        } else {
            throw new IOException("Request was not successful: " + roomData.toString(2));
        }
    }

    @Override
    public void invalidateCacheEntries() {
        resolution = null;
    }

    @Override
    public void receiveTip(int tokens) throws IOException {
        String url = BongaCams.BASE_URL + "/chat-ajax-amf-service?" + System.currentTimeMillis();
        int userId = ((BongaCamsHttpClient)site.getHttpClient()).getUserId();
        RequestBody body = new FormBody.Builder()
                .add("method", "tipModel")
                .add("args[]", getName())
                .add("args[]", Integer.toString(tokens))
                .add("args[]", Integer.toString(userId))
                .add("args[3]", "")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                .addHeader("Accept", "application/json, text/javascript, */*")
                .addHeader("Accept-Language", "en")
                .addHeader("Referer", BongaCams.BASE_URL + '/' + getName())
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .post(body)
                .build();
        try(Response response = site.getHttpClient().execute(request)) {
            if(response.isSuccessful()) {
                JSONObject json = new JSONObject(response.body().string());
                if(!json.optString("status").equals("success")) {
                    LOG.error("Sending tip failed {}", json.toString(2));
                    throw new IOException("Sending tip failed");
                }
            } else {
                throw new IOException(response.code() + ' ' + response.message());
            }
        }
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

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}
