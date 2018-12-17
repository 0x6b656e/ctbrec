package ctbrec.sites.fc2live;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.io.HttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class Fc2HttpClient extends HttpClient {

    private static final transient Logger LOG = LoggerFactory.getLogger(Fc2HttpClient.class);

    public Fc2HttpClient() {
        super("fc2live");
    }

    @Override
    public boolean login() throws IOException {
        //        if(loggedIn) {
        //            return true;
        //        }
        //
        //        if(checkLogin()) {
        //            loggedIn = true;
        //            LOG.debug("Logged in with cookies");
        //            return true;
        //        }
        //
        //        String username = Config.getInstance().getSettings().mfcUsername;
        //        String password = Config.getInstance().getSettings().mfcPassword;
        //        RequestBody body = new FormBody.Builder()
        //                .add("username", username)
        //                .add("password", password)
        //                .add("tz", "2")
        //                .add("ss", "1920x1080")
        //                .add("submit_login", "97")
        //                .build();
        //        Request req = new Request.Builder()
        //                .url(Fc2Live.BASE_URL + "/php/login.php")
        //                .header("Referer", Fc2Live.BASE_URL)
        //                .header("Content-Type", "application/x-www-form-urlencoded")
        //                .post(body)
        //                .build();
        //        Response resp = execute(req);
        //        if(resp.isSuccessful()) {
        //            String page = resp.body().string();
        //            if(page.contains("Your username or password are incorrect")) {
        //                return false;
        //            } else {
        //                loggedIn = true;
        //                return true;
        //            }
        //        } else {
        //            resp.close();
        //            LOG.error("Login failed {} {}", resp.code(), resp.message());
        //            return false;
        //        }
        return false;
    }

    private boolean checkLogin() throws IOException {
        //        Request req = new Request.Builder().url(Fc2Live.BASE_URL + "/php/account.php?request=status").build();
        //        try(Response response = execute(req)) {
        //            if(response.isSuccessful()) {
        //                String content = response.body().string();
        //                try {
        //                    Elements tags = HtmlParser.getTags(content, "div.content > p > b");
        //                    tags.get(2).text();
        //                    return true;
        //                } catch(Exception e) {
        //                    LOG.debug("Token tag not found. Login failed");
        //                    return false;
        //                }
        //            } else {
        //                throw new HttpException(response.code(), response.message());
        //            }
        //        }
        return false;
    }

    public WebSocket newWebSocket(Request req, WebSocketListener webSocketListener) {
        return client.newWebSocket(req, webSocketListener);
    }

    //    public Cookie getCookie(String name) {
    //        CookieJar jar = client.cookieJar();
    //        HttpUrl url = HttpUrl.parse(MyFreeCams.BASE_URI);
    //        List<Cookie> cookies = jar.loadForRequest(url);
    //        for (Cookie cookie : cookies) {
    //            if(Objects.equals(cookie.name(), name)) {
    //                return cookie;
    //            }
    //        }
    //        throw new NoSuchElementException("No cookie with name " + name);
    //    }
}
