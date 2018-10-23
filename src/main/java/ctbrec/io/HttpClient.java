package ctbrec.io;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.concurrent.TimeUnit;

import ctbrec.Config;
import ctbrec.Settings.ProxyType;
import ctbrec.ui.CookieJarImpl;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public abstract class HttpClient {
    protected  OkHttpClient client;
    protected CookieJarImpl cookieJar = new CookieJarImpl();
    protected  boolean loggedIn = false;
    protected  int loginTries = 0;

    protected HttpClient() {
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
                System.setProperty("http.proxyUser", Config.getInstance().getSettings().proxyUser);
                System.setProperty("http.proxyPassword", Config.getInstance().getSettings().proxyPassword);
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
                Authenticator.setDefault(new ProxyAuth(username, password));
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
        client = new OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .connectTimeout(Config.getInstance().getSettings().httpTimeout, TimeUnit.MILLISECONDS)
                .readTimeout(Config.getInstance().getSettings().httpTimeout, TimeUnit.MILLISECONDS)
                .connectionPool(new ConnectionPool(50, 10, TimeUnit.MINUTES))
                //.addInterceptor(new LoggingInterceptor())
                .build();
    }

    public void shutdown() {
        client.connectionPool().evictAll();
        client.dispatcher().executorService().shutdown();
    }

    public static class ProxyAuth extends Authenticator {
        private PasswordAuthentication auth;

        private ProxyAuth(String user, String password) {
            auth = new PasswordAuthentication(user, password == null ? new char[]{} : password.toCharArray());
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return auth;
        }
    }
}
