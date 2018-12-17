package ctbrec.ui.sites.bonga;

import ctbrec.Config;
import ctbrec.Settings;
import ctbrec.sites.bonga.BongaCams;
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

public class BongaCamsConfigUI extends AbstractConfigUI {
    private BongaCams bongaCams;

    public BongaCamsConfigUI(BongaCams bongaCams) {
        this.bongaCams = bongaCams;
    }

    @Override
    public Parent createConfigPanel() {
        GridPane layout = SettingsTab.createGridLayout();
        Settings settings = Config.getInstance().getSettings();

        int row = 0;
        Label l = new Label("Active");
        layout.add(l, 0, row);
        CheckBox enabled = new CheckBox();
        enabled.setSelected(!settings.disabledSites.contains(bongaCams.getName()));
        enabled.setOnAction((e) -> {
            if(enabled.isSelected()) {
                settings.disabledSites.remove(bongaCams.getName());
            } else {
                settings.disabledSites.add(bongaCams.getName());
            }
            save();
        });
        GridPane.setMargin(enabled, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        layout.add(enabled, 1, row++);

        layout.add(new Label("BongaCams User"), 0, row);
        TextField username = new TextField(settings.bongaUsername);
        username.textProperty().addListener((ob, o, n) -> {
            if(!n.equals(Config.getInstance().getSettings().bongaUsername)) {
                Config.getInstance().getSettings().bongaUsername = username.getText();
                bongaCams.getHttpClient().logout();
                save();
            }
        });
        GridPane.setFillWidth(username, true);
        GridPane.setHgrow(username, Priority.ALWAYS);
        GridPane.setColumnSpan(username, 2);
        layout.add(username, 1, row++);

        layout.add(new Label("BongaCams Password"), 0, row);
        PasswordField password = new PasswordField();
        password.setText(settings.bongaPassword);
        password.textProperty().addListener((ob, o, n) -> {
            if(!n.equals(Config.getInstance().getSettings().bongaPassword)) {
                Config.getInstance().getSettings().bongaPassword = password.getText();
                bongaCams.getHttpClient().logout();
                save();
            }
        });
        GridPane.setFillWidth(password, true);
        GridPane.setHgrow(password, Priority.ALWAYS);
        GridPane.setColumnSpan(password, 2);
        layout.add(password, 1, row++);

        Button createAccount = new Button("Create new Account");
        createAccount.setOnAction((e) -> DesktopIntegration.open(bongaCams.getAffiliateLink()));
        layout.add(createAccount, 1, row++);
        GridPane.setColumnSpan(createAccount, 2);
        GridPane.setMargin(username, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        GridPane.setMargin(password, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        GridPane.setMargin(createAccount, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        return layout;
    }

}
