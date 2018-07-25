package ctbrec.recorder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

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
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import ctbrec.HttpClient;
import ctbrec.Model;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Chaturbate {
    private static final transient Logger LOG = LoggerFactory.getLogger(Chaturbate.class);

    public static StreamInfo getStreamInfo(Model model, HttpClient client) throws IOException {
        RequestBody body = new FormBody.Builder()
                .add("room_slug", model.getName())
                .add("bandwidth", "high")
                .build();
        Request req = new Request.Builder()
                .url("https://chaturbate.com/get_edge_hls_url_ajax/")
                .post(body)
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .build();
        Response response = client.execute(req);
        try {
            if(response.isSuccessful()) {
                String content = response.body().string();
                LOG.debug("Raw stream info: {}", content);
                Moshi moshi = new Moshi.Builder().build();
                JsonAdapter<StreamInfo> adapter = moshi.adapter(StreamInfo.class);
                StreamInfo streamInfo = adapter.fromJson(content);
                model.setOnline(Objects.equals(streamInfo.room_status, "public"));
                return streamInfo;
            } else {
                int code = response.code();
                String message = response.message();
                throw new IOException("Server responded with " + code + " - " + message + " headers: [" + response.headers() + "]");
            }
        } finally {
            response.close();
        }
    }

    public static int[] getResolution(Model model, HttpClient client) throws IOException, ParseException, PlaylistException {
        int[] res = new int[2];
        StreamInfo streamInfo = getStreamInfo(model, client);
        if(!streamInfo.url.startsWith("http")) {
            return res;
        }

        MasterPlaylist master = getMasterPlaylist(model, client);
        for (PlaylistData playlistData : master.getPlaylists()) {
            if(playlistData.hasStreamInfo() && playlistData.getStreamInfo().hasResolution()) {
                int h = playlistData.getStreamInfo().getResolution().height;
                int w = playlistData.getStreamInfo().getResolution().width;
                if(w > res[1]) {
                    res[0] = w;
                    res[1] = h;
                }
            }
        }
        return res;
    }

    public static MasterPlaylist getMasterPlaylist(Model model, HttpClient client) throws IOException, ParseException, PlaylistException {
        StreamInfo streamInfo = getStreamInfo(model, client);
        return getMasterPlaylist(streamInfo, client);
    }

    public static MasterPlaylist getMasterPlaylist(StreamInfo streamInfo, HttpClient client) throws IOException, ParseException, PlaylistException {
        LOG.trace("Loading master playlist {}", streamInfo.url);
        Request req = new Request.Builder().url(streamInfo.url).build();
        Response response = client.execute(req);
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
}
