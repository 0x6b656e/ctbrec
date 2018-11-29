package ctbrec.sites.nood;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;

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
import ctbrec.io.HtmlParser;
import ctbrec.io.HttpException;
import ctbrec.recorder.download.StreamSource;
import okhttp3.Request;
import okhttp3.Response;

public class NoodModel extends AbstractModel {

    boolean online = false;

    @Override
    public boolean isOnline(boolean ignoreCache) throws IOException, ExecutionException, InterruptedException {
        if(ignoreCache) {
            Request request = new Request.Builder()
                    .url(getUrl())
                    .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                    .addHeader("Accept-Language", "en")
                    .addHeader("Referer", getSite().getBaseUrl())
                    .build();
            try(Response response = getSite().getHttpClient().execute(request)) {
                if(response.isSuccessful()) {
                    return response.body().string().contains("data-source");
                } else {
                    throw new HttpException(response.code(), response.message());
                }
            }
        }
        return online;
    }

    @Override
    public String getOnlineState(boolean failFast) throws IOException, ExecutionException {
        return "n/a";
    }

    @Override
    public List<StreamSource> getStreamSources() throws IOException, ExecutionException, ParseException, PlaylistException {
        String streamUrl = getStreamUrl();
        if (streamUrl == null) {
            return Collections.emptyList();
        }
        List<StreamSource> streamSources = new ArrayList<>();
        Request req = new Request.Builder()
                .url(streamUrl)
                .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                .addHeader("Accept-Language", "en")
                .addHeader("Referer", getSite().getBaseUrl())
                .addHeader("Origin", getSite().getBaseUrl())
                .build();
        try(Response response = site.getHttpClient().execute(req)) {
            if(response.isSuccessful()) {
                InputStream inputStream = response.body().byteStream();
                PlaylistParser parser = new PlaylistParser(inputStream, Format.EXT_M3U, Encoding.UTF_8, ParsingMode.LENIENT);
                Playlist playlist = parser.parse();
                MasterPlaylist master = playlist.getMasterPlaylist();
                for (PlaylistData playlistData : master.getPlaylists()) {
                    StreamSource streamsource = new StreamSource();
                    String urlBase = streamUrl.substring(0, streamUrl.lastIndexOf('/')+1);
                    streamsource.mediaPlaylistUrl = urlBase + playlistData.getUri();
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
        Request request = new Request.Builder()
                .url(getUrl())
                .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                .addHeader("Accept-Language", "en")
                .addHeader("Referer", getSite().getBaseUrl())
                .addHeader("Origin", getSite().getBaseUrl())
                .build();
        try(Response response = getSite().getHttpClient().execute(request)) {
            online = response.code() == 200;
            if(response.isSuccessful()) {
                String body = response.body().string();
                Element div = HtmlParser.getTag(body, "div[data-source]");
                JSONArray sources = new JSONArray(div.attr("data-source"));
                for (int i = 0; i < sources.length(); i++) {
                    JSONObject source = sources.getJSONObject(i);
                    String type = source.optString("type");
                    String quality = source.optString("quality");
                    String src = source.getString("src");
                    if(type.equalsIgnoreCase("application/x-mpegURL") && quality.equalsIgnoreCase("auto")) {
                        return src;
                    }
                }
                throw new RuntimeException("HLS playlist not found");
            } else {
                throw new HttpException(response.code(), response.message());
            }
        }
    }

    @Override
    public void invalidateCacheEntries() {
    }

    @Override
    public void receiveTip(int tokens) throws IOException {
    }

    @Override
    public int[] getStreamResolution(boolean failFast) throws ExecutionException {
        return new int[2];
    }

    @Override
    public boolean follow() throws IOException {
        return false;
    }

    @Override
    public boolean unfollow() throws IOException {
        return false;
    }

}
