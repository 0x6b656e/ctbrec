package ctbrec;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Settings.ProxyType;
import ctbrec.ui.CookieJarImpl;
import ctbrec.ui.CtbrecApplication;
import ctbrec.ui.HtmlParser;
import okhttp3.ConnectionPool;
import okhttp3.Cookie;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpClient {
    private static final transient Logger LOG = LoggerFactory.getLogger(HttpClient.class);
    private static HttpClient instance = new HttpClient();

    private OkHttpClient client;
    private CookieJarImpl cookieJar = new CookieJarImpl();
    private boolean loggedIn = false;
    private int loginTries = 0;
    private String token;

    private HttpClient() {
        loadProxySettings();
        client = new OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .connectTimeout(Config.getInstance().getSettings().httpTimeout, TimeUnit.SECONDS)
                .readTimeout(Config.getInstance().getSettings().httpTimeout, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(50, 10, TimeUnit.MINUTES))
                //.addInterceptor(new LoggingInterceptor())
                .build();
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
                System.setProperty("http.proxyUser", Config.getInstance().getSettings().proxyUser);
                System.setProperty("http.proxyPassword", Config.getInstance().getSettings().proxyPassword);
            }
            break;
        case SOCKS4:
            System.setProperty("socksProxyVersion", "4");
            System.setProperty("socksProxyHost", Config.getInstance().getSettings().proxyHost);
            System.setProperty("socksProxyPort", Config.getInstance().getSettings().proxyPort);
            if(Config.getInstance().getSettings().proxyUser != null && !Config.getInstance().getSettings().proxyUser.isEmpty()) {
                System.setProperty("java.net.socks.username", Config.getInstance().getSettings().proxyUser);
                System.setProperty("java.net.socks.password", Config.getInstance().getSettings().proxyPassword);
            }
            break;
        case SOCKS5:
            System.setProperty("socksProxyVersion", "5");
            System.setProperty("socksProxyHost", Config.getInstance().getSettings().proxyHost);
            System.setProperty("socksProxyPort", Config.getInstance().getSettings().proxyPort);
            if(Config.getInstance().getSettings().proxyUser != null && !Config.getInstance().getSettings().proxyUser.isEmpty()) {
                System.setProperty("java.net.socks.username", Config.getInstance().getSettings().proxyUser);
                System.setProperty("java.net.socks.password", Config.getInstance().getSettings().proxyPassword);
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

    public static HttpClient getInstance() {
        return instance;
    }

    public Response execute(Request request) throws IOException {
        Response resp = execute(request, false);
        return resp;
    }

    private void extractCsrfToken(Request request) {
        try {
            Cookie csrfToken = cookieJar.getCookie(request.url(), "csrftoken");
            token = csrfToken.value();
        } catch(NoSuchElementException e) {
            LOG.trace("CSRF token not found in cookies");
        }
    }

    public Response execute(Request req, boolean requiresLogin) throws IOException {
        if(requiresLogin && !loggedIn) {
            boolean loginSuccessful = login();
            if(!loginSuccessful) {
                throw new IOException("403 Unauthorized");
            }
        }
        Response resp = client.newCall(req).execute();
        extractCsrfToken(req);
        return resp;
    }

    public boolean login() throws IOException {
        try {
            Request login = new Request.Builder()
                    .url(CtbrecApplication.BASE_URI + "/auth/login/")
                    .build();
            Response response = client.newCall(login).execute();
            String content = response.body().string();
            token = HtmlParser.getTag(content, "input[name=csrfmiddlewaretoken]").attr("value");
            LOG.debug("csrf token is {}", token);

            RequestBody body = new FormBody.Builder()
                    .add("username", Config.getInstance().getSettings().username)
                    .add("password", Config.getInstance().getSettings().password)
                    .add("next", "")
                    .add("csrfmiddlewaretoken", token)
                    .build();
            login = new Request.Builder()
                    .url(CtbrecApplication.BASE_URI + "/auth/login/")
                    .header("Referer", CtbrecApplication.BASE_URI + "/auth/login/")
                    .post(body)
                    .build();

            response = client.newCall(login).execute();
            if(response.isSuccessful()) {
                content = response.body().string();
                if(content.contains("Login, Chaturbate login")) {
                    loggedIn = false;
                } else {
                    loggedIn = true;
                    extractCsrfToken(login);
                }
            } else {
                if(loginTries++ < 3) {
                    login();
                } else {
                    throw new IOException("Login failed: " + response.code() + " " + response.message());
                }
            }
            response.close();
        } finally {
            loginTries = 0;
        }
        return loggedIn;
    }

    public void reconfigure() {
        instance = new HttpClient();
    }

    public String getToken() throws IOException {
        if(token == null) {
            login();
        }
        return token;
    }

    public void shutdown() {
        client.connectionPool().evictAll();
        client.dispatcher().executorService().shutdown();
    }
}
