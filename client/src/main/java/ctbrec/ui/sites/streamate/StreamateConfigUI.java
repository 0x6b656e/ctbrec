package ctbrec.ui.sites.streamate;

import ctbrec.Config;
import ctbrec.Settings;
import ctbrec.sites.streamate.Streamate;
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

public class StreamateConfigUI extends AbstractConfigUI {
    private Streamate streamate;

    public StreamateConfigUI(Streamate streamate) {
        this.streamate = streamate;
    }

    @Override
    public Parent createConfigPanel() {
        GridPane layout = SettingsTab.createGridLayout();
        Settings settings = Config.getInstance().getSettings();

        int row = 0;
        Label l = new Label("Active");
        layout.add(l, 0, row);
        CheckBox enabled = new CheckBox();
        enabled.setSelected(!settings.disabledSites.contains(streamate.getName()));
        enabled.setOnAction((e) -> {
            if(enabled.isSelected()) {
                settings.disabledSites.remove(streamate.getName());
            } else {
                settings.disabledSites.add(streamate.getName());
            }
            save();
        });
        GridPane.setMargin(enabled, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        layout.add(enabled, 1, row++);

        layout.add(new Label("Streamate User"), 0, row);
        TextField username = new TextField(settings.streamateUsername);
        username.textProperty().addListener((ob, o, n) -> {
            if(!n.equals(Config.getInstance().getSettings().streamateUsername)) {
                Config.getInstance().getSettings().streamateUsername = username.getText();
                streamate.getHttpClient().logout();
                save();
            }
        });
        GridPane.setFillWidth(username, true);
        GridPane.setHgrow(username, Priority.ALWAYS);
        GridPane.setColumnSpan(username, 2);
        layout.add(username, 1, row++);

        layout.add(new Label("Streamate Password"), 0, row);
        PasswordField password = new PasswordField();
        password.setText(settings.streamatePassword);
        password.textProperty().addListener((ob, o, n) -> {
            if(!n.equals(Config.getInstance().getSettings().streamatePassword)) {
                Config.getInstance().getSettings().streamatePassword = password.getText();
                streamate.getHttpClient().logout();
                save();
            }
        });
        GridPane.setFillWidth(password, true);
        GridPane.setHgrow(password, Priority.ALWAYS);
        GridPane.setColumnSpan(password, 2);
        layout.add(password, 1, row++);

        Button createAccount = new Button("Create new Account");
        createAccount.setOnAction((e) -> DesktopIntegration.open(streamate.getAffiliateLink()));
        layout.add(createAccount, 1, row++);
        GridPane.setColumnSpan(createAccount, 2);
        GridPane.setMargin(username, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        GridPane.setMargin(password, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        GridPane.setMargin(createAccount, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        return layout;
    }

}
