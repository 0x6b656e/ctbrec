package ctbrec.ui.sites.jasmin;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.sites.ConfigUI;
import ctbrec.sites.jasmin.LiveJasmin;
import ctbrec.sites.jasmin.LiveJasminHttpClient;
import ctbrec.ui.SiteUI;
import ctbrec.ui.TabProvider;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public class LiveJasminSiteUi implements SiteUI {

    private static final transient Logger LOG = LoggerFactory.getLogger(LiveJasminSiteUi.class);
    private LiveJasmin liveJasmin;
    private LiveJasminTabProvider tabProvider;
    private LiveJasminConfigUi configUi;

    public LiveJasminSiteUi(LiveJasmin liveJasmin) {
        this.liveJasmin = liveJasmin;
        tabProvider = new LiveJasminTabProvider(liveJasmin);
        configUi = new LiveJasminConfigUi(liveJasmin);
        try {
            login();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public TabProvider getTabProvider() {
        return tabProvider;
    }

    @Override
    public ConfigUI getConfigUI() {
        return configUi;
    }

    @Override
    public synchronized boolean login() throws IOException {
        boolean automaticLogin = liveJasmin.login();
        return automaticLogin;
        //        if(automaticLogin) {
        //            return true;
        //        } else {
        //            BlockingQueue<Boolean> queue = new LinkedBlockingQueue<>();
        //
        //            Runnable showDialog = () -> {
        //                // login with javafx WebView
        //                LiveJasminLoginDialog loginDialog;
        //                try {
        //                    loginDialog = new LiveJasminLoginDialog(liveJasmin);
        //                    // transfer cookies from WebView to OkHttp cookie jar
        //                    transferCookies(loginDialog);
        //                } catch (IOException e1) {
        //                    LOG.error("Couldn't load login dialog", e1);
        //                }
        //
        //                try {
        //                    queue.put(true);
        //                } catch (InterruptedException e) {
        //                    LOG.error("Error while signaling termination", e);
        //                }
        //            };
        //
        //            if(Platform.isFxApplicationThread()) {
        //                showDialog.run();
        //            } else {
        //                Platform.runLater(showDialog);
        //                try {
        //                    queue.take();
        //                } catch (InterruptedException e) {
        //                    LOG.error("Error while waiting for login dialog to close", e);
        //                    throw new IOException(e);
        //                }
        //            }
        //
        //            LiveJasminHttpClient httpClient = (LiveJasminHttpClient)liveJasmin.getHttpClient();
        //            boolean loggedIn = httpClient.checkLoginSuccess();
        //            if(loggedIn) {
        //                LOG.info("Logged in.");
        //            } else {
        //                LOG.info("Login failed");
        //            }
        //            return loggedIn;
        //        }
    }

    private void transferCookies(LiveJasminLoginDialog loginDialog) {
        LiveJasminHttpClient httpClient = (LiveJasminHttpClient)liveJasmin.getHttpClient();
        CookieJar cookieJar = httpClient.getCookieJar();

        String[] urls = {
                "https://www.livejasmin.com",
                "http://www.livejasmin.com",
                "https://m.livejasmin.com",
                "http://m.livejasmin.com",
                "https://livejasmin.com",
                "http://livejasmin.com"
        };

        for (String u : urls) {
            HttpUrl url = HttpUrl.parse(u);
            List<Cookie> cookies = new ArrayList<>();
            for (HttpCookie webViewCookie : loginDialog.getCookies()) {
                Cookie cookie = Cookie.parse(url, webViewCookie.toString());
                cookies.add(cookie);
            }
            cookieJar.saveFromResponse(url, cookies);
        }
    }
}
