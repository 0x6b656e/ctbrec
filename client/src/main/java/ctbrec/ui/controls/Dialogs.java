package ctbrec.ui.controls;

import ctbrec.ui.AutosizeAlert;
import javafx.application.Platform;
import javafx.scene.control.Alert;

public class Dialogs {
    public static void showError(String header, String text, Throwable t) {
        Runnable r = () -> {
            Alert alert = new AutosizeAlert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(header);
            String content = text;
            if(t != null) {
                content += " " + t.getLocalizedMessage();
            }
            alert.setContentText(content);
            alert.showAndWait();
        };

        if(Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }
}
