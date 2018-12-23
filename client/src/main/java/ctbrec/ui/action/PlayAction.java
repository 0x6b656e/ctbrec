package ctbrec.ui.action;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.ui.Player;
import ctbrec.ui.controls.Toast;
import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.Node;

public class PlayAction {

    private Model selectedModel;
    private Node source;

    public PlayAction(Node source, Model selectedModel) {
        this.source = source;
        this.selectedModel = selectedModel;
    }

    public void execute() {
        source.setCursor(Cursor.WAIT);
        new Thread(() -> {
            boolean started = Player.play(selectedModel);
            Platform.runLater(() -> {
                if (started && Config.getInstance().getSettings().showPlayerStarting) {
                    Toast.makeText(source.getScene(), "Starting Player", 2000, 500, 500);
                }
                source.setCursor(Cursor.DEFAULT);
            });
        }).start();
    }
}
