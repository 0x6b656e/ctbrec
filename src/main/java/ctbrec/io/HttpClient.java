package ctbrec.io;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.List;
import java.util.Map;
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

public abstract class HttpClient {
    private static final transient Logger LOG = LoggerFactory.getLogger(HttpClient.class);

    protected  OkHttpClient client;
    protected CookieJarImpl cookieJar = new CookieJarImpl();
    protected  boolean loggedIn = false;
    protected  int loginTries = 0;
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

    public Response execute(Request request) throws IOException {
        Response resp = execute(request, false);
        return resp;
    }

    public Response execute(Request req, boolean requiresLogin) throws IOException {
        if(requiresLogin && !loggedIn) {
            loggedIn = login();
            if(!loggedIn) {
                throw new IOException("403 Unauthorized");
            }
        }
        Response resp = client.newCall(req).execute();
        return resp;
    }

    public abstract boolean login() throws IOException;

    public void reconfigure() {
        loadProxySettings();
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
            Map<String, List<Cookie>> cookies = cookieJar.getCookies();
            Moshi moshi = new Moshi.Builder().add(Cookie.class, new CookieJsonAdapter()).build();
            @SuppressWarnings("rawtypes")
            JsonAdapter<Map> adapter = moshi.adapter(Map.class).indent("  ");
            String json = adapter.toJson(cookies);
        } catch (Exception e) {
            LOG.error("Couldn't persist cookies for {}", name, e);
        }
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
}
