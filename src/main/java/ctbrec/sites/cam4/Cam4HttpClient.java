package ctbrec.sites.cam4;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.json.JSONObject;

import ctbrec.io.HttpClient;
import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

public class Cam4HttpClient extends HttpClient {

    @Override
    public boolean login() throws IOException {
        // login with javafx WebView
        Cam4LoginDialog loginDialog = new Cam4LoginDialog();

        // transfer cookies from WebView to OkHttp cookie jar
        transferCookies(loginDialog);

        return checkLoginSuccess();
    }

    /**
     *  check, if the login worked by requesting unchecked mail
     * @throws IOException
     */
    private boolean checkLoginSuccess() throws IOException {
        String mailUrl = "https://www.cam4.de.com/mail/unreadThreads";
        Request req = new Request.Builder().url(mailUrl).build();
        Response response = execute(req);
        if(response.isSuccessful()) {
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
}
