package ctbrec.ui.sites.cam4;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.sites.ConfigUI;
import ctbrec.sites.cam4.Cam4;
import ctbrec.sites.cam4.Cam4HttpClient;
import ctbrec.ui.SiteUI;
import ctbrec.ui.TabProvider;
import javafx.application.Platform;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public class Cam4SiteUi implements SiteUI {
    private static final transient Logger LOG = LoggerFactory.getLogger(Cam4SiteUi.class);

    private Cam4TabProvider tabProvider;
    private Cam4ConfigUI configUI;
    private Cam4 cam4;

    public Cam4SiteUi(Cam4 cam4) {
        this.cam4 = cam4;
        tabProvider = new Cam4TabProvider(cam4);
        configUI = new Cam4ConfigUI(cam4);
    }

    @Override
    public TabProvider getTabProvider() {
        return tabProvider;
    }

    @Override
    public ConfigUI getConfigUI() {
        return configUI;
    }

    @Override
    public synchronized boolean login() throws IOException {
        boolean automaticLogin = cam4.login();
        if(automaticLogin) {
            return true;
        } else {

            BlockingQueue<Boolean> queue = new LinkedBlockingQueue<>();

            Runnable showDialog = () -> {
                // login with javafx WebView
                Cam4LoginDialog loginDialog = new Cam4LoginDialog();

                // transfer cookies from WebView to OkHttp cookie jar
                transferCookies(loginDialog);

                try {
                    queue.put(true);
                } catch (InterruptedException e) {
                    LOG.error("Error while signaling termination", e);
                }
            };

            if(Platform.isFxApplicationThread()) {
                showDialog.run();
            } else {
                Platform.runLater(showDialog);
                try {
                    queue.take();
                } catch (InterruptedException e) {
                    LOG.error("Error while waiting for login dialog to close", e);
                    throw new IOException(e);
                }
            }

            Cam4HttpClient httpClient = (Cam4HttpClient) cam4.getHttpClient();
            boolean loggedIn = httpClient.checkLoginSuccess();
            return loggedIn;
        }
    }


    private void transferCookies(Cam4LoginDialog loginDialog) {
        Cam4HttpClient httpClient = (Cam4HttpClient) cam4.getHttpClient();
        CookieJar cookieJar = httpClient.getCookieJar();

        HttpUrl redirectedUrl = HttpUrl.parse(loginDialog.getUrl());
        List<Cookie> cookies = new ArrayList<>();
        for (HttpCookie webViewCookie : loginDialog.getCookies()) {
            if(webViewCookie.getDomain().contains("cam4")) {
                Cookie cookie = Cookie.parse(redirectedUrl, webViewCookie.toString());
                LOG.debug("{} {} {}", webViewCookie.getDomain(), webViewCookie.getName(), webViewCookie.getValue());
                cookies.add(cookie);
            }
        }
        cookieJar.saveFromResponse(redirectedUrl, cookies);

        HttpUrl origUrl = HttpUrl.parse(Cam4LoginDialog.URL);
        cookies = new ArrayList<>();
        for (HttpCookie webViewCookie : loginDialog.getCookies()) {
            if(webViewCookie.getDomain().contains("cam4")) {
                Cookie cookie = Cookie.parse(origUrl, webViewCookie.toString());
                cookies.add(cookie);
            }
        }
        cookieJar.saveFromResponse(origUrl, cookies);
    }
}
