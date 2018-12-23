package ctbrec.sites.streamate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.io.HttpClient;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class StreamateWebsocketClient {

    private static final transient Logger LOG = LoggerFactory.getLogger(StreamateWebsocketClient.class);
    private String url;
    private HttpClient client;

    public StreamateWebsocketClient(String url, HttpClient client) {
        this.url = url;
        this.client = client;
    }

    String roomId = "";
    public String getRoomId() throws InterruptedException {
        LOG.debug("Connecting to {}", url);
        Object monitor = new Object();
        client.newWebSocket(url, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                response.close();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                if(text.contains("NaiadAuthorized")) {
                    Matcher m = Pattern.compile("\"roomid\":\"(.*?)\"").matcher(text);
                    if(m.find()) {
                        roomId = m.group(1);
                        webSocket.close(1000, "");
                    }
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                LOG.debug("ws btxt {}", bytes.toString());
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                synchronized (monitor) {
                    monitor.notify();
                }
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
        return roomId;
    }
}

