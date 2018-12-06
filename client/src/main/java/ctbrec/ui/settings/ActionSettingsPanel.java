package ctbrec.ui.settings;

import ctbrec.event.Event.Type;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;

public class ActionSettingsPanel extends TitledPane {

    public ActionSettingsPanel(SettingsTab settingsTab) {
        setText("Actions");
        setExpanded(true);
        setCollapsible(false);
        createGui();
    }

    private void createGui() {
        GridPane mainLayout = SettingsTab.createGridLayout();
        setContent(mainLayout);

        int row = 0;
        for (Type type : Type.values()) {
            Label l = new Label(type.name());
            mainLayout.add(l, 0, row);
            Button b = new Button("Configure");
            mainLayout.add(b, 1, row++);
        }
    }
}
