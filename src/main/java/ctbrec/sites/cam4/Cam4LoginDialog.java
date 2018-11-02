package ctbrec.sites.cam4;

import java.io.File;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.OS;
import javafx.concurrent.Worker.State;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class Cam4LoginDialog {

    private static final transient Logger LOG = LoggerFactory.getLogger(Cam4LoginDialog.class);
    public static final String URL = Cam4.BASE_URI + "/login";
    private List<HttpCookie> cookies = null;
    private String url;
    private Region veil;
    private ProgressIndicator p;

    public Cam4LoginDialog() {
        Stage stage = new Stage();
        stage.setTitle("Cam4 Login");
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

        stage.setScene(new Scene(stackPane, 480, 854));
        stage.showAndWait();
        cookies = cookieManager.getCookieStore().getCookies();
    }

    private WebView createWebView(Stage stage) {
        WebView browser = new WebView();
        WebEngine webEngine = browser.getEngine();
        webEngine.setJavaScriptEnabled(true);
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
                try {
                    String username = Config.getInstance().getSettings().cam4Username;
                    if (username != null && !username.trim().isEmpty()) {
                        webEngine.executeScript("$('input[name=username]').attr('value','" + username + "')");
                    }
                    String password = Config.getInstance().getSettings().cam4Password;
                    if (password != null && !password.trim().isEmpty()) {
                        webEngine.executeScript("$('input[name=password]').attr('value','" + password + "')");
                    }
                    webEngine.executeScript("$('div[class~=navbar]').css('display','none')");
                    webEngine.executeScript("$('div#footer').css('display','none')");
                    webEngine.executeScript("$('div#content').css('padding','0')");
                } catch(Exception e) {
                    LOG.warn("Couldn't auto fill username and password for Cam4", e);
                }
            } else if (newState == State.CANCELLED || newState == State.FAILED) {
                veil.setVisible(false);
                p.setVisible(false);
            }
        });
        webEngine.setUserDataDirectory(new File(OS.getConfigDir(), "webengine"));
        webEngine.load(URL);
        return browser;
    }

    public List<HttpCookie> getCookies() {
        return cookies;
    }

    public String getUrl() {
        return url;
    }
}
