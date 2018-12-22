package ctbrec.sites.jasmin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.time.Instant;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.io.HttpClient;
import ctbrec.recorder.download.Download;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class LiveJasminWebSocketDownload implements Download {
    private static final transient Logger LOG = LoggerFactory.getLogger(LiveJasminWebSocketDownload.class);

    private String applicationId;
    private String sessionId;
    private String jsm2SessionId;
    private String sb_ip;
    private String sb_hash;
    private String relayHost;
    private String streamHost;
    private String clientInstanceId = "01234567890123456789012345678901"; // TODO where to get or generate a random id?
    private String streamPath = "streams/clonedLiveStream";
    private WebSocket relay;
    private WebSocket stream;

    protected boolean connectionClosed;
    private volatile boolean isAlive = true;

    private HttpClient client;
    private Model model;
    private Instant startTime;
    private File targetFile;

    public LiveJasminWebSocketDownload(HttpClient client) {
        this.client = client;
    }

    @Override
    public void start(Model model, Config config) throws IOException {
        this.model = model;
        startTime = Instant.now();
        File _targetFile = config.getFileForRecording(model);
        targetFile = new File(_targetFile.getAbsolutePath().replace(".ts", ".mp4"));

        getPerformerDetails(model.getName());
        LOG.debug("appid: {}", applicationId);
        LOG.debug("sessionid: {}",sessionId);
        LOG.debug("jsm2sessionid: {}",jsm2SessionId);
        LOG.debug("sb_ip: {}",sb_ip);
        LOG.debug("sb_hash:  {}",sb_hash);
        LOG.debug("relay host: {}",relayHost);
        LOG.debug("stream host: {}",streamHost);
        LOG.debug("clientinstanceid {}",clientInstanceId);

        Request request = new Request.Builder()
                .url("https://" + relayHost + "/")
                .header("Origin", "https://www.livejasmin.com")
                .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:63.0) Gecko/20100101 Firefox/63.0")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "de,en-US;q=0.7,en;q=0.3")
                .build();
        relay = client.newWebSocket(request, new WebSocketListener() {
            boolean streamSocketStarted = false;

            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                LOG.trace("relay open {}", model.getName());
                sendToRelay("{\"event\":\"register\",\"applicationId\":\"" + applicationId
                        + "\",\"connectionData\":{\"jasmin2App\":true,\"isMobileClient\":false,\"platform\":\"desktop\",\"chatID\":\"freechat\","
                        + "\"sessionID\":\"" + sessionId + "\"," + "\"jsm2SessionId\":\"" + jsm2SessionId + "\",\"userType\":\"user\"," + "\"performerId\":\""
                        + model
                        + "\",\"clientRevision\":\"\",\"proxyIP\":\"\",\"playerVer\":\"nanoPlayerVersion: 3.10.3 appCodeName: Mozilla appName: Netscape appVersion: 5.0 (X11) platform: Linux x86_64\",\"livejasminTvmember\":false,\"newApplet\":true,\"livefeedtype\":null,\"gravityCookieId\":\"\",\"passparam\":\"\",\"brandID\":\"jasmin\",\"cobrandId\":\"\",\"subbrand\":\"livejasmin\",\"siteName\":\"LiveJasmin\",\"siteUrl\":\"https://www.livejasmin.com\","
                        + "\"clientInstanceId\":\"" + clientInstanceId + "\",\"armaVersion\":\"34.10.0\",\"isPassive\":false}}");
                response.close();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                LOG.trace("relay <-- {} T{}", model.getName(), text);
                JSONObject event = new JSONObject(text);
                if (event.optString("event").equals("accept")) {
                    new Thread(() -> {
                        sendToRelay("{\"event\":\"connectSharedObject\",\"name\":\"data/chat_so\"}");
                    }).start();
                } else if (event.optString("event").equals("updateSharedObject")) {
                    // TODO
                    JSONArray list = event.getJSONArray("list");
                    for (int i = 0; i < list.length(); i++) {
                        JSONObject obj = list.getJSONObject(i);
                        if (obj.optString("name").equals("streamList")) {
                            LOG.debug(obj.toString(2));
                            streamPath = getStreamPath(obj.getJSONObject("newValue"));
                        }
                    }

                    if (!streamSocketStarted) {
                        streamSocketStarted = true;
                        sendToRelay("{\"event\":\"call\",\"funcName\":\"makeActive\",\"data\":[]}");
                        new Thread(() -> {
                            try {
                                startStreamSocket();
                            } catch (Exception e) {
                                LOG.error("Couldn't start stream websocket", e);
                                stop();
                            }
                        }).start();
                    }
                }else if(event.optString("event").equals("call")) {
                    String func = event.optString("funcName");
                    if(func.equals("closeConnection")) {
                        connectionClosed = true;
                        //System.out.println(event.get("data"));
                        stop();
                    }
                }
            }

            private String getStreamPath(JSONObject obj) {
                String streamName  = "streams/clonedLiveStream";
                int height = 0;
                if(obj.has("streams")) {
                    JSONArray streams = obj.getJSONArray("streams");
                    for (int i = 0; i < streams.length(); i++) {
                        JSONObject stream = streams.getJSONObject(i);
                        int h = stream.optInt("height");
                        if(h > height) {
                            height = h;
                            streamName = stream.getString("streamNameWithFolder");
                            streamName = "free/" + stream.getString("name");
                        }
                    }
                }
                return streamName;
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                LOG.trace("relay <-- {} B{}", model.getName(), bytes.toString());
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                LOG.trace("relay closed {} {} {}", code, reason, model.getName());
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                if(!connectionClosed) {
                    LOG.trace("relay failure {}", model.getName(), t);
                    if (response != null) {
                        response.close();
                    }
                }
            }
        });
    }

    private void sendToRelay(String msg) {
        LOG.trace("relay --> {} {}", model.getName(), msg);
        relay.send(msg);
    }

    protected void getPerformerDetails(String name) throws IOException {
        String url = "https://m.livejasmin.com/en/chat-html5/" + name;
        Request req = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (iPhone; CPU OS 10_14 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/11.1.1 Mobile/14E304 Safari/605.1.15")
                .header("Accept", "application/json,*/*")
                .header("Accept-Language", "en")
                .header("Referer", "https://www.livejasmin.com")
                .header("X-Requested-With", "XMLHttpRequest")
                .build();
        try (Response response = client.execute(req)) {
            if (response.isSuccessful()) {
                String body = response.body().string();
                JSONObject json = new JSONObject(body);
                // System.out.println(json.toString(2));
                if (json.optBoolean("success")) {
                    JSONObject data = json.getJSONObject("data");
                    JSONObject config = data.getJSONObject("config");
                    JSONObject armageddonConfig = config.getJSONObject("armageddonConfig");
                    JSONObject chatRoom = config.getJSONObject("chatRoom");
                    sessionId = armageddonConfig.getString("sessionid");
                    jsm2SessionId = armageddonConfig.getString("jsm2session");
                    sb_hash = chatRoom.getString("sb_hash");
                    sb_ip = chatRoom.getString("sb_ip");
                    applicationId = "memberChat/jasmin" + name + sb_hash;
                    relayHost = "dss-relay-" + sb_ip.replace('.', '-') + ".dditscdn.com";
                    streamHost = "dss-live-" + sb_ip.replace('.', '-') + ".dditscdn.com";
                } else {
                    throw new IOException("Response was not successful: " + body);
                }
            } else {
                throw new IOException(response.code() + " - " + response.message());
            }
        }
    }

    private void startStreamSocket() throws UnsupportedEncodingException {
        String rtmpUrl = "rtmp://" + sb_ip + "/" + applicationId + "?sessionId-" + sessionId + "|clientInstanceId-" + clientInstanceId;
        String url = "https://" + streamHost + "/stream/?url=" + URLEncoder.encode(rtmpUrl, "utf-8");
        url = url += "&stream=" + URLEncoder.encode(streamPath, "utf-8") + "&cid=863621&pid=49247581854";
        LOG.trace(rtmpUrl);
        LOG.trace(url);

        Request request = new Request.Builder().url(url).header("Origin", "https://www.livejasmin.com")
                .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:63.0) Gecko/20100101 Firefox/63.0")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8").header("Accept-Language", "de,en-US;q=0.7,en;q=0.3")
                .build();
        stream = client.newWebSocket(request, new WebSocketListener() {
            FileOutputStream fos;

            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                LOG.trace("stream open {}", model.getName());
                // webSocket.send("{\"event\":\"ping\"}");
                // webSocket.send("");
                response.close();
                try {
                    Files.createDirectories(targetFile.getParentFile().toPath());
                    fos = new FileOutputStream(targetFile);
                } catch (IOException e) {
                    LOG.error("Couldn't create video file", e);
                    stop();
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                LOG.trace("stream <-- {} T{}", model.getName(), text);
                JSONObject event = new JSONObject(text);
                if(event.optString("eventType").equals("onRandomAccessPoint")) {
                    // send ping
                    sendToRelay("{\"event\":\"ping\"}");
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                //System.out.println("stream <-- B" + bytes.toString());
                try {
                    fos.write(bytes.toByteArray());
                } catch (IOException e) {
                    LOG.error("Couldn't write video chunk to file", e);
                    stop();
                }
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                LOG.trace("stream closed {} {} {}", code, reason, model.getName());
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                if(!connectionClosed) {
                    LOG.trace("stream failure {}", model.getName(), t);
                    if (response != null) {
                        response.close();
                    }
                }
            }
        });
    }

    @Override
    public void stop() {
        connectionClosed = true;
        stream.close(1000, "");
        relay.close(1000, "");
        isAlive = false;
    }

    @Override
    public boolean isAlive() {
        return isAlive;
    }

    @Override
    public File getTarget() {
        return targetFile;
    }

    @Override
    public Model getModel() {
        return model;
    }

    @Override
    public Instant getStartTime() {
        return startTime;
    }
}
