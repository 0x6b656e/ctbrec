package ctbrec.ui.sites.bonga;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.sites.ConfigUI;
import ctbrec.sites.bonga.BongaCams;
import ctbrec.sites.bonga.BongaCamsHttpClient;
import ctbrec.ui.SiteUI;
import ctbrec.ui.TabProvider;
import javafx.application.Platform;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public class BongaCamsSiteUi implements SiteUI {

    private static final transient Logger LOG = LoggerFactory.getLogger(BongaCamsSiteUi.class);
    private BongaCamsTabProvider tabProvider;
    private BongaCamsConfigUI configUi;
    private BongaCams bongaCams;

    public BongaCamsSiteUi(BongaCams bongaCams) {
        this.bongaCams = bongaCams;
        tabProvider = new BongaCamsTabProvider(bongaCams);
        configUi = new BongaCamsConfigUI(bongaCams);
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
    public boolean login() throws IOException {
        boolean automaticLogin = bongaCams.login();
        if(automaticLogin) {
            return true;
        } else {
            BlockingQueue<Boolean> queue = new LinkedBlockingQueue<>();

            Runnable showDialog = () -> {
                // login with javafx WebView
                BongaCamsLoginDialog loginDialog = new BongaCamsLoginDialog();

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

            BongaCamsHttpClient httpClient = (BongaCamsHttpClient)bongaCams.getHttpClient();
            boolean loggedIn = httpClient.checkLoginSuccess();
            if(loggedIn) {
                LOG.info("Logged in. User ID is {}", httpClient.getUserId());
            } else {
                LOG.info("Login failed");
            }
            return loggedIn;
        }
    }


    private void transferCookies(BongaCamsLoginDialog loginDialog) {
        BongaCamsHttpClient httpClient = (BongaCamsHttpClient)bongaCams.getHttpClient();
        CookieJar cookieJar = httpClient.getCookieJar();

        HttpUrl redirectedUrl = HttpUrl.parse(loginDialog.getUrl());
        List<Cookie> cookies = new ArrayList<>();
        for (HttpCookie webViewCookie : loginDialog.getCookies()) {
            Cookie cookie = Cookie.parse(redirectedUrl, webViewCookie.toString());
            cookies.add(cookie);
        }
        cookieJar.saveFromResponse(redirectedUrl, cookies);

        HttpUrl origUrl = HttpUrl.parse(BongaCamsLoginDialog.URL);
        cookies = new ArrayList<>();
        for (HttpCookie webViewCookie : loginDialog.getCookies()) {
            Cookie cookie = Cookie.parse(origUrl, webViewCookie.toString());
            cookies.add(cookie);
        }
        cookieJar.saveFromResponse(origUrl, cookies);
    }
}
