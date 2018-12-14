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
                JSONObject ws = formats.getJSONObject("mp4-ws");
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
                JSONObject origin = hls.getJSONObject("origin");
                StreamSource src = new StreamSource();
                src.mediaPlaylistUrl = origin.getString("location");
                origin = ws.getJSONObject("origin"); // switch to web socket origin, because it has width, height and bitrates
                src.width = origin.optInt("videoWidth");
                src.height = origin.optInt("videoHeight");
                src.bandwidth = (origin.optInt("videoKbps") + origin.optInt("audioKbps")) * 1024;
                streamSources.add(src);
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
