package ctbrec.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import ctbrec.Config;
import ctbrec.Settings.ProxyType;
import okhttp3.ConnectionPool;
import okhttp3.Cookie;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public abstract class HttpClient {
    private static final transient Logger LOG = LoggerFactory.getLogger(HttpClient.class);

    protected OkHttpClient client;
    protected CookieJarImpl cookieJar = new CookieJarImpl();
    protected boolean loggedIn = false;
    protected int loginTries = 0;
    private String name;

    protected HttpClient(String name) {
        this.name = name;
        reconfigure();
    }

    private void loadProxySettings() {
        ProxyType proxyType = Config.getInstance().getSettings().proxyType;
        switch (proxyType) {
        case HTTP:
            System.setProperty("http.proxyHost", Config.getInstance().getSettings().proxyHost);
            System.setProperty("http.proxyPort", Config.getInstance().getSettings().proxyPort);
            System.setProperty("https.proxyHost", Config.getInstance().getSettings().proxyHost);
            System.setProperty("https.proxyPort", Config.getInstance().getSettings().proxyPort);
            if(Config.getInstance().getSettings().proxyUser != null && !Config.getInstance().getSettings().proxyUser.isEmpty()) {
                String username = Config.getInstance().getSettings().proxyUser;
                String password = Config.getInstance().getSettings().proxyPassword;
                System.setProperty("http.proxyUser", username);
                System.setProperty("http.proxyPassword", password);
            }
            break;
        case SOCKS4:
            System.setProperty("socksProxyVersion", "4");
            System.setProperty("socksProxyHost", Config.getInstance().getSettings().proxyHost);
            System.setProperty("socksProxyPort", Config.getInstance().getSettings().proxyPort);
            break;
        case SOCKS5:
            System.setProperty("socksProxyVersion", "5");
            System.setProperty("socksProxyHost", Config.getInstance().getSettings().proxyHost);
            System.setProperty("socksProxyPort", Config.getInstance().getSettings().proxyPort);
            if(Config.getInstance().getSettings().proxyUser != null && !Config.getInstance().getSettings().proxyUser.isEmpty()) {
                String username = Config.getInstance().getSettings().proxyUser;
                String password = Config.getInstance().getSettings().proxyPassword;
                Authenticator.setDefault(new SocksProxyAuth(username, password));
            }
            break;
        case DIRECT:
        default:
            System.clearProperty("http.proxyHost");
            System.clearProperty("http.proxyPort");
            System.clearProperty("https.proxyHost");
            System.clearProperty("https.proxyPort");
            System.clearProperty("socksProxyVersion");
            System.clearProperty("socksProxyHost");
            System.clearProperty("socksProxyPort");
            System.clearProperty("java.net.socks.username");
            System.clearProperty("java.net.socks.password");
            System.clearProperty("http.proxyUser");
            System.clearProperty("http.proxyPassword");
            break;
        }
    }

    public Response execute(Request req) throws IOException {
        Response resp = client.newCall(req).execute();
        return resp;
    }

    public abstract boolean login() throws IOException;

    public void reconfigure() {
        loadProxySettings();
        loadCookies();
        Builder builder = new OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .connectTimeout(Config.getInstance().getSettings().httpTimeout, TimeUnit.MILLISECONDS)
                .readTimeout(Config.getInstance().getSettings().httpTimeout, TimeUnit.MILLISECONDS)
                .connectionPool(new ConnectionPool(50, 10, TimeUnit.MINUTES));
        //.addInterceptor(new LoggingInterceptor());

        ProxyType proxyType = Config.getInstance().getSettings().proxyType;
        if (proxyType == ProxyType.HTTP) {
            String username = Config.getInstance().getSettings().proxyUser;
            String password = Config.getInstance().getSettings().proxyPassword;
            if (username != null && !username.isEmpty()) {
                builder.proxyAuthenticator(createHttpProxyAuthenticator(username, password));
            }
        }

        client = builder.build();
    }

    public void shutdown() {
        persistCookies();
        client.connectionPool().evictAll();
        client.dispatcher().executorService().shutdown();
    }

    private void persistCookies() {
        try {
            CookieContainer cookies = new CookieContainer();
            cookies.putAll(cookieJar.getCookies());
            Moshi moshi = new Moshi.Builder()
                    .add(CookieContainer.class, new CookieContainerJsonAdapter())
                    .build();
            JsonAdapter<CookieContainer> adapter = moshi.adapter(CookieContainer.class).indent("  ");
            String json = adapter.toJson(cookies);

            File cookieFile = new File(Config.getInstance().getConfigDir(), "cookies-" + name + ".json");
            try(FileOutputStream fout = new FileOutputStream(cookieFile)) {
                fout.write(json.getBytes("utf-8"));
            }
        } catch (Exception e) {
            LOG.error("Couldn't persist cookies for {}", name, e);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void loadCookies() {
        try {
            File cookieFile = new File(Config.getInstance().getConfigDir(), "cookies-" + name + ".json");
            if(!cookieFile.exists()) {
                return;
            }
            byte[] jsonBytes = Files.readAllBytes(cookieFile.toPath());
            String json = new String(jsonBytes, "utf-8");

            Map<String, List<Cookie>> cookies = cookieJar.getCookies();
            Moshi moshi = new Moshi.Builder()
                    .add(CookieContainer.class, new CookieContainerJsonAdapter())
                    .build();
            JsonAdapter<CookieContainer> adapter = moshi.adapter(CookieContainer.class).indent("  ");
            CookieContainer fromJson = adapter.fromJson(json);
            Set entries = fromJson.entrySet();
            for (Object _entry : entries) {
                Entry entry = (Entry) _entry;
                cookies.put((String)entry.getKey(), (List<Cookie>)entry.getValue());
            }

        } catch (Exception e) {
            LOG.error("Couldn't load cookies for {}", name, e);
        }
    }

    public static class CookieContainer extends HashMap<String, List<Cookie>> {

    }

    private okhttp3.Authenticator createHttpProxyAuthenticator(String username, String password) {
        return new okhttp3.Authenticator() {
            @Override
            public Request authenticate(Route route, Response response) throws IOException {
                String credential = Credentials.basic(username, password);
                return response.request().newBuilder().header("Proxy-Authorization", credential).build();
            }
        };
    }

    public static class SocksProxyAuth extends Authenticator {
        private PasswordAuthentication auth;

        private SocksProxyAuth(String user, String password) {
            auth = new PasswordAuthentication(user, password == null ? new char[]{} : password.toCharArray());
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return auth;
        }
    }

    public CookieJarImpl getCookieJar() {
        return cookieJar;
    }

    public void logout() {
        getCookieJar().clear();
        loggedIn = false;
    }

    public WebSocket newWebSocket(Request request, WebSocketListener l) {
        //Request request = new Request.Builder().url(url).build();
        return client.newWebSocket(request, l);
    }
}
