package ctbrec.ui;

import java.util.List;

import javafx.scene.Scene;
import javafx.scene.control.Tab;

public abstract class TabProvider {

    public abstract List<Tab> getTabs(Scene scene);
}
