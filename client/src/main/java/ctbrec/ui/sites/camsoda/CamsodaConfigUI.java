package ctbrec.ui.sites.camsoda;

import ctbrec.Config;
import ctbrec.Settings;
import ctbrec.sites.camsoda.Camsoda;
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

public class CamsodaConfigUI extends AbstractConfigUI {
    private Camsoda camsoda;

    public CamsodaConfigUI(Camsoda camsoda) {
        this.camsoda = camsoda;
    }

    @Override
    public Parent createConfigPanel() {
        GridPane layout = SettingsTab.createGridLayout();
        Settings settings = Config.getInstance().getSettings();

        int row = 0;
        Label l = new Label("Active");
        layout.add(l, 0, row);
        CheckBox enabled = new CheckBox();
        enabled.setSelected(!settings.disabledSites.contains(camsoda.getName()));
        enabled.setOnAction((e) -> {
            if(enabled.isSelected()) {
                settings.disabledSites.remove(camsoda.getName());
            } else {
                settings.disabledSites.add(camsoda.getName());
            }
            save();
        });
        GridPane.setMargin(enabled, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        layout.add(enabled, 1, row++);

        layout.add(new Label("CamSoda User"), 0, row);
        TextField username = new TextField(Config.getInstance().getSettings().camsodaUsername);
        username.textProperty().addListener((ob, o, n) -> {
            Config.getInstance().getSettings().camsodaUsername = username.getText();
            save();
        });
        GridPane.setFillWidth(username, true);
        GridPane.setHgrow(username, Priority.ALWAYS);
        GridPane.setColumnSpan(username, 2);
        layout.add(username, 1, row++);

        layout.add(new Label("CamSoda Password"), 0, row);
        PasswordField password = new PasswordField();
        password.setText(Config.getInstance().getSettings().camsodaPassword);
        password.textProperty().addListener((ob, o, n) -> {
            Config.getInstance().getSettings().camsodaPassword = password.getText();
            save();
        });
        GridPane.setFillWidth(password, true);
        GridPane.setHgrow(password, Priority.ALWAYS);
        GridPane.setColumnSpan(password, 2);
        layout.add(password, 1, row++);

        Button createAccount = new Button("Create new Account");
        createAccount.setOnAction((e) -> DesktopIntegration.open(camsoda.getAffiliateLink()));
        layout.add(createAccount, 1, row++);
        GridPane.setColumnSpan(createAccount, 2);
        GridPane.setMargin(username, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        GridPane.setMargin(password, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        GridPane.setMargin(createAccount, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        return layout;
    }

}
