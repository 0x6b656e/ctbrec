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
                    // TxCmd Sending - nType: 1, nTo: 0, nArg1: 20080909, nArg2: 0, sMsg:guest:guest
                    webSocket.send("1 0 0 20080909 0 guest:guest\n");
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
                        LOG.trace("login");
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
                        requestExtData(message.getMessage());
                        break;
                    default:
                        LOG.trace("Unknown message {}", message);
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
                    // TODO parseTags();
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
