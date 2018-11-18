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
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import ctbrec.AbstractModel;
import ctbrec.io.HtmlParser;
import ctbrec.io.HttpException;
import ctbrec.recorder.download.StreamSource;
import ctbrec.sites.Site;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MyFreeCamsModel extends AbstractModel {

    private static final transient Logger LOG = LoggerFactory.getLogger(MyFreeCamsModel.class);

    private int uid = -1; // undefined
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
        LOG.trace("Loading master playlist {}", hlsUrl);
        Request req = new Request.Builder().url(hlsUrl).build();
        try(Response response = site.getHttpClient().execute(req)) {
            if(response.isSuccessful()) {
                InputStream inputStream = response.body().byteStream();
                PlaylistParser parser = new PlaylistParser(inputStream, Format.EXT_M3U, Encoding.UTF_8);
                Playlist playlist = parser.parse();
                MasterPlaylist master = playlist.getMasterPlaylist();
                return master;
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
        String tipUrl = MyFreeCams.BASE_URI + "/php/tip.php";
        String initUrl = tipUrl + "?request=tip&username="+getName()+"&broadcaster_id="+getUid();
        Request req = new Request.Builder().url(initUrl).build();
        try(Response resp = site.getHttpClient().execute(req)) {
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
                    try(Response response = site.getHttpClient().execute(req)) {
                        if(!response.isSuccessful()) {
                            throw new HttpException(response.code(), response.message());
                        }
                    }
                }
            } else {
                throw new HttpException(resp.code(), resp.message());
            }
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
                } catch (ParseException | PlaylistException e) {
                    LOG.warn("Couldn't determine stream resolution - {}", e.getMessage());
                } catch (ExecutionException | IOException e) {
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

    @Override
    public void setName(String name) {
        if(getName() != null && name != null && !getName().equals(name)) {
            LOG.debug("Model name changed {} -> {}", getName(), name);
            setUrl("https://profiles.myfreecams.com/" + name);
        }
        super.setName(name);
    }

    public void update(SessionState state, String streamUrl) {
        uid = Integer.parseInt(state.getUid().toString());
        setName(state.getNm());
        setCamScore(state.getM().getCamscore());
        setState(State.of(state.getVs()));
        setStreamUrl(streamUrl);

        // preview
        String uid = state.getUid().toString();
        String uidStart = uid.substring(0, 3);
        String previewUrl = "https://img.mfcimg.com/photos2/"+uidStart+'/'+uid+"/avatar.300x300.jpg";
        if(MyFreeCamsModel.this.state == State.ONLINE) {
            try {
                previewUrl = getLivePreviewUrl(state);
            } catch(Exception e) {
                LOG.error("Couldn't get live preview. Falling back to avatar", e);
            }
        }
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

    private String getLivePreviewUrl(SessionState state) {
        String previewUrl;
        int userChannel = 100000000 + state.getUid();
        int camserv = state.getU().getCamserv();
        String server = Integer.toString(camserv);
        ServerConfig sc = site.getClient().getServerConfig();
        if(sc.isOnNgServer(state)) {
            server = sc.ngVideoServers.get(Integer.toString(camserv));
            camserv = Integer.parseInt(server.replaceAll("[^0-9]+", ""));
            previewUrl = "https://snap.mfcimg.com/snapimg/" + camserv + "/320x240/mfc_" + state.getU().getPhase()+ '_' + userChannel;
        } else if(sc.isOnWzObsVideoServer(state)) {
            server = sc.wzobsServers.get(Integer.toString(camserv));
            camserv = Integer.parseInt(server.replaceAll("[^0-9]+", ""));
            previewUrl = "https://snap.mfcimg.com/snapimg/" + camserv + "/320x240/mfc_" + state.getU().getPhase()+ '_' + userChannel;
        } else if(sc.isOnHtml5VideoServer(state)) {
            server = sc.h5Servers.get(Integer.toString(camserv));
            camserv = Integer.parseInt(server.replaceAll("[^0-9]+", ""));
            previewUrl = "https://snap.mfcimg.com/snapimg/" + camserv + "/320x240/mfc_" + userChannel;
        } else {
            if(camserv > 500) camserv -= 500;
            previewUrl = "https://snap.mfcimg.com/snapimg/" + camserv + "/320x240/mfc_" + userChannel;
        }
        return previewUrl;
    }

    @Override
    public boolean follow() {
        return site.getClient().follow(getUid());
    }

    @Override
    public boolean unfollow() {
        return site.getClient().unfollow(getUid());
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

    @Override
    public Site getSite() {
        return site;
    }

    @Override
    public void readSiteSpecificData(JsonReader reader) throws IOException {
        reader.nextName();
        uid = reader.nextInt();
    }

    @Override
    public void writeSiteSpecificData(JsonWriter writer) throws IOException {
        writer.name("uid").value(uid);
    }
}
