package ctbrec.ui.sites.myfreecams;

import ctbrec.Config;
import ctbrec.Settings;
import ctbrec.sites.mfc.MyFreeCams;
import ctbrec.ui.DesktopIntegration;
import ctbrec.ui.SettingsTab;
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

public class MyFreeCamsConfigUI extends AbstractConfigUI {
    private MyFreeCams myFreeCams;

    public MyFreeCamsConfigUI(MyFreeCams myFreeCams) {
        this.myFreeCams = myFreeCams;
    }

    @Override
    public Parent createConfigPanel() {
        int row = 0;
        GridPane layout = SettingsTab.createGridLayout();
        Settings settings = Config.getInstance().getSettings();

        Label l = new Label("Active");
        layout.add(l, 0, row);
        CheckBox enabled = new CheckBox();
        enabled.setSelected(!settings.disabledSites.contains(myFreeCams.getName()));
        enabled.setOnAction((e) -> {
            if(enabled.isSelected()) {
                settings.disabledSites.remove(myFreeCams.getName());
            } else {
                settings.disabledSites.add(myFreeCams.getName());
            }
            save();
        });
        GridPane.setMargin(enabled, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        layout.add(enabled, 1, row++);

        layout.add(new Label("MyFreeCams User"), 0, row);
        TextField username = new TextField(Config.getInstance().getSettings().mfcUsername);
        username.setPrefWidth(300);
        username.textProperty().addListener((ob, o, n) -> {
            Config.getInstance().getSettings().mfcUsername = username.getText();
            save();
        });
        GridPane.setFillWidth(username, true);
        GridPane.setHgrow(username, Priority.ALWAYS);
        GridPane.setColumnSpan(username, 2);
        layout.add(username, 1, row++);

        layout.add(new Label("MyFreeCams Password"), 0, row);
        PasswordField password = new PasswordField();
        password.setText(Config.getInstance().getSettings().mfcPassword);
        password.textProperty().addListener((ob, o, n) -> {
            Config.getInstance().getSettings().mfcPassword = password.getText();
            save();
        });
        GridPane.setFillWidth(password, true);
        GridPane.setHgrow(password, Priority.ALWAYS);
        GridPane.setColumnSpan(password, 2);
        layout.add(password, 1, row++);

        layout.add(new Label("MyFreeCams Base URL"), 0, row);
        TextField baseUrl = new TextField();
        baseUrl.setText(Config.getInstance().getSettings().mfcBaseUrl);
        baseUrl.textProperty().addListener((ob, o, n) -> {
            Config.getInstance().getSettings().mfcBaseUrl = baseUrl.getText();
            save();
        });
        GridPane.setFillWidth(baseUrl, true);
        GridPane.setHgrow(baseUrl, Priority.ALWAYS);
        GridPane.setColumnSpan(baseUrl, 2);
        layout.add(baseUrl, 1, row++);

        layout.add(new Label("Ignore upscaled stream (960p)"), 0, row);
        CheckBox ignoreUpscaled = new CheckBox();
        ignoreUpscaled.setSelected(Config.getInstance().getSettings().mfcIgnoreUpscaled);
        ignoreUpscaled.selectedProperty().addListener((obs, oldV, newV) -> {
            Config.getInstance().getSettings().mfcIgnoreUpscaled = newV;
        });
        layout.add(ignoreUpscaled, 1, row++);

        Button createAccount = new Button("Create new Account");
        createAccount.setOnAction((e) -> DesktopIntegration.open(myFreeCams.getAffiliateLink()));
        layout.add(createAccount, 1, row);
        GridPane.setColumnSpan(createAccount, 2);
        GridPane.setMargin(username, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        GridPane.setMargin(password, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        GridPane.setMargin(baseUrl, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        GridPane.setMargin(ignoreUpscaled, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        GridPane.setMargin(createAccount, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));

        return layout;
    }
}
