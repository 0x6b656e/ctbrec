package ctbrec.ui.sites.camsoda;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.sites.ConfigUI;
import ctbrec.sites.camsoda.Camsoda;
import ctbrec.sites.camsoda.CamsodaHttpClient;
import ctbrec.ui.SiteUI;
import ctbrec.ui.TabProvider;
import ctbrec.ui.sites.cam4.Cam4LoginDialog;
import javafx.application.Platform;
import okhttp3.Cookie;
import okhttp3.HttpUrl;

public class CamsodaSiteUi implements SiteUI {

    private static final transient Logger LOG = LoggerFactory.getLogger(CamsodaSiteUi.class);

    private CamsodaTabProvider tabProvider;
    private CamsodaConfigUI configUi;
    private Camsoda camsoda;

    public CamsodaSiteUi(Camsoda camsoda) {
        this.camsoda = camsoda;
        tabProvider = new CamsodaTabProvider(camsoda);
        configUi = new CamsodaConfigUI(camsoda);
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
        boolean automaticLogin = camsoda.login();
        return automaticLogin;
    }


    @SuppressWarnings("unused")
    private boolean loginWithDialog() throws IOException {
        BlockingQueue<Boolean> queue = new LinkedBlockingQueue<>();

        Runnable showDialog = () -> {
            // login with javafx WebView
            CamsodaLoginDialog loginDialog = new CamsodaLoginDialog();

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

        CamsodaHttpClient httpClient = (CamsodaHttpClient)camsoda.getHttpClient();
        boolean loggedIn = httpClient.checkLoginSuccess();
        return loggedIn;
    }

    private void transferCookies(CamsodaLoginDialog loginDialog) {
        HttpUrl redirectedUrl = HttpUrl.parse(loginDialog.getUrl());
        List<Cookie> cookies = new ArrayList<>();
        for (HttpCookie webViewCookie : loginDialog.getCookies()) {
            Cookie cookie = Cookie.parse(redirectedUrl, webViewCookie.toString());
            cookies.add(cookie);
        }
        camsoda.getHttpClient().getCookieJar().saveFromResponse(redirectedUrl, cookies);

        HttpUrl origUrl = HttpUrl.parse(Cam4LoginDialog.URL);
        cookies = new ArrayList<>();
        for (HttpCookie webViewCookie : loginDialog.getCookies()) {
            Cookie cookie = Cookie.parse(origUrl, webViewCookie.toString());
            cookies.add(cookie);
        }
        camsoda.getHttpClient().getCookieJar().saveFromResponse(origUrl, cookies);
    }

}
