package ctbrec.sites.mfc;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.io.HtmlParser;
import ctbrec.io.HttpClient;
import ctbrec.io.HttpException;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class MyFreeCamsHttpClient extends HttpClient {

    private static final transient Logger LOG = LoggerFactory.getLogger(MyFreeCamsHttpClient.class);

    public MyFreeCamsHttpClient() {
        super("myfreecams");
    }

    @Override
    public boolean login() throws IOException {
        if(loggedIn) {
            return true;
        }

        if(checkLogin()) {
            loggedIn = true;
            LOG.debug("Logged in with cookies");
            return true;
        }

        String username = Config.getInstance().getSettings().mfcUsername;
        String password = Config.getInstance().getSettings().mfcPassword;
        RequestBody body = new FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .add("tz", "2")
                .add("ss", "1920x1080")
                .add("submit_login", "97")
                .build();
        Request req = new Request.Builder()
                .url(MyFreeCams.baseUrl + "/php/login.php")
                .header("Referer", MyFreeCams.baseUrl)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(body)
                .build();
        Response resp = execute(req);
        if(resp.isSuccessful()) {
            String page = resp.body().string();
            if(page.contains("Your username or password are incorrect")) {
                return false;
            } else {
                loggedIn = true;
                return true;
            }
        } else {
            resp.close();
            LOG.error("Login failed {} {}", resp.code(), resp.message());
            return false;
        }
    }

    private boolean checkLogin() throws IOException {
        Request req = new Request.Builder().url(MyFreeCams.baseUrl + "/php/account.php?request=status").build();
        try(Response response = execute(req)) {
            if(response.isSuccessful()) {
                String content = response.body().string();
                try {
                    Elements tags = HtmlParser.getTags(content, "div.content > p > b");
                    tags.get(2).text();
                    return true;
                } catch(Exception e) {
                    LOG.debug("Token tag not found. Login failed");
                    return false;
                }
            } else {
                throw new HttpException(response.code(), response.message());
            }
        }
    }

    public WebSocket newWebSocket(Request req, WebSocketListener webSocketListener) {
        return client.newWebSocket(req, webSocketListener);
    }

    public Cookie getCookie(String name) {
        CookieJar jar = client.cookieJar();
        HttpUrl url = HttpUrl.parse(MyFreeCams.baseUrl);
        List<Cookie> cookies = jar.loadForRequest(url);
        for (Cookie cookie : cookies) {
            if(Objects.equals(cookie.name(), name)) {
                return cookie;
            }
        }
        throw new NoSuchElementException("No cookie with name " + name);
    }
}
