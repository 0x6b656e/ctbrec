package ctbrec.ui.sites.bonga;

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
import ctbrec.sites.bonga.BongaCams;
import javafx.concurrent.Worker.State;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class BongaCamsLoginDialog {

    private static final transient Logger LOG = LoggerFactory.getLogger(BongaCamsLoginDialog.class);
    public static final String URL = BongaCams.BASE_URL + "/login";
    private List<HttpCookie> cookies = null;
    private String url;
    private Region veil;
    private ProgressIndicator p;

    public BongaCamsLoginDialog() {
        Stage stage = new Stage();
        stage.setTitle("BongaCams Login");
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

        stage.setScene(new Scene(stackPane, 640, 480));
        stage.showAndWait();
        cookies = cookieManager.getCookieStore().getCookies();
    }

    private WebView createWebView(Stage stage) {
        WebView browser = new WebView();
        WebEngine webEngine = browser.getEngine();
        webEngine.setJavaScriptEnabled(true);
        webEngine.setUserAgent(Config.getInstance().getSettings().httpUserAgent);
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
                //System.out.println("############# " + webEngine.getLocation());
                //System.out.println(webEngine.getDocument().getDocumentElement().getTextContent());
                try {
                    String username = Config.getInstance().getSettings().bongaUsername;
                    if (username != null && !username.trim().isEmpty()) {
                        webEngine.executeScript("$('input[name=\"log_in[username]\"]').attr('value','" + username + "')");
                    }
                    String password = Config.getInstance().getSettings().bongaPassword;
                    if (password != null && !password.trim().isEmpty()) {
                        webEngine.executeScript("$('input[name=\"log_in[password]\"]').attr('value','" + password + "')");
                    }
                    webEngine.executeScript("$('div[class~=\"fancybox-overlay\"]').css('display','none')");
                    webEngine.executeScript("$('div#header').css('display','none')");
                    webEngine.executeScript("$('div.footer').css('display','none')");
                    webEngine.executeScript("$('div.footer_copy').css('display','none')");
                    webEngine.executeScript("$('div[class~=\"banner_top_index\"]').css('display','none')");
                    webEngine.executeScript("$('td.menu_container').css('display','none')");
                } catch(Exception e) {
                    LOG.warn("Couldn't auto fill username and password for BongaCams", e);
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
        //        for (HttpCookie httpCookie : cookies) {
        //            LOG.debug("Cookie: {}", httpCookie);
        //        }
        return cookies;
    }

    public String getUrl() {
        return url;
    }
}
