package ctbrec.ui;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.OS;
import ctbrec.ui.controls.Dialogs;
import javafx.scene.control.Tab;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class WebbrowserTab extends Tab {

    private static final transient Logger LOG = LoggerFactory.getLogger(WebbrowserTab.class);

    public WebbrowserTab(String uri) {
        WebView browser = new WebView();
        WebEngine webEngine = browser.getEngine();
        webEngine.setUserDataDirectory(new File(OS.getConfigDir(), "webengine"));
        webEngine.setJavaScriptEnabled(true);
        webEngine.load(uri);
        setContent(browser);

        webEngine.setOnError(evt -> {
            LOG.error("Couldn't load {}", uri, evt.getException());
            Dialogs.showError("Error", "Couldn't load " + uri, evt.getException());
        });
    }
}
