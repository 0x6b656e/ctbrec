package ctbrec.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.ui.Launcher.Release;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class UpdateTab extends Tab {

    private static final transient Logger LOG = LoggerFactory.getLogger(UpdateTab.class);

    private WebView browser;

    public UpdateTab(Release latest) {
        setText("Update Available");
        VBox vbox = new VBox(10);
        vbox.getChildren().add(new Label("New Version available " + latest.getVersion()));
        Button button = new Button("Download");
        button.setOnAction((e) -> Launcher.open(latest.getHtmlUrl()));
        vbox.getChildren().add(button);
        vbox.setAlignment(Pos.CENTER);

        browser = new WebView();
        try {
            WebEngine webEngine = browser.getEngine();
            webEngine.load("https://raw.githubusercontent.com/0xboobface/ctbrec/master/CHANGELOG.md");
            vbox.getChildren().add(browser);
        } catch (Exception e) {
            LOG.error("Couldn't load changelog", e);
        }

        setContent(vbox);
    }
}
