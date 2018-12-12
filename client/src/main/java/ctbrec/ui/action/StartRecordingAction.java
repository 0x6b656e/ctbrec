package ctbrec.ui.action;

import java.util.List;

import ctbrec.Model;
import ctbrec.recorder.Recorder;
import ctbrec.ui.controls.Dialogs;
import javafx.application.Platform;
import javafx.scene.Node;

public class StartRecordingAction extends ModelMassEditAction {

    public StartRecordingAction(Node source, List<? extends Model> models, Recorder recorder) {
        super(source, models);
        action = (m) -> {
            try {
                recorder.startRecording(m);
            } catch(Exception e) {
                Platform.runLater(() ->
                Dialogs.showError("Couldn't start recording", "Starting recording of " + m.getName() + " failed", e));
            }
        };
    }
}
