package ctbrec.sites.mfc;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.io.HttpClient;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class MyFreeCamsHttpClient extends HttpClient {

    private static final transient Logger LOG = LoggerFactory.getLogger(MyFreeCamsHttpClient.class);

    @Override
    public boolean login() throws IOException {
        String username = Config.getInstance().getSettings().username;
        String password = Config.getInstance().getSettings().password;
        RequestBody body = new FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .add("tz", "2")
                .add("ss", "1920x1080")
                .add("submit_login", "97")
                .build();
        Request req = new Request.Builder()
                .url(MyFreeCams.BASE_URI + "/php/login.php")
                .header("Referer", MyFreeCams.BASE_URI)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(body)
                .build();
        Response resp = execute(req);
        if(resp.isSuccessful()) {
            resp.close();
            return true;
        } else {
            resp.close();
            LOG.error("Login failed {} {}", resp.code(), resp.message());
            return false;
        }
    }

    public WebSocket newWebSocket(Request req, WebSocketListener webSocketListener) {
        return client.newWebSocket(req, webSocketListener);
    }
}
