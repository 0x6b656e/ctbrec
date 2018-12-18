package ctbrec.sites.fc2live;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.io.HttpClient;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class Fc2WebSocketClient {

    private static final transient Logger LOG = LoggerFactory.getLogger(Fc2WebSocketClient.class);
    private String url;
    private HttpClient client;

    public Fc2WebSocketClient(String url, HttpClient client) {
        this.url = url;
        this.client = client;
    }

    String playlistUrl = "";
    public String getPlaylistUrl() throws InterruptedException {
        LOG.debug("Connecting to {}", url);
        Object monitor = new Object();
        client.newWebSocket(url, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                response.close();
                webSocket.send("{\"name\":\"get_hls_information\",\"arguments\":{},\"id\":1}");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                JSONObject json = new JSONObject(text);
                if(json.optString("name").equals("_response_") && json.optInt("id") == 1) {
                    LOG.debug(json.toString(2));
                    JSONObject args = json.getJSONObject("arguments");
                    JSONArray playlists = args.getJSONArray("playlists_high_latency");
                    JSONObject playlist = playlists.getJSONObject(0);
                    playlistUrl = playlist.getString("url");
                    synchronized (monitor) {
                        monitor.notify();
                    }
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                LOG.debug("ws btxt {}", bytes.toString());
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                LOG.debug("ws closed {} - {}", code, reason);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                LOG.debug("ws failure", t);
                response.close();
                synchronized (monitor) {
                    monitor.notify();
                }
            }
        });
        synchronized (monitor) {
            monitor.wait();
        }
        return playlistUrl;
    }
}

