package ctbrec.sites.chaturbate;

import static ctbrec.sites.chaturbate.Chaturbate.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iheartradio.m3u8.ParseException;
import com.iheartradio.m3u8.PlaylistException;
import com.iheartradio.m3u8.data.MasterPlaylist;
import com.iheartradio.m3u8.data.PlaylistData;

import ctbrec.AbstractModel;
import ctbrec.recorder.download.StreamSource;
import ctbrec.sites.Site;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChaturbateModel extends AbstractModel {

    private static final transient Logger LOG = LoggerFactory.getLogger(ChaturbateModel.class);
    private Chaturbate site;

    ChaturbateModel(Chaturbate site) {
        this.site = site;
    }

    @Override
    public boolean isOnline() throws IOException, ExecutionException, InterruptedException {
        return isOnline(false);
    }

    @Override
    public boolean isOnline(boolean ignoreCache) throws IOException, ExecutionException, InterruptedException {
        StreamInfo info;
        if(ignoreCache) {
            info = site.loadStreamInfo(getName());
            LOG.trace("Model {} room status: {}", getName(), info.room_status);
        } else {
            info = site.getStreamInfo(getName());
        }
        return Objects.equals("public", info.room_status);
    }

    @Override
    public int[] getStreamResolution(boolean failFast) throws ExecutionException {
        int[] resolution = site.streamResolutionCache.getIfPresent(getName());
        if(resolution != null) {
            return site.getResolution(getName());
        } else {
            if(failFast) {
                return new int[2];
            } else {
                return site.getResolution(getName());
            }
        }
    }

    /**
     * Invalidates the entries in StreamInfo and resolution cache for this model
     * and thus causes causes the LoadingCache to update them
     */
    @Override
    public void invalidateCacheEntries() {
        site.streamInfoCache.invalidate(getName());
        site.streamResolutionCache.invalidate(getName());
    }

    public String getOnlineState() throws IOException, ExecutionException {
        return getOnlineState(false);
    }

    @Override
    public String getOnlineState(boolean failFast) throws IOException, ExecutionException {
        StreamInfo info = site.streamInfoCache.getIfPresent(getName());
        return info != null ? info.room_status : "n/a";
    }

    public StreamInfo getStreamInfo() throws IOException, ExecutionException {
        return site.getStreamInfo(getName());
    }
    public MasterPlaylist getMasterPlaylist() throws IOException, ParseException, PlaylistException, ExecutionException {
        return site.getMasterPlaylist(getName());
    }

    @Override
    public void receiveTip(int tokens) throws IOException {
        site.sendTip(getName(), tokens);
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
            url = BASE_URI + "/follow/follow/" + getName() + "/";
        } else {
            url = BASE_URI + "/follow/unfollow/" + getName() + "/";
        }

        RequestBody body = RequestBody.create(null, new byte[0]);
        req = new Request.Builder()
                .url(url)
                .method("POST", body)
                .header("Accept", "*/*")
                .header("Accept-Language", "en-US,en;q=0.5")
                .header("Referer", getUrl())
                .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:59.0) Gecko/20100101 Firefox/59.0")
                .header("X-CSRFToken", ((ChaturbateHttpClient)site.getHttpClient()).getToken())
                .header("X-Requested-With", "XMLHttpRequest")
                .build();
        resp = site.getHttpClient().execute(req, true);
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

    @Override
    public void setSite(Site site) {
        if(site instanceof Chaturbate) {
            this.site = (Chaturbate) site;
        } else {
            throw new IllegalArgumentException("Site has to be an instance of Chaturbate");
        }
    }
}
