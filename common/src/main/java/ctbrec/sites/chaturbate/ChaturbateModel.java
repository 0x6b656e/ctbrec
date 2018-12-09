package ctbrec.sites.chaturbate;

import static ctbrec.Model.State.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iheartradio.m3u8.ParseException;
import com.iheartradio.m3u8.PlaylistException;
import com.iheartradio.m3u8.data.MasterPlaylist;
import com.iheartradio.m3u8.data.PlaylistData;

import ctbrec.AbstractModel;
import ctbrec.Config;
import ctbrec.recorder.download.StreamSource;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChaturbateModel extends AbstractModel {

    private static final transient Logger LOG = LoggerFactory.getLogger(ChaturbateModel.class);
    private int[] resolution = new int[2];

    /**
     * This constructor exists only for deserialization. Please don't call it directly
     */
    public ChaturbateModel() {
    }

    ChaturbateModel(Chaturbate site) {
        this.site = site;
    }

    @Override
    public boolean isOnline(boolean ignoreCache) throws IOException, ExecutionException, InterruptedException {
        String roomStatus;
        if(ignoreCache) {
            StreamInfo info = getChaturbate().loadStreamInfo(getName());
            roomStatus = Optional.ofNullable(info).map(i -> i.room_status).orElse("");
            LOG.trace("Model {} room status: {}", getName(), info.room_status);
        } else {
            StreamInfo info = getChaturbate().getStreamInfo(getName(), true);
            roomStatus = Optional.ofNullable(info).map(i -> i.room_status).orElse("");
        }
        return Objects.equals("public", roomStatus);
    }

    @Override
    public int[] getStreamResolution(boolean failFast) throws ExecutionException {
        if(failFast) {
            return resolution;
        }

        try {
            resolution = getChaturbate().getResolution(getName());
        } catch(Exception e) {
            throw new ExecutionException(e);
        }
        return resolution;
    }

    /**
     * Invalidates the entries in StreamInfo and resolution cache for this model
     * and thus causes causes the LoadingCache to update them
     */
    @Override
    public void invalidateCacheEntries() {
        getChaturbate().streamInfoCache.invalidate(getName());
    }

    public State getOnlineState() throws IOException, ExecutionException {
        return getOnlineState(false);
    }

    @Override
    public State getOnlineState(boolean failFast) throws IOException, ExecutionException {
        if(failFast) {
            StreamInfo info = getChaturbate().streamInfoCache.getIfPresent(getName());
            setOnlineStateByRoomStatus(info.room_status);
        } else {
            StreamInfo info = getChaturbate().streamInfoCache.get(getName());
            setOnlineStateByRoomStatus(info.room_status);
        }
        return onlineState;
    }

    private void setOnlineStateByRoomStatus(String room_status) {
        if(room_status != null) {
            switch(room_status) {
            case "public":
                onlineState = ONLINE;
                break;
            case "offline":
                onlineState = OFFLINE;
                break;
            case "private":
            case "hidden":
            case "password protected":
                onlineState = PRIVATE;
                break;
            case "away":
                onlineState = AWAY;
                break;
            case "group":
                onlineState = State.GROUP;
                break;
            default:
                LOG.debug("Unknown show type {}", room_status);
                onlineState = State.UNKNOWN;
            }
        }
    }

    public StreamInfo getStreamInfo() throws IOException, ExecutionException {
        return getChaturbate().getStreamInfo(getName());
    }
    public MasterPlaylist getMasterPlaylist() throws IOException, ParseException, PlaylistException, ExecutionException {
        return getChaturbate().getMasterPlaylist(getName());
    }

    @Override
    public void receiveTip(int tokens) throws IOException {
        getChaturbate().sendTip(getName(), tokens);
    }

    @Override
    public List<StreamSource> getStreamSources() throws IOException, ExecutionException, ParseException, PlaylistException {
        invalidateCacheEntries();
        StreamInfo streamInfo = getStreamInfo();
        MasterPlaylist masterPlaylist = getMasterPlaylist();
        List<StreamSource> sources = new ArrayList<>();
        for (PlaylistData playlist : masterPlaylist.getPlaylists()) {
            if (playlist.hasStreamInfo()) {
                StreamSource src = new StreamSource();
                src.bandwidth = playlist.getStreamInfo().getBandwidth();
                src.height = playlist.getStreamInfo().getResolution().height;
                String masterUrl = streamInfo.url;
                String baseUrl = masterUrl.substring(0, masterUrl.lastIndexOf('/') + 1);
                String segmentUri = baseUrl + playlist.getUri();
                src.mediaPlaylistUrl = segmentUri;
                if(src.mediaPlaylistUrl.contains("?")) {
                    src.mediaPlaylistUrl = src.mediaPlaylistUrl.substring(0, src.mediaPlaylistUrl.lastIndexOf('?'));
                }
                LOG.trace("Media playlist {}", src.mediaPlaylistUrl);
                sources.add(src);
            }
        }
        return sources;
    }

    @Override
    public boolean follow() throws IOException {
        return follow(true);
    }

    @Override
    public boolean unfollow() throws IOException {
        return follow(false);
    }

    private boolean follow(boolean follow) throws IOException {
        Request req = new Request.Builder().url(getUrl()).build();
        Response resp = site.getHttpClient().execute(req);
        resp.close();

        String url = null;
        if(follow) {
            url = getSite().getBaseUrl() + "/follow/follow/" + getName() + "/";
        } else {
            url = getSite().getBaseUrl() + "/follow/unfollow/" + getName() + "/";
        }

        RequestBody body = RequestBody.create(null, new byte[0]);
        req = new Request.Builder()
                .url(url)
                .method("POST", body)
                .header("Accept", "*/*")
                .header("Accept-Language", "en-US,en;q=0.5")
                .header("Referer", getUrl())
                .header("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                .header("X-CSRFToken", ((ChaturbateHttpClient)site.getHttpClient()).getToken())
                .header("X-Requested-With", "XMLHttpRequest")
                .build();
        resp = site.getHttpClient().execute(req);
        if(resp.isSuccessful()) {
            String msg = resp.body().string();
            if(!msg.equalsIgnoreCase("ok")) {
                LOG.debug(msg);
                throw new IOException("Response was " + msg.substring(0, Math.min(msg.length(), 500)));
            } else {
                LOG.debug("Follow/Unfollow -> {}", msg);
                return true;
            }
        } else {
            resp.close();
            throw new IOException("HTTP status " + resp.code() + " " + resp.message());
        }
    }

    private Chaturbate getChaturbate() {
        return (Chaturbate) site;
    }
}
