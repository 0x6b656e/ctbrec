package ctbrec.ui.sites.cam4;

import ctbrec.Config;
import ctbrec.Settings;
import ctbrec.sites.cam4.Cam4;
import ctbrec.ui.DesktopIntegration;
import ctbrec.ui.settings.SettingsTab;
import ctbrec.ui.sites.AbstractConfigUI;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public class Cam4ConfigUI extends AbstractConfigUI {
    private Cam4 cam4;

    public Cam4ConfigUI(Cam4 cam4) {
        this.cam4 = cam4;
    }

    @Override
    public Parent createConfigPanel() {
        GridPane layout = SettingsTab.createGridLayout();
        Settings settings = Config.getInstance().getSettings();

        int row = 0;
        Label l = new Label("Active");
        layout.add(l, 0, row);
        CheckBox enabled = new CheckBox();
        enabled.setSelected(!settings.disabledSites.contains(cam4.getName()));
        enabled.setOnAction((e) -> {
            if(enabled.isSelected()) {
                settings.disabledSites.remove(cam4.getName());
            } else {
                settings.disabledSites.add(cam4.getName());
            }
            save();
        });
        GridPane.setMargin(enabled, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        layout.add(enabled, 1, row++);

        layout.add(new Label("Cam4 User"), 0, row);
        TextField username = new TextField(Config.getInstance().getSettings().cam4Username);
        username.textProperty().addListener((ob, o, n) -> {
            Config.getInstance().getSettings().cam4Username = username.getText();
            save();
        });
        GridPane.setFillWidth(username, true);
        GridPane.setHgrow(username, Priority.ALWAYS);
        GridPane.setColumnSpan(username, 2);
        layout.add(username, 1, row++);

        layout.add(new Label("Cam4 Password"), 0, row);
        PasswordField password = new PasswordField();
        password.setText(Config.getInstance().getSettings().cam4Password);
        password.focusedProperty().addListener((e) -> {
            Config.getInstance().getSettings().cam4Password = password.getText();
            save();
        });
        GridPane.setFillWidth(password, true);
        GridPane.setHgrow(password, Priority.ALWAYS);
        GridPane.setColumnSpan(password, 2);
        layout.add(password, 1, row++);

        Button createAccount = new Button("Create new Account");
        createAccount.setOnAction((e) -> DesktopIntegration.open(Cam4.AFFILIATE_LINK));
        layout.add(createAccount, 1, row++);
        GridPane.setColumnSpan(createAccount, 2);
        GridPane.setMargin(username, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        GridPane.setMargin(password, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        GridPane.setMargin(createAccount, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        return layout;
    }

}
