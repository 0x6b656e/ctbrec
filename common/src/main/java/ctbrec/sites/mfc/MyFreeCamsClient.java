package ctbrec.sites.mfc;

import static ctbrec.sites.mfc.MessageTypes.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.EvictingQueue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import ctbrec.Config;
import ctbrec.StringUtil;
import okhttp3.Cookie;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class MyFreeCamsClient {

    private static final transient Logger LOG = LoggerFactory.getLogger(MyFreeCamsClient.class);

    private static MyFreeCamsClient instance;
    private MyFreeCams mfc;
    private WebSocket ws;
    private Moshi moshi;
    private volatile boolean running = false;

    private Cache<Integer, SessionState> sessionStates = CacheBuilder.newBuilder().maximumSize(4000).build();
    private Cache<Integer, MyFreeCamsModel> models = CacheBuilder.newBuilder().maximumSize(4000).build();
    private Lock lock = new ReentrantLock();
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private ServerConfig serverConfig;
    @SuppressWarnings("unused")
    private String tkx;
    private Integer cxid;
    private int[] ctx;
    private String ctxenc;
    private String chatToken;
    private int sessionId;
    private long heartBeat;
    private volatile boolean connecting = false;
    private static int messageId = 31415; // starting with 31415 just for fun
    private Map<Integer, Consumer<Message>> responseHandlers = new HashMap<>();

    private EvictingQueue<String> receivedTextHistory = EvictingQueue.create(100);

    private MyFreeCamsClient() {
        moshi = new Moshi.Builder().build();
    }

    public static synchronized MyFreeCamsClient getInstance() {
        if (instance == null) {
            instance = new MyFreeCamsClient();
        }
        return instance;
    }

    public void setSite(MyFreeCams mfc) {
        this.mfc = mfc;
    }

    public void start() throws IOException {
        running = true;
        serverConfig = new ServerConfig(mfc);
        List<String> websocketServers = new ArrayList<String>(serverConfig.wsServers.keySet());
        String server = websocketServers.get((int) (Math.random()*websocketServers.size()));
        String wsUrl = "ws://" + server + ".myfreecams.com:8080/fcsl";
        LOG.debug("Connecting to random websocket server {}", wsUrl);

        Thread watchDog = new Thread(() -> {
            while(running) {
                if (ws == null && !connecting) {
                    LOG.info("Websocket is null. Starting a new connection");
                    Request req = new Request.Builder()
                            .url(wsUrl)
                            .addHeader("Origin", "http://m.myfreecams.com")
                            .build();
                    ws = createWebSocket(req);
                }

                try {
                    Thread.sleep(10000);
                } catch(InterruptedException e) {
                    LOG.error("WatchDog couldn't sleep", e);
                    stop();
                    running = false;
                }
            }
        });
        watchDog.setDaemon(true);
        watchDog.setName("MFC WebSocket WatchDog");
        watchDog.setPriority(Thread.MIN_PRIORITY);
        watchDog.start();
    }

    public void stop() {
        running  = false;
        ws.close(1000, "Good Bye"); // terminate normally (1000)
    }

    public List<MyFreeCamsModel> getModels() {
        lock.lock();
        try {
            LOG.trace("Models: {}", models.size());
            return new ArrayList<>(this.models.asMap().values());
        } finally {
            lock.unlock();
        }
    }

    private WebSocket createWebSocket(Request req) {
        connecting = true;
        WebSocket ws = mfc.getHttpClient().newWebSocket(req, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                super.onOpen(webSocket, response);
                try {
                    connecting = false;
                    sessionStates.invalidateAll();
                    models.invalidateAll();
                    LOG.trace("open: [{}]", response.body().string());
                    webSocket.send("hello fcserver\n");
                    webSocket.send("fcsws_20180422\n");
                    // TODO find out, what the values in the json message mean, at the moment we hust send 0s, which seems to work, too
                    // webSocket.send("1 0 0 81 0 %7B%22err%22%3A0%2C%22start%22%3A1540159843072%2C%22stop%22%3A1540159844121%2C%22a%22%3A6392%2C%22time%22%3A1540159844%2C%22key%22%3A%228da80f985c9db390809713dac71df297%22%2C%22cid%22%3A%22c504d684%22%2C%22pid%22%3A1%2C%22site%22%3A%22www%22%7D\n");
                    webSocket.send("1 0 0 81 0 %7B%22err%22%3A0%2C%22start%22%3A0%2C%22stop%22%3A0%2C%22a%22%3A0%2C%22time%22%3A0%2C%22key%22%3A%22%22%2C%22cid%22%3A%22%22%2C%22pid%22%3A1%2C%22site%22%3A%22www%22%7D\n");
                    heartBeat = System.currentTimeMillis();
                    startKeepAlive(webSocket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                super.onClosed(webSocket, code, reason);
                connecting = false;
                LOG.info("MFC websocket closed: {} {}", code, reason);
                MyFreeCamsClient.this.ws = null;
                if(!running) {
                    mfc.getHttpClient().shutdown();
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                super.onFailure(webSocket, t, response);
                connecting = false;
                if(response != null) {
                    int code = response.code();
                    String message = response.message();
                    LOG.error("MFC websocket failure: {} {}", code, message, t);
                    response.close();
                } else {
                    LOG.error("MFC websocket failure", t);
                }
                MyFreeCamsClient.this.ws = null;
            }

            private StringBuilder msgBuffer = new StringBuilder();

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                super.onMessage(webSocket, text);
                heartBeat = System.currentTimeMillis();
                receivedTextHistory.add(text);
                msgBuffer.append(text);
                Message message;
                try {
                    while( (message = parseMessage(msgBuffer)) != null) {
                        switch (message.getType()) {
                        case NULL:
                            LOG.trace("NULL websocket still alive");
                            break;
                        case LOGIN:
                            LOG.debug("LOGIN: {}", message);
                            sessionId = message.getReceiver();
                            LOG.debug("Session ID {}", sessionId);
                            break;
                        case DETAILS:
                        case ROOMHELPER:
                        case ADDFRIEND:
                        case ADDIGNORE:
                        case CMESG:
                        case PMESG:
                        case TXPROFILE:
                        case MYCAMSTATE:
                        case MYWEBCAM:
                        case JOINCHAN:
                        case SESSIONSTATE:
                            if(!message.getMessage().isEmpty()) {
                                //LOG.debug("SessionState: {}", message.getMessage());
                                JsonAdapter<SessionState> adapter = moshi.adapter(SessionState.class);
                                try {
                                    SessionState sessionState = adapter.fromJson(message.getMessage());
                                    updateSessionState(sessionState);
                                } catch (IOException e) {
                                    LOG.error("Couldn't parse session state message {}", message, e);
                                }
                            }
                            break;
                        case USERNAMELOOKUP:
                            //                            LOG.debug("{}", message.getType());
                            //                            LOG.debug("{}", message.getSender());
                            //                            LOG.debug("{}", message.getReceiver());
                            //                            LOG.debug("{}", message.getArg1());
                            //                            LOG.debug("{}", message.getArg2());
                            //                            LOG.debug("{}", message.getMessage());
                            Consumer<Message> responseHandler = responseHandlers.remove(message.getArg1());
                            if(responseHandler != null) {
                                responseHandler.accept(message);
                            }
                            break;
                        case TAGS:
                            JSONObject json = new JSONObject(message.getMessage());
                            String[] names = JSONObject.getNames(json);
                            Integer uid = Integer.parseInt(names[0]);
                            SessionState sessionState = sessionStates.getIfPresent(uid);
                            if (sessionState != null) {
                                JSONArray tags = json.getJSONArray(names[0]);
                                for (Object obj : tags) {
                                    sessionState.getM().getTags().add((String) obj);
                                }
                            }
                            break;
                        case EXTDATA:
                            if(message.getArg1() == MessageTypes.LOGIN) {
                                chatToken = message.getMessage();
                                String username = Config.getInstance().getSettings().mfcUsername;
                                if(StringUtil.isNotBlank(username)) {
                                    boolean login = mfc.getHttpClient().login();
                                    if (login) {
                                        Cookie passcode = mfc.getHttpClient().getCookie("passcode");
                                        webSocket.send("1 0 0 20071025 0 " + chatToken + "@1/" + username + ":" + passcode.value() + "\n");
                                    } else {
                                        LOG.error("Login failed");
                                        webSocket.send("1 0 0 20080909 0 guest:guest\n");
                                    }
                                } else {
                                    webSocket.send("1 0 0 20080909 0 guest:guest\n");
                                }
                            } else if(message.getArg1() == MessageTypes.MANAGELIST) {
                                requestExtData(message.getMessage());
                            } else {
                                LOG.debug("EXTDATA: {}", message);
                            }
                            break;
                        case ROOMDATA:
                            LOG.debug("ROOMDATA: {}", message);
                        case UEOPT:
                            LOG.debug("UEOPT: {}", message);
                            break;
                        case SLAVEVSHARE:
                            //                        LOG.debug("SLAVEVSHARE {}", message);
                            //                        LOG.debug("SLAVEVSHARE MSG [{}]", message.getMessage());
                            break;
                        case TKX:
                            json = new JSONObject(message.getMessage());
                            tkx = json.getString("tkx");
                            cxid = json.getInt("cxid");
                            ctxenc = URLDecoder.decode(json.getString("ctxenc"), "utf-8");
                            JSONArray ctxArray = json.getJSONArray("ctx");
                            ctx = new int[ctxArray.length()];
                            for (int i = 0; i < ctxArray.length(); i++) {
                                ctx[i] = ctxArray.getInt(i);
                            }
                            break;
                        default:
                            LOG.debug("Unknown message {}", message);
                            break;
                        }
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }

            private void requestExtData(String message) {
                try {
                    JSONObject json = new JSONObject(message);
                    long respkey = json.getInt("respkey");
                    long opts = json.getInt("opts");
                    long serv = json.getInt("serv");
                    long type = json.getInt("type");
                    String base = "http://www.myfreecams.com/php/FcwExtResp.php";
                    String url = base + "?respkey="+respkey+"&opts="+opts+"&serv="+serv+"&type="+type;
                    Request req = new Request.Builder().url(url).build();
                    LOG.trace("Requesting EXTDATA {}", url);
                    try(Response resp = mfc.getHttpClient().execute(req)) {
                        if(resp.isSuccessful()) {
                            parseExtDataSessionStates(resp.body().string());
                        }
                    }
                } catch(Exception e) {
                    LOG.warn("Couldn't request EXTDATA", e);
                }
            }

            private void parseExtDataSessionStates(String json) {
                JSONObject object = new JSONObject(json);
                if(object.has("type") && object.getInt("type") == 21) {
                    JSONArray outer = object.getJSONArray("rdata");
                    LOG.debug("{} models", outer.length());
                    for (int i = 1; i < outer.length(); i++) {
                        JSONArray inner = outer.getJSONArray(i);
                        try {
                            SessionState state = new SessionState();
                            int idx = 0;
                            state.setNm(inner.getString(idx++));
                            state.setSid(inner.getInt(idx++));
                            state.setUid(inner.getInt(idx++));
                            state.setVs(inner.getInt(idx++));
                            state.setPid(inner.getInt(idx++));
                            state.setLv(inner.getInt(idx++));
                            state.setU(new User());
                            state.getU().setCamserv(inner.getInt(idx++));
                            state.getU().setPhase(inner.getString(idx++));
                            state.getU().setChatColor(inner.getString(idx++));
                            state.getU().setChatFont(inner.getInt(idx++));
                            state.getU().setChatOpt(inner.getInt(idx++));
                            state.getU().setCreation(inner.getInt(idx++));
                            state.getU().setAvatar(inner.getInt(idx++));
                            state.getU().setProfile(inner.getInt(idx++));
                            state.getU().setPhotos(inner.getInt(idx++));
                            state.getU().setBlurb(inner.getString(idx++));
                            state.setM(new Model());
                            state.getM().setNewModel(inner.getInt(idx++));
                            state.getM().setMissmfc(inner.getInt(idx++));
                            state.getM().setCamscore(inner.getDouble(idx++));
                            state.getM().setContinent(inner.getString(idx++));
                            state.getM().setFlags(inner.getInt(idx++));
                            state.getM().setRank(inner.getInt(idx++));
                            state.getM().setRc(inner.getInt(idx++));
                            state.getM().setTopic(inner.getString(idx++));
                            state.getM().setHidecs(inner.getInt(idx++) == 1);
                            updateSessionState(state);
                        } catch(Exception e) {
                            LOG.warn("Couldn't parse session state {}", inner.toString());
                        }
                    }
                } else if(object.has("type") && object.getInt("type") == 20) {
                    JSONObject outer = object.getJSONObject("rdata");
                    for (String uidString : outer.keySet()) {
                        try {
                            int uid = Integer.parseInt(uidString);
                            MyFreeCamsModel model = getModel(uid);
                            if(model != null) {
                                model.getTags().clear();
                                JSONArray jsonTags = outer.getJSONArray(uidString);
                                jsonTags.forEach((tag) -> {
                                    model.getTags().add((String) tag);
                                });
                            }
                        } catch(Exception e) {
                            // fail silently
                        }

                    }
                }
            }

            private void updateSessionState(SessionState newState) {
                if (newState.getUid() <= 0) {
                    return;
                }
                SessionState storedState = sessionStates.getIfPresent(newState.getUid());
                if (storedState != null) {
                    storedState.merge(newState);
                    updateModel(storedState);
                } else {
                    lock.lock();
                    try {
                        sessionStates.put(newState.getUid(), newState);
                        updateModel(newState);
                    } finally {
                        lock.unlock();
                    }
                }
            }

            private void updateModel(SessionState state) {
                // essential data not yet available
                if(state.getNm() == null || state.getM() == null || state.getU() == null || state.getU().getCamserv() == null || state.getU().getCamserv() == 0) {
                    return;
                }

                // tokens not yet available
                if(ctxenc == null) {
                    return;
                }

                MyFreeCamsModel model = models.getIfPresent(state.getUid());
                if(model == null) {
                    model = mfc.createModel(state.getNm());
                    model.setUid(state.getUid());
                    models.put(state.getUid(), model);
                }
                model.update(state, getStreamUrl(state));
            }

            private Message parseMessage(StringBuilder msgBuffer) throws UnsupportedEncodingException {
                if (msgBuffer.length() < 4) {
                    // packet size not transmitted completely
                    return null;
                } else {
                    try {
                        int packetLength = Integer.parseInt(msgBuffer.substring(0, 4));
                        if (packetLength > msgBuffer.length() - 4) {
                            // packet not complete
                            return null;
                        } else {
                            msgBuffer.delete(0, 4);
                            StringBuilder rawMessage = new StringBuilder(msgBuffer.substring(0, packetLength));
                            int type = parseNextInt(rawMessage);
                            int sender = parseNextInt(rawMessage);
                            int receiver = parseNextInt(rawMessage);
                            int arg1 = parseNextInt(rawMessage);
                            int arg2 = parseNextInt(rawMessage);
                            Message message = new Message(type, sender, receiver, arg1, arg2, URLDecoder.decode(rawMessage.toString(), "utf-8"));
                            msgBuffer.delete(0, packetLength);
                            return message;
                        }
                    } catch(Exception e) {
                        LOG.error("StringBuilder contains invalid data {}", msgBuffer.toString(), e);
                        String logfile = "mfc_messages.log";
                        try(FileOutputStream fout = new FileOutputStream(logfile)) {
                            for (String string : receivedTextHistory) {
                                fout.write(string.getBytes());
                                fout.write(10);
                            }
                            //System.exit(1);
                        } catch (Exception e1) {
                            LOG.error("Couldn't write mfc message history to " + logfile);
                            e1.printStackTrace();
                        }
                        msgBuffer.setLength(0);
                        return null;
                    }
                }
            }

            private int parseNextInt(StringBuilder s) {
                int nextSpace = s.indexOf(" ");
                int i = Integer.parseInt(s.substring(0, nextSpace));
                s.delete(0, nextSpace + 1);
                return i;
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                super.onMessage(webSocket, bytes);
                LOG.debug("msgb: {}", bytes.hex());
            }
        });
        return ws;
    }

    protected boolean follow(int uid) {
        if(ws != null) {
            return ws.send(ADDFRIENDREQ + " " + sessionId + " 0 " + uid + " 1\n");
        } else {
            return false;
        }
    }

    protected boolean unfollow(int uid) {
        if(ws != null) {
            return ws.send(ADDFRIENDREQ + " " + sessionId + " 0 " + uid + " 2\n");
        } else {
            return false;
        }
    }

    private void startKeepAlive(WebSocket ws) {
        Thread keepAlive = new Thread(() ->  {
            while(running) {
                LOG.trace("--> NULL to keep the connection alive");
                try {
                    ws.send("0 0 0 0 0 -\n");

                    long millisSinceLastMessage = System.currentTimeMillis() - heartBeat;
                    if(millisSinceLastMessage > TimeUnit.MINUTES.toMillis(2)) {
                        LOG.info("No message since 2 mins. Restarting websocket");
                        ws.close(1000, "");
                        MyFreeCamsClient.this.ws = null;
                    }

                    Thread.sleep(TimeUnit.SECONDS.toMillis(15));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        keepAlive.setName("KeepAlive");
        keepAlive.setDaemon(true);
        keepAlive.start();
    }

    public void update(MyFreeCamsModel model) {
        lock.lock();
        try {
            for (SessionState state : sessionStates.asMap().values()) {
                String nm = Optional.ofNullable(state.getNm()).orElse("");
                String name = Optional.ofNullable(model.getName()).orElse("");
                if(Objects.equals(nm.toLowerCase(), name.toLowerCase()) || Objects.equals(model.getUid(), state.getUid()) && state.getUid() > 0) {
                    model.update(state, getStreamUrl(state));
                    return;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public String getStreamUrl(SessionState state) {
        Integer camserv = Optional.ofNullable(state.getU()).map(u -> u.getCamserv()).orElse(-1);
        if(camserv != null && camserv != -1) {
            int userChannel = 100000000 + state.getUid();
            String streamUrl = "";
            String phase = state.getU().getPhase() != null ? state.getU().getPhase() : "z";
            if(serverConfig.isOnNgServer(state)) {
                String server = serverConfig.ngVideoServers.get(camserv.toString());
                streamUrl = "https://" + server + ".myfreecams.com:8444/x-hls/" + cxid + '/' + userChannel + '/' + ctxenc + "/mfc_" + phase + '_' + userChannel + ".m3u8";
            } else if(serverConfig.isOnWzObsVideoServer(state)) {
                String server = serverConfig.wzobsServers.get(camserv.toString());
                streamUrl = "https://"+ server + ".myfreecams.com/NxServer/ngrp:mfc_" + phase + '_' + userChannel + ".f4v_mobile/playlist.m3u8";
            } else if(serverConfig.isOnHtml5VideoServer(state)) {
                String server = serverConfig.h5Servers.get(camserv.toString());
                streamUrl = "https://"+ server + ".myfreecams.com/NxServer/ngrp:mfc_" + userChannel + ".f4v_mobile/playlist.m3u8";
            } else {
                if(camserv > 500) {
                    camserv -= 500;
                }
                streamUrl = "https://video" + camserv + ".myfreecams.com/NxServer/ngrp:mfc_" + userChannel + ".f4v_mobile/playlist.m3u8";
            }
            return streamUrl;
        }
        return null;
    }

    public MyFreeCamsModel getModel(int uid) {
        return models.getIfPresent(uid);
    }

    public void execute(Runnable r) {
        executor.execute(r);
    }

    public void getSessionState(ctbrec.Model model) {
        for (SessionState state : sessionStates.asMap().values()) {
            if(Objects.equals(state.getNm(), model.getName())) {
                JsonAdapter<SessionState> adapter = moshi.adapter(SessionState.class).indent("  ");
                System.out.println(adapter.toJson(state));
                System.out.println(model.getPreview());
                System.out.println("H5 " + serverConfig.isOnHtml5VideoServer(state));
                System.out.println("NG " + serverConfig.isOnNgServer(state));
                System.out.println("WZ " + serverConfig.isOnWzObsVideoServer(state));
                System.out.println("#####################");
            }
        }
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public List<ctbrec.Model> search(String q) throws InterruptedException {
        LOG.debug("Sending USERNAMELOOKUP for {}", q);
        int msgId = messageId++;
        Object monitor = new Object();
        List<ctbrec.Model> result = new ArrayList<>();
        responseHandlers.put(msgId, msg -> {
            LOG.debug("Search result: " + msg);
            if(StringUtil.isNotBlank(msg.getMessage()) && !Objects.equals(msg.getMessage(), q)) {
                JSONObject json = new JSONObject(msg.getMessage());
                String name = json.getString("nm");
                MyFreeCamsModel model = mfc.createModel(name);
                model.setUid(json.getInt("uid"));
                model.setState(State.of(json.getInt("vs")));
                String uid = Integer.toString(model.getUid());
                String uidStart = uid.substring(0, 3);
                String previewUrl = "https://img.mfcimg.com/photos2/"+uidStart+'/'+uid+"/avatar.90x90.jpg";
                model.setPreview(previewUrl);
                result.add(model);
            }
            synchronized (monitor) {
                monitor.notify();
            }
        });
        ws.send("10 " + sessionId + " 0 " + msgId + " 0 " + q + "\n");
        synchronized (monitor) {
            monitor.wait();
        }

        for (MyFreeCamsModel model : models.asMap().values()) {
            if(StringUtil.isNotBlank(model.getName())) {
                if(model.getName().toLowerCase().contains(q.toLowerCase())) {
                    result.add(model);
                }
            }
        }

        return result;
    }
}
