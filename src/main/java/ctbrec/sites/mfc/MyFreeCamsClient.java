package ctbrec.sites.mfc;

import static ctbrec.sites.mfc.MessageTypes.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

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

    private Map<Integer, SessionState> sessionStates = new HashMap<>();
    private Map<Integer, MyFreeCamsModel> models = new HashMap<>();
    private Lock lock = new ReentrantLock();
    private ExecutorService executor = Executors.newSingleThreadExecutor();

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
        ServerConfig serverConfig = new ServerConfig(mfc.getHttpClient());
        List<String> websocketServers = new ArrayList<String>(serverConfig.wsServers.keySet());
        String server = websocketServers.get((int) (Math.random()*websocketServers.size()));
        String wsUrl = "ws://" + server + ".myfreecams.com:8080/fcsl";
        Request req = new Request.Builder()
                .url(wsUrl)
                .addHeader("Origin", "http://m.myfreecams.com")
                .build();
        ws = createWebSocket(req);
    }

    public void stop() {
        ws.close(1000, "Good Bye"); // terminate normally (1000)
        running  = false;
    }

    public List<MyFreeCamsModel> getModels() {
        lock.lock();
        try {
            LOG.trace("Models: {}", models.size());
            return new ArrayList<>(this.models.values());
        } finally {
            lock.unlock();
        }
    }

    private WebSocket createWebSocket(Request req) {
        WebSocket ws = mfc.getHttpClient().newWebSocket(req, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                super.onOpen(webSocket, response);
                try {
                    LOG.trace("open: [{}]", response.body().string());
                    webSocket.send("hello fcserver\n");
                    //                    webSocket.send("fcsws_20180422\n");
                    //                    webSocket.send("1 0 0 81 0 %7B%22err%22%3A0%2C%22start%22%3A1540159843072%2C%22stop%22%3A1540159844121%2C%22a%22%3A6392%2C%22time%22%3A1540159844%2C%22key%22%3A%228da80f985c9db390809713dac71df297%22%2C%22cid%22%3A%22c504d684%22%2C%22pid%22%3A1%2C%22site%22%3A%22www%22%7D\n");
                    // TxCmd Sending - nType: 1, nTo: 0, nArg1: 20080909, nArg2: 0, sMsg:guest:guest
                    //                    String username = Config.getInstance().getSettings().username;
                    //                    if(username != null && !username.trim().isEmpty()) {
                    //                        mfc.getHttpClient().login();
                    //                        Cookie passcode = mfc.getHttpClient().getCookie("passcode");
                    //                        webSocket.send("1 0 0 20080909 0 "+username+":"+passcode+"\n");
                    //                    } else {
                    webSocket.send("1 0 0 20080909 0 guest:guest\n");
                    //                    }
                    startKeepAlive(webSocket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                // TODO decide what todo: is this the end of the session
                // or do we have to reconnect to keep things running?
                super.onClosed(webSocket, code, reason);
                LOG.trace("close: {} {}", code, reason);
                running = false;
                mfc.getHttpClient().shutdown();
            }

            private StringBuilder msgBuffer = new StringBuilder();

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                super.onMessage(webSocket, text);
                msgBuffer.append(text);
                Message message;
                try {
                    message = parseMessage(msgBuffer);
                    if (message != null) {
                        msgBuffer.setLength(0);
                    }

                    switch (message.getType()) {
                    case LOGIN:
                        System.out.println("LOGIN");
                        System.out.println("Sender " + message.getSender());
                        System.out.println("Receiver " + message.getReceiver());
                        System.out.println("Arg1 " + message.getArg1());
                        System.out.println("Arg2 " + message.getArg2());
                        System.out.println("Msg " + message.getMessage());
                        break;
                    case DETAILS:
                    case ROOMHELPER:
                    case ADDFRIEND:
                    case ADDIGNORE:
                    case CMESG:
                    case PMESG:
                    case TXPROFILE:
                    case USERNAMELOOKUP:
                    case MYCAMSTATE:
                    case MYWEBCAM:
                    case JOINCHAN:
                    case SESSIONSTATE:
                        if(!message.getMessage().isEmpty()) {
                            JsonAdapter<SessionState> adapter = moshi.adapter(SessionState.class);
                            try {
                                SessionState sessionState = adapter.fromJson(message.getMessage());
                                updateSessionState(sessionState);
                            } catch (IOException e) {
                                LOG.error("Couldn't parse session state message", e);
                            }
                        }
                        break;
                    case TAGS:
                        JSONObject json = new JSONObject(message.getMessage());
                        String[] names = JSONObject.getNames(json);
                        Integer uid = Integer.parseInt(names[0]);
                        SessionState sessionState = sessionStates.get(uid);
                        if (sessionState != null) {
                            JSONArray tags = json.getJSONArray(names[0]);
                            for (Object obj : tags) {
                                sessionState.getM().getTags().add((String) obj);
                            }
                        }
                        break;
                    case EXTDATA:
                        //                        if(message.getReceiver() == 0) {
                        //                            String key = message.getMessage();
                        //                            String username = Config.getInstance().getSettings().username;
                        //                            if(username != null && !username.trim().isEmpty()) {
                        //                                boolean login = mfc.getHttpClient().login();
                        //                                if(login) {
                        //                                    Cookie passcode = mfc.getHttpClient().getCookie("passcode");
                        //                                    System.out.println("1 0 0 20071025 0 "+key+"@1/"+username+":"+passcode+"\n");
                        //                                    webSocket.send("1 0 0 20071025 0 "+key+"@1/"+username+":"+passcode+"\n");
                        //                                } else {
                        //                                    LOG.error("Login failed");
                        //                                }
                        //                            } else {
                        //                                webSocket.send("1 0 0 20080909 0 guest:guest\n");
                        //                            }
                        //                        }
                        //                        System.out.println("EXTDATA");
                        //                        System.out.println("Sender " + message.getSender());
                        //                        System.out.println("Receiver " + message.getReceiver());
                        //                        System.out.println("Arg1 " + message.getArg1());
                        //                        System.out.println("Arg2 " + message.getArg2());
                        //                        System.out.println("Msg " + message.getMessage());
                        requestExtData(message.getMessage());
                        break;
                    case ROOMDATA:
                        System.out.println("ROOMDATA");
                        System.out.println("Sender " + message.getSender());
                        System.out.println("Receiver " + message.getReceiver());
                        System.out.println("Arg1 " + message.getArg1());
                        System.out.println("Arg2 " + message.getArg2());
                        System.out.println("Msg " + message.getMessage());
                    case UEOPT:
                        System.out.println("UEOPT");
                        System.out.println("Sender " + message.getSender());
                        System.out.println("Receiver " + message.getReceiver());
                        System.out.println("Arg1 " + message.getArg1());
                        System.out.println("Arg2 " + message.getArg2());
                        System.out.println("Msg " + message.getMessage());
                        break;
                    default:
                        LOG.debug("Unknown message {}", message);
                        break;
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
                    LOG.debug("Requesting EXTDATA {}", url);
                    Response resp = mfc.getHttpClient().execute(req);

                    if(resp.isSuccessful()) {
                        parseExtDataSessionStates(resp.body().string());
                    }
                } catch(Exception e) {
                    LOG.warn("Couldn't request EXTDATA", e);
                }
            }

            private void parseExtDataSessionStates(String json) {
                JSONObject object = new JSONObject(json);
                if(object.has("type") && object.getInt("type") == 21) {
                    JSONArray outer = object.getJSONArray("rdata");
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
                            idx++;
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
                SessionState storedState = sessionStates.get(newState.getUid());
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
                if(state.getNm() == null || state.getM() == null || state.getU() == null || state.getU().getCamserv() == null) {
                    return;
                }

                MyFreeCamsModel model = models.get(state.getUid());
                if(model == null) {
                    model = mfc.createModel(state.getNm());
                    model.setUid(state.getUid());
                    models.put(state.getUid(), model);
                }
                model.update(state);
            }

            private Message parseMessage(StringBuilder msg) throws UnsupportedEncodingException {
                if (msg.length() < 4) {
                    // packet size not transmitted completely
                    return null;
                } else {
                    int packetLength = Integer.parseInt(msg.substring(0, 4));
                    if (packetLength > msg.length() - 4) {
                        // packet not complete
                        return null;
                    } else {
                        msg.delete(0, 4);
                        int type = parseNextInt(msg);
                        int sender = parseNextInt(msg);
                        int receiver = parseNextInt(msg);
                        int arg1 = parseNextInt(msg);
                        int arg2 = parseNextInt(msg);
                        return new Message(type, sender, receiver, arg1, arg2, URLDecoder.decode(msg.toString(), "utf-8"));
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

    private void startKeepAlive(WebSocket ws) {
        Thread keepAlive = new Thread(() ->  {
            while(running) {
                LOG.trace("--> NULL to keep the connection alive");
                try {
                    ws.send("0 0 0 0 0 -\n");
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
            for (SessionState state : sessionStates.values()) {
                if(Objects.equals(state.getNm(), model.getName())) {
                    model.update(state);
                    return;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public MyFreeCamsModel getModel(int uid) {
        return models.get(uid);
    }

    public void execute(Runnable r) {
        executor.execute(r);
    }

    public void getSessionState(ctbrec.Model model) {
        for (SessionState state : sessionStates.values()) {
            if(Objects.equals(state.getNm(), model.getName())) {
                JsonAdapter<SessionState> adapter = moshi.adapter(SessionState.class).indent("  ");
                System.out.println(adapter.toJson(state));
                System.out.println("#####################");
            }
        }
    }
}
