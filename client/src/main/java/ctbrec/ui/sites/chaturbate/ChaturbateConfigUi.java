package ctbrec.ui.sites.chaturbate;

import ctbrec.Config;
import ctbrec.Settings;
import ctbrec.sites.chaturbate.Chaturbate;
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

public class ChaturbateConfigUi extends AbstractConfigUI {
    private Chaturbate chaturbate;

    public ChaturbateConfigUi(Chaturbate chaturbate) {
        this.chaturbate = chaturbate;
    }

    @Override
    public Parent createConfigPanel() {
        Settings settings = Config.getInstance().getSettings();
        GridPane layout = SettingsTab.createGridLayout();

        int row = 0;
        Label l = new Label("Active");
        layout.add(l, 0, row);
        CheckBox enabled = new CheckBox();
        enabled.setSelected(!settings.disabledSites.contains(chaturbate.getName()));
        enabled.setOnAction((e) -> {
            if(enabled.isSelected()) {
                settings.disabledSites.remove(chaturbate.getName());
            } else {
                settings.disabledSites.add(chaturbate.getName());
            }
            save();
        });
        GridPane.setMargin(enabled, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        layout.add(enabled, 1, row++);

        layout.add(new Label("Chaturbate User"), 0, row);
        TextField username = new TextField(Config.getInstance().getSettings().username);
        username.textProperty().addListener((ob, o, n) -> {
            if(!n.equals(Config.getInstance().getSettings().username)) {
                Config.getInstance().getSettings().username = n;
                chaturbate.getHttpClient().logout();
                save();
            }
        });
        GridPane.setFillWidth(username, true);
        GridPane.setHgrow(username, Priority.ALWAYS);
        GridPane.setColumnSpan(username, 2);
        layout.add(username, 1, row++);

        layout.add(new Label("Chaturbate Password"), 0, row);
        PasswordField password = new PasswordField();
        password.setText(Config.getInstance().getSettings().password);
        password.textProperty().addListener((ob, o, n) -> {
            if(!n.equals(Config.getInstance().getSettings().password)) {
                Config.getInstance().getSettings().password = n;
                chaturbate.getHttpClient().logout();
                save();
            }
        });
        GridPane.setFillWidth(password, true);
        GridPane.setHgrow(password, Priority.ALWAYS);
        GridPane.setColumnSpan(password, 2);
        layout.add(password, 1, row++);

        layout.add(new Label("Chaturbate Base URL"), 0, row);
        TextField baseUrl = new TextField();
        baseUrl.setText(Config.getInstance().getSettings().chaturbateBaseUrl);
        baseUrl.textProperty().addListener((ob, o, n) -> {
            Config.getInstance().getSettings().chaturbateBaseUrl = baseUrl.getText();
            save();
        });
        GridPane.setFillWidth(baseUrl, true);
        GridPane.setHgrow(baseUrl, Priority.ALWAYS);
        GridPane.setColumnSpan(baseUrl, 2);
        layout.add(baseUrl, 1, row++);

        Button createAccount = new Button("Create new Account");
        createAccount.setOnAction((e) -> DesktopIntegration.open(Chaturbate.REGISTRATION_LINK));
        layout.add(createAccount, 1, row++);
        GridPane.setColumnSpan(createAccount, 2);
        GridPane.setMargin(username, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        GridPane.setMargin(password, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        GridPane.setMargin(baseUrl, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        GridPane.setMargin(createAccount, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));

        username.setPrefWidth(300);

        return layout;
    }
}
