package ctbrec.sites.jasmin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Random;

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

public class LiveJasminChunkedHttpDownload implements Download {

    private static final transient Logger LOG = LoggerFactory.getLogger(LiveJasminChunkedHttpDownload.class);
    private static final transient String USER_AGENT = "Mozilla/5.0 (iPhone; CPU OS 10_14 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/11.1.1 Mobile/14E304 Safari/605.1.15";

    private HttpClient client;
    private Model model;
    private Instant startTime;
    private File targetFile;

    private String applicationId;
    private String sessionId;
    private String jsm2SessionId;
    private String sb_ip;
    private String sb_hash;
    private String relayHost;
    private String hlsHost;
    private String clientInstanceId = newClientInstanceId(); // generate a 32 digit random number
    private String streamPath = "streams/clonedLiveStream";
    private boolean isAlive = true;

    public LiveJasminChunkedHttpDownload(HttpClient client) {
        this.client = client;
    }

    private String newClientInstanceId() {
        return new java.math.BigInteger(256, new Random()).toString().substring(0, 32);
    }

    @Override
    public void start(Model model, Config config) throws IOException {
        this.model = model;
        startTime = Instant.now();
        File _targetFile = config.getFileForRecording(model);
        targetFile = new File(_targetFile.getAbsolutePath().replace(".ts", ".mp4"));

        getPerformerDetails(model.getName());
        try {
            getStreamPath();
        } catch (InterruptedException e) {
            throw new IOException("Couldn't determine stream path", e);
        }

        LOG.debug("appid: {}", applicationId);
        LOG.debug("sessionid: {}", sessionId);
        LOG.debug("jsm2sessionid: {}", jsm2SessionId);
        LOG.debug("sb_ip: {}", sb_ip);
        LOG.debug("sb_hash:  {}", sb_hash);
        LOG.debug("hls host: {}", hlsHost);
        LOG.debug("clientinstanceid {}", clientInstanceId);
        LOG.debug("stream path {}", streamPath);

        String rtmpUrl = "rtmp://" + sb_ip + "/" + applicationId + "?sessionId-" + sessionId + "|clientInstanceId-" + clientInstanceId;

        String m3u8 = "https://" + hlsHost + "/h5live/http/playlist.m3u8?url=" + URLEncoder.encode(rtmpUrl, "utf-8");
        m3u8 = m3u8 += "&stream=" + URLEncoder.encode(streamPath, "utf-8");

        Request req = new Request.Builder()
                .url(m3u8)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json,*/*")
                .header("Accept-Language", "en")
                .header("Referer", model.getUrl())
                .header("X-Requested-With", "XMLHttpRequest")
                .build();
        try (Response response = client.execute(req)) {
            if (response.isSuccessful()) {
                System.out.println(response.body().string());
            } else {
                throw new IOException(response.code() + " - " + response.message());
            }
        }

        String url = "https://" + hlsHost + "/h5live/http/stream.mp4?url=" + URLEncoder.encode(rtmpUrl, "utf-8");
        url = url += "&stream=" + URLEncoder.encode(streamPath, "utf-8");

        LOG.debug("Downloading {}", url);
        req = new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json,*/*")
                .header("Accept-Language", "en")
                .header("Referer", model.getUrl())
                .header("X-Requested-With", "XMLHttpRequest")
                .build();
        try (Response response = client.execute(req)) {
            if (response.isSuccessful()) {
                FileOutputStream fos = null;
                try {
                    Files.createDirectories(targetFile.getParentFile().toPath());
                    fos = new FileOutputStream(targetFile);

                    InputStream in = response.body().byteStream();
                    byte[] b = new byte[10240];
                    int len = -1;
                    while (isAlive && (len = in.read(b)) >= 0) {
                        fos.write(b, 0, len);
                    }
                } catch (IOException e) {
                    LOG.error("Couldn't create video file", e);
                } finally {
                    isAlive = false;
                    if(fos != null) {
                        fos.close();
                    }
                }
            } else {
                throw new IOException(response.code() + " - " + response.message());
            }
        }
    }

