package ctbrec.ui;

import ctbrec.ui.Launcher.Release;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.VBox;

public class UpdateTab extends Tab {
    public UpdateTab(Release latest) {
        setText("Update Available");
        VBox vbox = new VBox(10);
        vbox.getChildren().add(new Label("New Version available " + latest.getVersion()));
        Button button = new Button("Download");
        button.setOnAction((e) -> Launcher.open(latest.getHtmlUrl()));
        vbox.getChildren().add(button);
        vbox.setAlignment(Pos.CENTER);
        setContent(vbox);
    }
}
