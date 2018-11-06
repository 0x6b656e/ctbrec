package ctbrec.sites.cam4;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.io.HttpClient;
import javafx.application.Platform;
import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

public class Cam4HttpClient extends HttpClient {

    private static final transient Logger LOG = LoggerFactory.getLogger(Cam4HttpClient.class);

    public Cam4HttpClient() {
        super("cam4");
    }

    @Override
    public synchronized boolean login() throws IOException {
        if(loggedIn) {
            return true;
        }

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

        loggedIn = checkLoginSuccess();
        return loggedIn;
    }

    /**
     *  check, if the login worked by requesting unchecked mail
     * @throws IOException
     */
    private boolean checkLoginSuccess() throws IOException {
        String mailUrl = Cam4.BASE_URI + "/mail/unreadThreads";
        Request req = new Request.Builder()
                .url(mailUrl)
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .build();
        Response response = execute(req);
        if(response.isSuccessful() && response.body().contentLength() > 0) {
            JSONObject json = new JSONObject(response.body().string());
            return json.has("status") && Objects.equals("success", json.getString("status"));
        } else {
            response.close();
            return false;
        }
    }

    private void transferCookies(Cam4LoginDialog loginDialog) {
        HttpUrl redirectedUrl = HttpUrl.parse(loginDialog.getUrl());
        List<Cookie> cookies = new ArrayList<>();
        for (HttpCookie webViewCookie : loginDialog.getCookies()) {
            Cookie cookie = Cookie.parse(redirectedUrl, webViewCookie.toString());
            cookies.add(cookie);
        }
        cookieJar.saveFromResponse(redirectedUrl, cookies);

        HttpUrl origUrl = HttpUrl.parse(Cam4LoginDialog.URL);
        cookies = new ArrayList<>();
        for (HttpCookie webViewCookie : loginDialog.getCookies()) {
            Cookie cookie = Cookie.parse(origUrl, webViewCookie.toString());
            cookies.add(cookie);
        }
        cookieJar.saveFromResponse(origUrl, cookies);
    }

    protected int getTokenBalance() throws IOException {
        if(!loggedIn) {
            login();
        }

        throw new RuntimeException("Not implemented, yet");
    }
}
