package ctbrec.sites.cam4;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import org.json.JSONArray;
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

import ctbrec.AbstractModel;
import ctbrec.recorder.download.StreamSource;
import ctbrec.sites.Site;
import okhttp3.Request;
import okhttp3.Response;

public class Cam4Model extends AbstractModel {

    private static final transient Logger LOG = LoggerFactory.getLogger(Cam4Model.class);
    private Cam4 site;
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
            loadModelDetails();
        }
        return Objects.equals("NORMAL", onlineState);
    }

    private void loadModelDetails() throws IOException {
        String url = "https://www.cam4.de.com/getBroadcasting?usernames=" + getName();
        LOG.debug("Loading model details {}", url);
        Request req = new Request.Builder().url(url).build();
        Response response = site.getHttpClient().execute(req);
        if(response.isSuccessful()) {
            JSONArray json = new JSONArray(response.body().string());
            JSONObject details = json.getJSONObject(0);
            onlineState = details.getString("showType");
            playlistUrl = details.getString("hlsPreviewUrl");
            if(details.has("resolution")) {
                String res = details.getString("resolution");
                String[] tokens = res.split(":");
                resolution = new int[] {Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1])};
            }
        } else {
            IOException io = new IOException(response.code() + " " + response.message());
            response.close();
            throw io;
        }
    }

    @Override
    public String getOnlineState(boolean failFast) throws IOException, ExecutionException {
        return onlineState;
    }

    private String getPlaylistUrl() throws IOException {
        if(playlistUrl == null) {
            loadModelDetails();
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
        // TODO Auto-generated method stub

    }

    @Override
    public void receiveTip(int tokens) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public int[] getStreamResolution(boolean failFast) throws ExecutionException {
        if(resolution == null) {
            if(failFast) {
                return new int[2];
            } else {
                try {
                    loadModelDetails();
                } catch (IOException e) {
                    throw new ExecutionException(e);
                }
            }
        }
        return resolution;
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
        if(site instanceof Cam4) {
            this.site = (Cam4) site;
        } else {
            throw new IllegalArgumentException("Site has to be an instance of Cam4");
        }
    }

    @Override
    public Site getSite() {
        return site;
    }

    public void setPlaylistUrl(String playlistUrl) {
        this.playlistUrl = playlistUrl;
    }
}
