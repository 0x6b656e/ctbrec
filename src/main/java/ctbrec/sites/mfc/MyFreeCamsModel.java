package ctbrec.sites.mfc;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
import okhttp3.Request;
import okhttp3.Response;

public class MyFreeCamsModel extends AbstractModel {

    private static final transient Logger LOG = LoggerFactory.getLogger(MyFreeCamsModel.class);

    private String hlsUrl;
    private double camScore;
    private State state;
    private int resolution[];

    @Override
    public boolean isOnline() throws IOException, ExecutionException, InterruptedException {
        MyFreeCamsClient.getInstance().update(this);
        return state == State.ONLINE;
    }

    @Override
    public boolean isOnline(boolean ignoreCache) throws IOException, ExecutionException, InterruptedException {
        return isOnline();
    }

    @Override
    public String getOnlineState(boolean failFast) throws IOException, ExecutionException {
        return state != null ? state.toString() : "offline";
    }

    @Override
    public List<StreamSource> getStreamSources() throws IOException, ExecutionException, ParseException, PlaylistException {
        MasterPlaylist masterPlaylist = getMasterPlaylist();
        List<StreamSource> sources = new ArrayList<>();
        for (PlaylistData playlist : masterPlaylist.getPlaylists()) {
            if (playlist.hasStreamInfo()) {
                StreamSource src = new StreamSource();
                src.bandwidth = playlist.getStreamInfo().getBandwidth();
                if(playlist.getStreamInfo().getResolution() != null) {
                    src.width = playlist.getStreamInfo().getResolution().width;
                    src.height = playlist.getStreamInfo().getResolution().height;
                    String masterUrl = hlsUrl;
                    String baseUrl = masterUrl.substring(0, masterUrl.lastIndexOf('/') + 1);
                    String segmentUri = baseUrl + playlist.getUri();
                    src.mediaPlaylistUrl = segmentUri;
                    LOG.trace("Media playlist {}", src.mediaPlaylistUrl);
                    sources.add(src);
                }
            }
        }
        return sources;
    }

    private MasterPlaylist getMasterPlaylist() throws IOException, ParseException, PlaylistException {
        if(hlsUrl == null) {
            throw new IllegalStateException("Stream url unknown");
        }
        LOG.debug("Loading master playlist {}", hlsUrl);
        Request req = new Request.Builder().url(hlsUrl).build();
        Response response = MyFreeCams.httpClient.newCall(req).execute();
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
    }

    @Override
    public void receiveTip(int tokens) throws IOException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public int[] getStreamResolution(boolean failFast) throws ExecutionException {
        if(resolution == null) {
            if(failFast || hlsUrl == null) {
                return new int[2];
            }
            MyFreeCamsClient.getInstance().execute(()->{
                try {
                    List<StreamSource> streamSources = getStreamSources();
                    Collections.sort(streamSources);
                    StreamSource best = streamSources.get(streamSources.size()-1);
                    resolution = new int[] {best.width, best.height};
                } catch (ExecutionException | IOException | ParseException | PlaylistException e) {
                    LOG.error("Couldn't determine stream resolution", e);
                }
            });
            return new int[2];
        } else {
            return resolution;
        }
    }

    public void setStreamUrl(String hlsUrl) {
        this.hlsUrl = hlsUrl;
    }

    public String getStreamUrl() {
        return hlsUrl;
    }

    public double getCamScore() {
        return camScore;
    }

    public void setCamScore(double camScore) {
        this.camScore = camScore;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void update(SessionState state) {
        setCamScore(state.getM().getCamscore());
        setState(State.of(state.getVs()));

        // preview
        String uid = state.getUid().toString();
        String uidStart = uid.substring(0, 3);
        String previewUrl = "https://img.mfcimg.com/photos2/"+uidStart+'/'+uid+"/avatar.300x300.jpg";
        setPreview(previewUrl);

        // stream url
        Integer camserv = state.getU().getCamserv();
        if(camserv != null) {
            String hlsUrl = "http://video" + (camserv - 500) + ".myfreecams.com:1935/NxServer/ngrp:mfc_" + (100000000 + state.getUid()) + ".f4v_mobile/playlist.m3u8";
            setStreamUrl(hlsUrl);
        }
    }
}
