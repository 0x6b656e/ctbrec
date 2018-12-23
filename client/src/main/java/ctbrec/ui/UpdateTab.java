package ctbrec.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.ui.CamrecApplication.Release;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class UpdateTab extends Tab {

    private static final transient Logger LOG = LoggerFactory.getLogger(UpdateTab.class);

    private WebView browser;

    public UpdateTab(Release latest) {
        setText("Update Available");
        VBox vbox = new VBox(10);
        Label l = new Label("New Version available " + latest.getVersion());
        vbox.getChildren().add(l);
        VBox.setMargin(l, new Insets(20, 0, 0, 0));
        Button button = new Button("Download");
        button.setOnAction((e) -> DesktopIntegration.open(latest.getHtmlUrl()));
        vbox.getChildren().add(button);
        VBox.setMargin(button, new Insets(0, 0, 10, 0));
        vbox.setAlignment(Pos.CENTER);

        browser = new WebView();
        try {
            WebEngine webEngine = browser.getEngine();
            webEngine.load("https://raw.githubusercontent.com/0xboobface/ctbrec/master/CHANGELOG.md");
            webEngine.setUserDataDirectory(Config.getInstance().getConfigDir());
            vbox.getChildren().add(browser);
            VBox.setVgrow(browser, Priority.ALWAYS);
        } catch (Exception e) {
            LOG.error("Couldn't load changelog", e);
        }

        setContent(vbox);
    }
}
