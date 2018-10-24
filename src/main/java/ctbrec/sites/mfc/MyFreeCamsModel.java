package ctbrec.sites.mfc;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

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
import ctbrec.recorder.download.StreamSource;
import ctbrec.sites.Site;
import ctbrec.ui.HtmlParser;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MyFreeCamsModel extends AbstractModel {

    private static final transient Logger LOG = LoggerFactory.getLogger(MyFreeCamsModel.class);

    private int uid;
    private String hlsUrl;
    private double camScore;
    private int viewerCount;
    private State state;
    private int resolution[];
    private MyFreeCams site;

    /**
     * This constructor exists only for deserialization. Please don't call it directly
     */
    public MyFreeCamsModel() {}

    MyFreeCamsModel(MyFreeCams site) {
        this.site = site;
    }

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
                } else {
                    src.width = Integer.MAX_VALUE;
                    src.height = Integer.MAX_VALUE;
                }
                String masterUrl = hlsUrl;
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
        if(hlsUrl == null) {
            throw new IllegalStateException("Stream url unknown");
        }
        LOG.debug("Loading master playlist {}", hlsUrl);
        Request req = new Request.Builder().url(hlsUrl).build();
        Response response = site.getHttpClient().execute(req);
        try {
            if(response.isSuccessful()) {
                InputStream inputStream = response.body().byteStream();
                PlaylistParser parser = new PlaylistParser(inputStream, Format.EXT_M3U, Encoding.UTF_8);
                Playlist playlist = parser.parse();
                MasterPlaylist master = playlist.getMasterPlaylist();
                return master;
            } else {
                throw new IOException(response.code() + " " + response.message());
            }
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
        String tipUrl = MyFreeCams.BASE_URI + "/php/tip.php";
        String initUrl = tipUrl + "?request=tip&username="+getName()+"&broadcaster_id="+getUid();
        Request req = new Request.Builder().url(initUrl).build();
        Response resp = site.getHttpClient().execute(req);
        if(resp.isSuccessful()) {
            String page = resp.body().string();
            Element hiddenInput = HtmlParser.getTag(page, "input[name=token]");
            String token = hiddenInput.attr("value");
            if (!Objects.equals(System.getenv("CTBREC_DEV"), "1")) {
                RequestBody body = new FormBody.Builder()
                        .add("token", token)
                        .add("broadcaster_id", Integer.toString(uid))
                        .add("tip_value", Integer.toString(tokens))
                        .add("submit_tip", "1")
                        .add("anonymous", "")
                        .add("public", "1")
                        .add("public_comment", "1")
                        .add("hide_amount", "0")
                        .add("silent", "")
                        .add("comment", "")
                        .add("mode", "")
                        .add("submit", " Confirm & Close Window")
                        .build();
                req = new Request.Builder()
                        .url(tipUrl)
                        .post(body)
                        .addHeader("Referer", initUrl)
                        .build();
                try(Response response = site.getHttpClient().execute(req, true)) {
                    if(!response.isSuccessful()) {
                        throw new IOException(response.code() + " " + response.message());
                    }
                }
            }
        } else {
            resp.close();
            throw new IOException(resp.code() + " " + resp.message());
        }
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

    public void update(SessionState state, String streamUrl) {
        setCamScore(state.getM().getCamscore());
        setState(State.of(state.getVs()));
        setStreamUrl(streamUrl);

        // preview
        String uid = state.getUid().toString();
        String uidStart = uid.substring(0, 3);
        String previewUrl = "https://img.mfcimg.com/photos2/"+uidStart+'/'+uid+"/avatar.300x300.jpg";
        setPreview(previewUrl);

        // tags
        Optional.ofNullable(state.getM()).map((m) -> m.getTags()).ifPresent((tags) -> {
            ArrayList<String> t = new ArrayList<>();
            t.addAll(tags);
            setTags(t);
        });

        // description
        Optional.ofNullable(state.getM()).map((m) -> m.getTopic()).ifPresent((topic) -> {
            try {
                setDescription(URLDecoder.decode(topic, "utf-8"));
            } catch (UnsupportedEncodingException e) {
                LOG.warn("Couldn't url decode topic", e);
            }
        });

        viewerCount = Optional.ofNullable(state.getM()).map((m) -> m.getRc()).orElseGet(() -> 0);
    }

    @Override
    public boolean follow() {
        return false;
    }

    @Override
    public boolean unfollow() {
        return false;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public int getViewerCount() {
        return viewerCount;
    }

    public void setViewerCount(int viewerCount) {
        this.viewerCount = viewerCount;
    }

    @Override
    public void setSite(Site site) {
        if(site instanceof MyFreeCams) {
            this.site = (MyFreeCams) site;
        } else {
            throw new IllegalArgumentException("Site has to be an instance of MyFreeCams");
        }
    }
}
