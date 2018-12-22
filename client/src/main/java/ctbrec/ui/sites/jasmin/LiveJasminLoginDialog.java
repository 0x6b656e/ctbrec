package ctbrec.ui.sites.jasmin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.OS;
import ctbrec.io.HttpException;
import ctbrec.sites.jasmin.LiveJasmin;
import javafx.concurrent.Worker.State;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import okhttp3.Request;
import okhttp3.Response;

public class LiveJasminLoginDialog {

    private static final transient Logger LOG = LoggerFactory.getLogger(LiveJasminLoginDialog.class);
    public static final String URL = "https://m.livejasmin.com/en/list"; // #login-modal
    private List<HttpCookie> cookies = null;
    private String url;
    private Region veil;
    private ProgressIndicator p;
    private LiveJasmin liveJasmin;

    public LiveJasminLoginDialog(LiveJasmin liveJasmin) throws IOException {
        this.liveJasmin = liveJasmin;
        Stage stage = new Stage();
        stage.setTitle("LiveJasmin Login");
        InputStream icon = getClass().getResourceAsStream("/icon.png");
        stage.getIcons().add(new Image(icon));
        CookieManager cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
        WebView webView = createWebView(stage);

        veil = new Region();
        veil.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4)");
        p = new ProgressIndicator();
        p.setMaxSize(140, 140);

        StackPane stackPane = new StackPane();
        stackPane.getChildren().addAll(webView, veil, p);

        stage.setScene(new Scene(stackPane, 360, 480));
        stage.showAndWait();
        cookies = cookieManager.getCookieStore().getCookies();
    }

    private WebView createWebView(Stage stage) throws IOException {


        WebView browser = new WebView();
        WebEngine webEngine = browser.getEngine();
        webEngine.setJavaScriptEnabled(true);
        //webEngine.setUserAgent("Mozilla/5.0 (Android 9.0; Mobile; rv:63.0) Gecko/63.0 Firefox/63.0");
        webEngine.setUserAgent("Mozilla/5.0 (Mobile; rv:30.0) Gecko/20100101 Firefox/30.0");
        webEngine.locationProperty().addListener((obs, oldV, newV) -> {
            try {
                URL _url = new URL(newV);
                if (Objects.equals(_url.getPath(), "/")) {
                    stage.close();
                }
            } catch (MalformedURLException e) {
                LOG.error("Couldn't parse new url {}", newV, e);
            }
            url = newV.toString();
        });
        webEngine.getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
            if (newState == State.SUCCEEDED) {
                veil.setVisible(false);
                p.setVisible(false);
                //                try {
                //                    //webEngine.executeScript("$('#eighteen-plus-modal').hide();");
                //                    //webEngine.executeScript("$('body').html('"+loginForm+"');");
                //                    //webEngine.executeScript("$('#listpage').append('"+loginForm+"');");
                //                    //                    webEngine.executeScript("$('#main-menu-button').click();");
                //                    //                    webEngine.executeScript("$('#login-menu').click();");
                //                    String username = Config.getInstance().getSettings().livejasminUsername;
                //                    if (username != null && !username.trim().isEmpty()) {
                //                        webEngine.executeScript("$('#username').attr('value','" + username + "')");
                //                    }
                //                    String password = Config.getInstance().getSettings().livejasminPassword;
                //                    if (password != null && !password.trim().isEmpty()) {
                //                        webEngine.executeScript("$('#password').attr('value','" + password + "')");
                //                    }
                //                } catch(Exception e) {
                //                    LOG.warn("Couldn't auto fill username and password for LiveJasmin", e);
                //                }
            } else if (newState == State.CANCELLED || newState == State.FAILED) {
                veil.setVisible(false);
                p.setVisible(false);
            }
        });
        webEngine.setUserDataDirectory(new File(OS.getConfigDir(), "webengine"));
        webEngine.load(URL);
        return browser;
    }

    private String getLoginForm() throws IOException {
        callBaseUrl(); // to get cookies
        String url = "https://m.livejasmin.com/en/auth/window/get-login-window?isAjax=1";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Android 9.0; Mobile; rv:63.0) Gecko/63.0 Firefox/63.0")
                .addHeader("Accept", "application/json, text/javascript, */*")
                .addHeader("Accept-Language", "en")
                .addHeader("Referer", LiveJasmin.BASE_URL)
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .build();
        try(Response response = liveJasmin.getHttpClient().execute(request)) {
            if(response.isSuccessful()) {
                String body = response.body().string();
                JSONObject json = new JSONObject(body);
                System.out.println(json.toString(2));
                if(json.optBoolean("success")) {
                    JSONObject data = json.getJSONObject("data");
                    return data.getString("content");
                } else {
                    throw new IOException("Request was not successful: " + body);
                }
            } else {
                throw new HttpException(response.code(), response.message());
            }
        }
    }

    private void callBaseUrl() throws IOException {
        String url = liveJasmin.getBaseUrl();
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                .build();
        try(Response response = liveJasmin.getHttpClient().execute(request)) {
            if(response.isSuccessful()) {

            } else {
                throw new HttpException(response.code(), response.message());
            }
        }
    }

    public List<HttpCookie> getCookies() {
        for (HttpCookie httpCookie : cookies) {
            LOG.debug("Cookie: {}", httpCookie);
        }
        return cookies;
    }

    public String getUrl() {
        return url;
    }
}
