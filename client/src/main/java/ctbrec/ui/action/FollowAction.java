package ctbrec.ui.action;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Model;
import ctbrec.ui.controls.Dialogs;
import javafx.application.Platform;
import javafx.scene.Node;

public class FollowAction extends ModelMassEditAction {

    private static final transient Logger LOG = LoggerFactory.getLogger(FollowAction.class);

    public FollowAction(Node source, List<? extends Model> models) {
        super(source, models);
        action = (m) -> {
            try {
                m.follow();
            } catch(Exception e) {
                LOG.error("Couldn't follow model {}", m, e);
                Platform.runLater(() ->
                Dialogs.showError("Couldn't follow model", "Following " + m.getName() + " failed: " + e.getMessage(), e));
            }
        };
    }
}
