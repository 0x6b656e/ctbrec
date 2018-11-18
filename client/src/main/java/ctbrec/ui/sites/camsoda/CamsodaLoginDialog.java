package ctbrec.ui.sites.camsoda;

import java.io.File;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.util.Base64;
import java.util.List;

import ctbrec.OS;
import ctbrec.sites.camsoda.Camsoda;
import javafx.concurrent.Worker.State;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

// FIXME this dialog does not help, because google's recaptcha does not work
// with WebView even though it does work in Cam4LoginDialog
public class CamsodaLoginDialog {

    public static final String URL = Camsoda.BASE_URI;
    private List<HttpCookie> cookies = null;
    private String url;
    private Region veil;
    private ProgressIndicator p;

    public CamsodaLoginDialog() {
        Stage stage = new Stage();
        stage.setTitle("CamSoda Login");
        InputStream icon = getClass().getResourceAsStream("/icon.png");
        stage.getIcons().add(new Image(icon));
        CookieManager cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
        WebView webView = createWebView(stage);

        veil = new Region();
        veil.setStyle("-fx-background-color: rgba(1, 1, 1)");
        p = new ProgressIndicator();
        p.setMaxSize(140, 140);

        p.setVisible(true);
        veil.visibleProperty().bind(p.visibleProperty());

        StackPane stackPane = new StackPane();
        stackPane.getChildren().addAll(webView, veil, p);

        stage.setScene(new Scene(stackPane, 400, 358));
        stage.showAndWait();
        cookies = cookieManager.getCookieStore().getCookies();
    }

    private WebView createWebView(Stage stage) {
        WebView browser = new WebView();
        WebEngine webEngine = browser.getEngine();
        webEngine.setJavaScriptEnabled(true);
        webEngine.locationProperty().addListener((obs, oldV, newV) -> {
            //                        try {
            //                            URL _url = new URL(newV);
            //                            if (Objects.equals(_url.getPath(), "/")) {
            //                                stage.close();
            //                            }
            //                        } catch (MalformedURLException e) {
            //                            LOG.error("Couldn't parse new url {}", newV, e);
            //                        }
            url = newV.toString();
            System.out.println(newV.toString());
        });
        webEngine.getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
            if (newState == State.SUCCEEDED) {
                webEngine.executeScript("document.querySelector('a[ng-click=\"signin();\"]').click()");
                p.setVisible(false);

                // TODO make this work
                //                String username = Config.getInstance().getSettings().camsodaUsername;
                //                if (username != null && !username.trim().isEmpty()) {
                //                    webEngine.executeScript("document.querySelector('input[name=\"loginUsername\"]').value = '" + username + "'");
                //                }
                //                String password = Config.getInstance().getSettings().camsodaPassword;
                //                if (password != null && !password.trim().isEmpty()) {
                //                    webEngine.executeScript("document.querySelector('input[name=\"loginPassword\"]').value = '" + password + "'");
                //                }
            } else if (newState == State.CANCELLED || newState == State.FAILED) {
                p.setVisible(false);
            }
        });

        webEngine.setUserStyleSheetLocation("data:text/css;base64," + Base64.getEncoder().encodeToString(CUSTOM_STYLE.getBytes()));
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

    private static final String CUSTOM_STYLE = ""
            + ".ngdialog.ngdialog-theme-custom { padding: 0 !important }"
            + ".ngdialog-overlay { background: black !important; }";
}
