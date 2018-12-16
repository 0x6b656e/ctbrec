package ctbrec.ui;

import ctbrec.Config;
import javafx.scene.control.Tab;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class WebbrowserTab extends Tab {

    public WebbrowserTab(String uri) {
        WebView browser = new WebView();
        WebEngine webEngine = browser.getEngine();
        webEngine.load(uri);
        webEngine.setUserDataDirectory(Config.getInstance().getConfigDir());
        setContent(browser);
    }
}
