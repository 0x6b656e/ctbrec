package ctbrec.sites.streamate;

import static ctbrec.Model.State.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iheartradio.m3u8.ParseException;
import com.iheartradio.m3u8.PlaylistException;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import ctbrec.AbstractModel;
import ctbrec.Config;
import ctbrec.io.HttpException;
import ctbrec.recorder.download.StreamSource;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

public class StreamateModel extends AbstractModel {

    private static final transient Logger LOG = LoggerFactory.getLogger(StreamateModel.class);

    private boolean online = false;
    private List<StreamSource> streamSources = new ArrayList<>();
    private int[] resolution;
    private Long id;

    @Override
    public boolean isOnline(boolean ignoreCache) throws IOException, ExecutionException, InterruptedException {
        if(ignoreCache) {
            String url = "https://sea1c-ls.naiadsystems.com/sea1c-edge-ls/80/live/s:" + getName() + ".json";
            Request req = new Request.Builder().url(url)
                    .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                    .addHeader("Accept", "*/*")
                    .addHeader("Accept-Language", "en")
                    .addHeader("Referer", Streamate.BASE_URL + '/' + getName())
                    .addHeader("X-Requested-With", "XMLHttpRequest")
                    .build();
            try(Response response = site.getHttpClient().execute(req)) {
                online = response.isSuccessful();
            }
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
        String url = "https://sea1c-ls.naiadsystems.com/sea1c-edge-ls/80/live/s:" + getName() + ".json";
        Request req = new Request.Builder().url(url)
                .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                .addHeader("Accept", "*/*")
                .addHeader("Accept-Language", "en")
                .addHeader("Referer", Streamate.BASE_URL + '/' + getName())
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .build();
        try(Response response = site.getHttpClient().execute(req)) {
            if(response.isSuccessful()) {
                JSONObject json = new JSONObject(response.body().string());
                JSONObject formats = json.getJSONObject("formats");
                JSONObject hls = formats.getJSONObject("mp4-hls");

                // add encodings
                JSONArray encodings = hls.getJSONArray("encodings");
                streamSources.clear();
                for (int i = 0; i < encodings.length(); i++) {
                    JSONObject encoding = encodings.getJSONObject(i);
                    StreamSource src = new StreamSource();
                    src.mediaPlaylistUrl = encoding.getString("location");
                    src.width = encoding.optInt("videoWidth");
                    src.height = encoding.optInt("videoHeight");
                    src.bandwidth = (encoding.optInt("videoKbps") + encoding.optInt("audioKbps")) * 1024;
                    streamSources.add(src);
                }

                // add raw source stream
                if(formats.has("mp4-ws")) {
                    JSONObject ws = formats.getJSONObject("mp4-ws");
                    JSONObject origin = hls.getJSONObject("origin");
                    StreamSource src = new StreamSource();
                    src.mediaPlaylistUrl = origin.getString("location");
                    origin = ws.getJSONObject("origin"); // switch to web socket origin, because it has width, height and bitrates
                    src.width = origin.optInt("videoWidth");
                    src.height = origin.optInt("videoHeight");
                    src.bandwidth = (origin.optInt("videoKbps") + origin.optInt("audioKbps")) * 1024;
                    streamSources.add(src);
                }
            } else {
                throw new HttpException(response.code(), response.message());
            }
        }
        return streamSources;
    }

    @Override
    public void invalidateCacheEntries() {
        resolution = null;
    }

    @Override
    public void receiveTip(int tokens) throws IOException {
        /*
        Mt._giveGoldAjax = function(e, t) {
                var n = _t.getState(),
                    a = n.nickname,
                    o = n.id,
                    i = Ds.getState(),
                    r = i.userStreamId,
                    s = i.sakey,
                    l = i.userId,
                    c = i.nickname,
                    u = "";
                switch (Ot.getState().streamType) {
                    case z.STREAM_TYPE_PRIVATE:
                    case z.STREAM_TYPE_BLOCK:
                        u = "premium";
                        break;
                    case z.STREAM_TYPE_EXCLUSIVE:
                    case z.STREAM_TYPE_BLOCK_EXCLUSIVE:
                        u = "exclusive"
                }
                if (!l) return ae.a.reject("no userId!");
                var d = {
                        amt: e,
                        isprepopulated: t,
                        modelname: a,
                        nickname: c,
                        performernickname: a,
                        sakey: s,
                        session: u,
                        smid: o,
                        streamid: r,
                        userid: l,
                        username: c
                    },
                    p = de.a.getBaseUrl() + "/api/v1/givegold/";
                return de.a.postPromise(p, d, "json")
            },
         */

        StreamateHttpClient client = (StreamateHttpClient) getSite().getHttpClient();
        client.login();
        String saKey = client.getSaKey();
        Long userId = client.getUserId();
        String nickname = client.getUserNickname();

        String url = "https://hybridclient.naiadsystems.com/api/v1/givegold/";  // this returns 404 at the moment. not sure if it's the wrong server, or if this is not used anymore
        RequestBody body = new FormBody.Builder()
                .add("amt", Integer.toString(tokens))           // amount
                .add("isprepopulated", "1")                     // ?
                .add("modelname", getName())                    // model's name
                .add("nickname", nickname)                      // user's nickname
                .add("performernickname", getName())            // model's name
                .add("sakey", saKey)                            // sakey from login
                .add("session", "")                             // is related to gold an private shows, for normal tips keep it empty
                .add("smid", Long.toString(getId()))            // model id
                .add("streamid", getStreamId())                 // id of the current stream
                .add("userid", Long.toString(userId))           // user's id
                .add("username", nickname)                      // user's nickname
                .build();
        Buffer b = new Buffer();
        body.writeTo(b);
        LOG.debug("tip params {}", b.readUtf8());
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                .addHeader("Accept", "application/json, text/javascript, */*")
                .addHeader("Accept-Language", "en")
                .addHeader("Referer", Streamate.BASE_URL + '/' + getName())
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .post(body)
                .build();
        try(Response response = site.getHttpClient().execute(request)) {
            if(response.isSuccessful()) {
                JSONObject json = new JSONObject(response.body().string());
                LOG.debug(json.toString(2));
                if(!json.optString("status").equals("success")) {
                    LOG.error("Sending tip failed {}", json.toString(2));
                    throw new IOException("Sending tip failed");
                }
            } else {
                throw new HttpException(response.code(), response.message());
            }
        }
    }

    private String getStreamId() throws IOException {
        String url = "https://hybridclient.naiadsystems.com/api/v1/config/?name=" + getName()
        + "&sabasic=&sakey=&sk=www.streamate.com&userid=0&version=6.3.17&ajax=1";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                .addHeader("Accept", "application/json, text/javascript, */*")
                .addHeader("Accept-Language", "en")
                .addHeader("Referer", Streamate.BASE_URL + '/' + getName())
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .build();
        try(Response response = site.getHttpClient().execute(request)) {
            if(response.isSuccessful()) {
                JSONObject json = new JSONObject(response.body().string());
                JSONObject stream = json.getJSONObject("stream");
                return stream.getString("streamId");
            } else {
                throw new HttpException(response.code(), response.message());
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
        return follow(true);
    }

    @Override
    public boolean unfollow() throws IOException {
        return follow(false);
    }

    private boolean follow(boolean follow) throws IOException {
        StreamateHttpClient client = (StreamateHttpClient) getSite().getHttpClient();
        client.login();
        String saKey = client.getSaKey();
        Long userId = client.getUserId();

        JSONObject requestParams = new JSONObject();
        requestParams.put("sakey", saKey);
        requestParams.put("userid", userId);
        requestParams.put("pid", id);
        requestParams.put("domain", "streamate.com");
        requestParams.put("fav", follow);
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), requestParams.toString());

        String url = site.getBaseUrl() + "/ajax/fav-notify.php?userid="+userId+"&sakey="+saKey+"&pid="+id+"&fav="+follow+"&domain=streamate.com";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                .addHeader("Accept", "application/json, */*")
                .addHeader("Accept-Language", "en")
                .addHeader("Referer", getSite().getBaseUrl())
                .post(body)
                .build();
        try(Response response = getSite().getHttpClient().execute(request)) {
            String content = response.body().string();
            if (response.isSuccessful()) {
                JSONObject json = new JSONObject(content);
                return json.optBoolean("success");
            } else {
                throw new HttpException(response.code(), response.message());
            }
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public void readSiteSpecificData(JsonReader reader) throws IOException {
        reader.nextName();
        id = reader.nextLong();
    }

    @Override
    public void writeSiteSpecificData(JsonWriter writer) throws IOException {
        writer.name("id").value(id);
    }
}