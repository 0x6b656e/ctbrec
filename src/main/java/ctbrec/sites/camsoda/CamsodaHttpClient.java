package ctbrec.sites.camsoda;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.io.HttpClient;
import ctbrec.sites.cam4.Cam4LoginDialog;
import ctbrec.ui.HtmlParser;
import javafx.application.Platform;
import okhttp3.Cookie;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

public class CamsodaHttpClient extends HttpClient {

    private static final transient Logger LOG = LoggerFactory.getLogger(CamsodaHttpClient.class);
    private String csrfToken = null;

    @Override
    public boolean login() throws IOException {
        if(loggedIn) {
            return true;
        }

        String url = Camsoda.BASE_URI + "/api/v1/auth/login";
        FormBody body = new FormBody.Builder()
                .add("username", Config.getInstance().getSettings().camsodaUsername)
                .add("password", Config.getInstance().getSettings().camsodaPassword)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Response response = execute(request);
        if(response.isSuccessful()) {
            JSONObject resp = new JSONObject(response.body().string());
            if(resp.has("error")) {
                String error = resp.getString("error");
                if (Objects.equals(error, "Please confirm that you are not a robot.")) {
                    //return loginWithDialog();
                    throw new IOException("CamSoda requested to solve a captcha. Please try again in a while (maybe 15 min).");
                } else {
                    throw new IOException(resp.getString("error"));
                }
            } else {
                return true;
            }
        } else {
            throw new IOException(response.code() + " " + response.message());
        }
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

        loggedIn = checkLoginSuccess();
        return loggedIn;
    }

    /**
     *  check, if the login worked
     * @throws IOException
     */
    private boolean checkLoginSuccess() throws IOException {
        String url = Camsoda.BASE_URI + "/api/v1/user/current";
        Request request = new Request.Builder().url(url).build();
        try(Response response = execute(request)) {
            if(response.isSuccessful()) {
                JSONObject resp = new JSONObject(response.body().string());
                return resp.optBoolean("status");
            } else {
                return false;
            }
        }
    }

    private void transferCookies(CamsodaLoginDialog loginDialog) {
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

    protected String getCsrfToken() throws IOException {
        if(csrfToken == null) {
            String url = Camsoda.BASE_URI;
            Request request = new Request.Builder().url(url).build();
            Response resp = execute(request, true);
            if(resp.isSuccessful()) {
                Element meta = HtmlParser.getTag(resp.body().string(), "meta[name=\"_token\"]");
                csrfToken = meta.attr("content");
            } else {
                IOException e = new IOException(resp.code() + " " + resp.message());
                resp.close();
                throw e;
            }
        }
        return csrfToken;
    }
}
