package ctbrec.sites.bonga;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.io.HttpClient;
import javafx.application.Platform;
import okhttp3.Cookie;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class BongaCamsHttpClient extends HttpClient {

    private static final transient Logger LOG = LoggerFactory.getLogger(BongaCamsHttpClient.class);

    @Override
    public synchronized boolean login() throws IOException {
        if(loggedIn) {
            return true;
        }

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

        loggedIn = checkLoginSuccess();
        createWebSocket();
        return loggedIn;
    }

    private void createWebSocket() {
        //        $.noticeSocket = new SocketAdapter('wss://notice.bcrncdn.com:443/ws');
        //        $.noticeSocket.onopen = function(){
        //          this.send({type: 'identify', data: '0387db666178a863395c49f5f912cf070055482716514804'});
        //          $(document).trigger('onNoticeSocketOpen');
        //        };
        //        $.noticeSocket.onmessage = function(e){$(document).trigger('onNoticeSocketMessage', [e])};
        //                          $(function() {
        //                                  window.setTimeout($.checkAuth, 3600 * 5 * 1000);
        //            if ($('#email_confirmed_popup').length > 0) {
        //              $('#email_confirmed_popup').show();
        //              setTimeout(function() { $('#email_confirmed_popup').fadeOut('fast');}, 5000);
        //            }
        //                  });
        Request req = new Request.Builder()
                .url("wss://notice.bcrncdn.com:443/ws")
                .build();
        LOG.debug("Creating websocket");
        WebSocket ws = super.client.newWebSocket(req, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                super.onOpen(webSocket, response);
                try {
                    LOG.trace("open: [{}]", response.body().string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                super.onClosed(webSocket, code, reason);
                LOG.info("Bonga websocket closed: {} {}", code, reason);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                super.onFailure(webSocket, t, response);
                LOG.error("Bonga websocket failure: {} {}", response.code(), response.message(), t);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                super.onMessage(webSocket, text);
                LOG.debug("onMessage {}", text);
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                super.onMessage(webSocket, bytes);
                LOG.debug("msgb: {}", bytes.hex());
            }
        });
    }


    /**
     *  check, if the login by sending a ping request
     * @throws IOException
     */
    private boolean checkLoginSuccess() throws IOException {
        String url = BongaCams.BASE_URL + "/tools/amf.php";
        RequestBody body = new FormBody.Builder()
                //                .add("method", "getRoomData")
                //                .add("args[]", name)
                //                .add("args[]", "false")
                .add("method", "ping")
                .add("args[]", "66050808") // TODO where to get the userId
                .build();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Android 9.0; Mobile; rv:61.0) Gecko/61.0 Firefox/61.0")
                .addHeader("Accept", "application/json, text/javascript, */*")
                .addHeader("Accept-Language", "en")
                .addHeader("Referer", BongaCams.BASE_URL)
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .post(body)
                .build();
        try(Response response = execute(request)) {
            if(response.isSuccessful()) {
                JSONObject json = new JSONObject(response.body().string());
                if(json.optString("status").equals("online")) {
                    return true;
                } else {
                    throw new IOException("Request was not successful: " + json.toString(2));
                }
            } else {
                throw new IOException(response.code() + " " + response.message());
            }
        }
    }

    private void transferCookies(BongaCamsLoginDialog loginDialog) {
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

    //    @Override
    //    public boolean login() throws IOException {
    //        String url = BongaCams.BASE_URL + "/login";
    //        String dateTime = new SimpleDateFormat("d.MM.yyyy', 'HH:mm:ss").format(new Date());
    //        RequestBody body = new FormBody.Builder()
    //                .add("security_log_additional_info","{\"language\":\"en\",\"cookieEnabled\":true,\"javaEnabled\":false,\"flashVersion\":\"31.0.0\",\"dateTime\":\""+dateTime+"\",\"ips\":[\"192.168.0.1\"]}")
    //                .add("log_in[username]", Config.getInstance().getSettings().bongaUsername)
    //                .add("log_in[password]", Config.getInstance().getSettings().bongaPassword)
    //                .add("log_in[remember]", "1")
    //                .add("log_in[bfpt]", "")
    //                .add("header_form", "1")
    //                .build();
    //        Request request = new Request.Builder()
    //                .url(url)
    //                .post(body)
    //                .addHeader("User-Agent", "Mozilla/5.0 (Android 9.0; Mobile; rv:61.0) Gecko/61.0 Firefox/61.0")
    //                .addHeader("Accept","application/json")
    //                .addHeader("Accept-Language", "en")
    //                .addHeader("Referer", BongaCams.BASE_URL)
    //                .addHeader("X-Requested-With", "XMLHttpRequest")
    //                .build();
    //        try(Response response = execute(request)) {
    //            if(response.isSuccessful()) {
    //                JSONObject json = new JSONObject(response.body().string());
    //                if(json.optString("status").equals("success")) {
    //                    return true;
    //                } else {
    //                    LOG.debug("Login response: {}", json.toString(2));
    //                    Platform.runLater(() -> new BongaCamsLoginDialog());
    //                    throw new IOException("Login not successful");
    //                }
    //            } else {
    //                throw new IOException(response.code() + " " + response.message());
    //            }
    //        }
    //    }

}
