package ctbrec.sites.cam4;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
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

import ctbrec.AbstractModel;
import ctbrec.Config;
import ctbrec.StringUtil;
import ctbrec.io.HtmlParser;
import ctbrec.io.HttpException;
import ctbrec.recorder.download.StreamSource;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Cam4Model extends AbstractModel {

    private static final transient Logger LOG = LoggerFactory.getLogger(Cam4Model.class);
    private String playlistUrl;
    private String onlineState = "offline";
    private int[] resolution = null;

    @Override
    public boolean isOnline() throws IOException, ExecutionException, InterruptedException {
        return isOnline(false);
    }

    @Override
    public boolean isOnline(boolean ignoreCache) throws IOException, ExecutionException, InterruptedException {
        if(ignoreCache || onlineState == null) {
            try {
                loadModelDetails();
            } catch (ModelDetailsEmptyException e) {
                return false;
            }
        }
        return Objects.equals("NORMAL", onlineState) && StringUtil.isNotBlank(playlistUrl);
    }

    private void loadModelDetails() throws IOException, ModelDetailsEmptyException {
        String url = site.getBaseUrl() + "/getBroadcasting?usernames=" + getName();
        LOG.trace("Loading model details {}", url);
        Request req = new Request.Builder().url(url).build();
        try(Response response = site.getHttpClient().execute(req)) {
            if(response.isSuccessful()) {
                JSONArray json = new JSONArray(response.body().string());
                if(json.length() == 0) {
                    onlineState = "offline";
                    throw new ModelDetailsEmptyException("Model details are empty");
                }
                JSONObject details = json.getJSONObject(0);
                onlineState = details.getString("showType");
                playlistUrl = details.getString("hlsPreviewUrl");
                if(details.has("resolution")) {
                    String res = details.getString("resolution");
                    String[] tokens = res.split(":");
                    resolution = new int[] {Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1])};
                }
            } else {
                throw new HttpException(response.code(), response.message());
            }
        }
    }

    @Override
    public String getOnlineState(boolean failFast) throws IOException, ExecutionException {
        return onlineState;
    }

    private String getPlaylistUrl() throws IOException {
        if(playlistUrl == null) {
            try {
                loadModelDetails();
            } catch (ModelDetailsEmptyException e) {
                throw new IOException(e);
            }
        }
        return playlistUrl;
    }

    @Override
    public List<StreamSource> getStreamSources() throws IOException, ExecutionException, ParseException, PlaylistException {
        MasterPlaylist masterPlaylist = getMasterPlaylist();
        List<StreamSource> sources = new ArrayList<>();
        for (PlaylistData playlist : masterPlaylist.getPlaylists()) {
            if (playlist.hasStreamInfo()) {
                StreamSource src = new StreamSource();
                src.bandwidth = playlist.getStreamInfo().getBandwidth();
                src.height = playlist.getStreamInfo().getResolution().height;
                String masterUrl = getPlaylistUrl();
                String baseUrl = masterUrl.substring(0, masterUrl.lastIndexOf('/') + 1);
                String segmentUri = baseUrl + playlist.getUri();
                src.mediaPlaylistUrl = segmentUri;
                LOG.trace("Media playlist {}", src.mediaPlaylistUrl);
                sources.add(src);
            }
        }
        return sources;
    }

    private MasterPlaylist getMasterPlaylist() throws IOException, ParseException, PlaylistException {
        LOG.trace("Loading master playlist {}", getPlaylistUrl());
        Request req = new Request.Builder().url(getPlaylistUrl()).build();
        Response response = site.getHttpClient().execute(req);
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
    public void invalidateCacheEntries() {
        resolution = null;
        playlistUrl = null;
    }

    @Override
    public void receiveTip(int tokens) throws IOException {
        throw new RuntimeException("Not implemented for Cam4, yet");
    }

    @Override
    public int[] getStreamResolution(boolean failFast) throws ExecutionException {
        if(resolution == null) {
            if(failFast) {
                return new int[2];
            } else {
                try {
                    loadModelDetails();
                } catch (Exception e) {
                    throw new ExecutionException(e);
                }
            }
        }
        return resolution;
    }

    @Override
    public boolean follow() throws IOException {
        String url = site.getBaseUrl() + "/profiles/addFriendFavorite?action=addFavorite&object=" + getName() + "&_=" + System.currentTimeMillis();
        Request req = new Request.Builder()
                .url(url)
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .build();
        Response response = site.getHttpClient().execute(req);
        boolean success = response.isSuccessful();
        response.close();
        return success;
    }

    @Override
    public boolean unfollow() throws IOException {
        // get model user id
        String url = site.getBaseUrl() + '/' + getName();
        Request req = new Request.Builder()
                .url(url)
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .build();

        // we have to use a client without any cam4 cookies here, otherwise
        // this request is redirected to the login page. no idea why
        try(Response response = site.getRecorder().getHttpClient().execute(req)) {
            String broadCasterId = null;
            if(response.isSuccessful()) {
                String content = response.body().string();
                try {
                    Element tag = HtmlParser.getTag(content, "input[name=\"broadcasterId\"]");
                    broadCasterId = tag.attr("value");
                } catch(Exception e) {
                    LOG.debug(content);
                    throw new IOException(e);
                }

                // send unfollow request
                String username = Config.getInstance().getSettings().cam4Username;
                url = site.getBaseUrl() + '/' + username + "/edit/friends_favorites";
                RequestBody body = new FormBody.Builder()
                        .add("deleteFavorites", broadCasterId)
                        .add("simpleresult", "true")
                        .build();
                req = new Request.Builder()
                        .url(url)
                        .post(body)
                        .addHeader("X-Requested-With", "XMLHttpRequest")
                        .build();
                Response resp = site.getHttpClient().execute(req);
                if(resp.isSuccessful()) {
                    return Objects.equals(resp.body().string(), "Ok");
                } else {
                    resp.close();
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    public void setPlaylistUrl(String playlistUrl) {
        this.playlistUrl = playlistUrl;
    }

    public void setOnlineState(String onlineState) {
        this.onlineState = onlineState;
    }

    public class ModelDetailsEmptyException extends Exception {
        public ModelDetailsEmptyException(String msg) {
            super(msg);
        }
    }
}