    private void getStreamPath() throws InterruptedException {
        Object lock = new Object();

        Request request = new Request.Builder()
                .url("https://" + relayHost + "/?random=" + newClientInstanceId())
                .header("Origin", "https://www.livejasmin.com")
                .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:63.0) Gecko/20100101 Firefox/63.0")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "de,en-US;q=0.7,en;q=0.3")
                .build();
        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                LOG.debug("relay open {}", model.getName());
                webSocket.send("{\"event\":\"register\",\"applicationId\":\"" + applicationId
                        + "\",\"connectionData\":{\"jasmin2App\":true,\"isMobileClient\":false,\"platform\":\"desktop\",\"chatID\":\"freechat\","
                        + "\"sessionID\":\"" + sessionId + "\"," + "\"jsm2SessionId\":\"" + jsm2SessionId + "\",\"userType\":\"user\"," + "\"performerId\":\""
                        + model
                        + "\",\"clientRevision\":\"\",\"proxyIP\":\"\",\"playerVer\":\"nanoPlayerVersion: 3.10.3 appCodeName: Mozilla appName: Netscape appVersion: 5.0 (X11) platform: Linux x86_64\",\"livejasminTvmember\":false,\"newApplet\":true,\"livefeedtype\":null,\"gravityCookieId\":\"\",\"passparam\":\"\",\"brandID\":\"jasmin\",\"cobrandId\":\"\",\"subbrand\":\"livejasmin\",\"siteName\":\"LiveJasmin\",\"siteUrl\":\"https://www.livejasmin.com\","
                        + "\"clientInstanceId\":\"" + clientInstanceId + "\",\"armaVersion\":\"34.10.0\",\"isPassive\":false}}");
                response.close();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                LOG.debug("relay <-- {} T{}", model.getName(), text);
                JSONObject event = new JSONObject(text);
                if (event.optString("event").equals("accept")) {
                    webSocket.send("{\"event\":\"connectSharedObject\",\"name\":\"data/chat_so\"}");
                } else if (event.optString("event").equals("updateSharedObject")) {
                    JSONArray list = event.getJSONArray("list");
                    for (int i = 0; i < list.length(); i++) {
                        JSONObject obj = list.getJSONObject(i);
                        if (obj.optString("name").equals("streamList")) {
                            LOG.debug(obj.toString(2));
                            streamPath = getStreamPath(obj.getJSONObject("newValue"));
                            LOG.debug("Stream Path: {}", streamPath);
                            webSocket.send("{\"event\":\"call\",\"funcName\":\"makeActive\",\"data\":[]}");
                            webSocket.close(1000, "");
                            synchronized (lock) {
                                lock.notify();
                            }
                        }
                    }
                }else if(event.optString("event").equals("call")) {
                    String func = event.optString("funcName");
                    if(func.equals("closeConnection")) {
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
                LOG.debug("relay <-- {} B{}", model.getName(), bytes.toString());
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                LOG.debug("relay closed {} {} {}", code, reason, model.getName());
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                LOG.debug("relay failure {}", model.getName(), t);
                if (response != null) {
                    response.close();
                }
            }
        });

        synchronized (lock) {
            lock.wait();
        }
    }

    protected void getPerformerDetails(String name) throws IOException {
        String url = "https://m.livejasmin.com/en/chat-html5/" + name;
        Request req = new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
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
                    hlsHost = "dss-hls-" + sb_ip.replace('.', '-') + ".dditscdn.com";
                    relayHost = "dss-relay-" + sb_ip.replace('.', '-') + ".dditscdn.com";
                } else {
                    throw new IOException("Response was not successful: " + body);
                }
            } else {
                throw new IOException(response.code() + " - " + response.message());
            }
        }
    }

    @Override
    public void stop() {
        isAlive = false;
    }

    @Override
    public boolean isAlive() {
        return isAlive ;
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
