package ctbrec.sites.streamate;

import static ctbrec.Model.State.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.json.JSONArray;
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
import okhttp3.Request;
import okhttp3.Response;

public class StreamateModel extends AbstractModel {

    private static final transient Logger LOG = LoggerFactory.getLogger(StreamateModel.class);

    private boolean online = false;
    private List<StreamSource> streamSources = new ArrayList<>();
    private int[] resolution;
    private String id;

    @Override
    public boolean isOnline(boolean ignoreCache) throws IOException, ExecutionException, InterruptedException {
        if(ignoreCache) {
            JSONObject roomInfo = getRoomInfo();
            JSONObject stream = roomInfo.getJSONObject("stream");
            String serverId = stream.optString("serverId");
            online = !serverId.equals("0");
        }
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    @Override
    public State getOnlineState(boolean failFast) throws IOException, ExecutionException {
        if(failFast) {
            return onlineState;
        } else {
            if(onlineState == UNKNOWN) {
                return online ? ONLINE : OFFLINE;
            }
            return onlineState;
        }
    }

    @Override
    public void setOnlineState(State onlineState) {
        this.onlineState = onlineState;
    }

    @Override
    public List<StreamSource> getStreamSources() throws IOException, ExecutionException, ParseException, PlaylistException {
        String streamUrl = getStreamUrl();
        if (streamUrl == null) {
            return Collections.emptyList();
        }
        LOG.debug(streamUrl);
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
                    streamSources.add(streamsource);
                }
            } else {
                throw new HttpException(response.code(), response.message());
            }
        }
        return streamSources;
    }

    private String getStreamUrl() throws IOException {
        JSONObject json = getRoomInfo();
        JSONObject performer = json.getJSONObject("performer");
        id = Long.toString(performer.getLong("id"));
        JSONObject stream = json.getJSONObject("stream");
        String sserver = stream.getString("serverId");
        String streamId = stream.getString("streamId");
        String wsHost = stream.getString("nodeHost");
        JSONObject liveservices = json.getJSONObject("liveservices");
        String streamHost = liveservices.getString("host").replace("wss", "https");

        String roomId;
        try {
            roomId = getRoomId(wsHost, sserver, streamId);
            LOG.debug("room id: {}", roomId);
        } catch (InterruptedException e) {
            throw new IOException("Couldn't get room id", e);
        }

        String streamFormatUrl = getStreamFormatUrl(streamHost, roomId);
        return getMasterPlaylistUrl(streamFormatUrl);
    }

    private String getMasterPlaylistUrl(String url) throws IOException {
        LOG.debug(url);
        Request req = new Request.Builder()
                .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                .addHeader("Accept", "*/*")
                .addHeader("Accept-Language", "en")
                .addHeader("Referer", Streamate.BASE_URL + '/' + getName())
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .url(url)
                .build();
        try(Response response = site.getHttpClient().execute(req)) {
            if(response.isSuccessful()) {
                JSONObject json = new JSONObject(response.body().string());
                JSONObject formats = json.getJSONObject("formats");
                JSONObject hls = formats.getJSONObject("mp4-hls");
                return hls.getString("manifest");
            } else {
                throw new HttpException(response.code(), response.message());
            }
        }
    }

    private String getStreamFormatUrl(String streamHost, String roomId) throws IOException {
        String url = streamHost + "/videourl?payload="
                + URLEncoder.encode("{\"puserid\":" + id + ",\"roomid\":\"" + roomId + "\",\"showtype\":1,\"nginx\":1}", "utf-8");
        LOG.debug(url);
        Request req = new Request.Builder()
                .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                .addHeader("Accept", "*/*")
                .addHeader("Accept-Language", "en")
                .addHeader("Referer", Streamate.BASE_URL + '/' + getName())
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .url(url)
                .build();
        try(Response response = site.getHttpClient().execute(req)) {
            if(response.isSuccessful()) {
                JSONArray streamConfig = new JSONArray(response.body().string());
                JSONObject obj = streamConfig.getJSONObject(0);
                return obj.getString("url");
            } else {
                throw new HttpException(response.code(), response.message());
            }
        }
    }

    private String getRoomId(String wsHost, String sserver, String streamId) throws InterruptedException {
        String wsUrl = wsHost + "/socket.io/?"
                + "performerid=" + id
                + "&sserver=" + sserver
                + "&streamid=" + streamId
                + "&sakey=&sessiontype=preview&perfdiscountid=0&minduration=0&goldshowid=0&version=7&referrer=hybrid.client.6.3.16/avchat.swf&usertype=false&lang=en&EIO=3&transport=websocket";

        StreamateWebsocketClient wsClient = new StreamateWebsocketClient(wsUrl, site.getHttpClient());
        return wsClient.getRoomId();
    }

    private JSONObject getRoomInfo() throws IOException {
        String url = "https://hybridclient.naiadsystems.com/api/v1/config/?sabasic=&sakey=&sk=www.streamate.com&userid=0&version=6.3.16&ajax=1&name=" + getName();
        Request req = new Request.Builder()
                .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                .addHeader("Accept", "application/json, text/javascript, */*")
                .addHeader("Accept-Language", "en")
                .addHeader("Referer", Streamate.BASE_URL + '/' + getName())
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .url(url)
                .build();
        try(Response response = site.getHttpClient().execute(req)) {
            if(response.isSuccessful()) {
                return new JSONObject(response.body().string());
            } else {
                throw new HttpException(response.code(), response.message());
            }
        }
    }

    @Override
    public void invalidateCacheEntries() {
        resolution = null;
    }

    @Override
    public void receiveTip(int tokens) throws IOException {
        //        String url = Streamate.BASE_URL + "/chat-ajax-amf-service?" + System.currentTimeMillis();
        //        int userId = ((StreamateHttpClient)site.getHttpClient()).getUserId();
        //        RequestBody body = new FormBody.Builder()
        //                .add("method", "tipModel")
        //                .add("args[]", getName())
        //                .add("args[]", Integer.toString(tokens))
        //                .add("args[]", Integer.toString(userId))
        //                .add("args[3]", "")
        //                .build();
        //        Request request = new Request.Builder()
        //                .url(url)
        //                .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
        //                .addHeader("Accept", "application/json, text/javascript, */*")
        //                .addHeader("Accept-Language", "en")
        //                .addHeader("Referer", Streamate.BASE_URL + '/' + getName())
        //                .addHeader("X-Requested-With", "XMLHttpRequest")
        //                .post(body)
        //                .build();
        //        try(Response response = site.getHttpClient().execute(request)) {
        //            if(response.isSuccessful()) {
        //                JSONObject json = new JSONObject(response.body().string());
        //                if(!json.optString("status").equals("success")) {
        //                    LOG.error("Sending tip failed {}", json.toString(2));
        //                    throw new IOException("Sending tip failed");
        //                }
        //            } else {
        //                throw new IOException(response.code() + ' ' + response.message());
        //            }
        //        }
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
