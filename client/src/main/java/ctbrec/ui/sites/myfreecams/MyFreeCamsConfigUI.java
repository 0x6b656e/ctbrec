package ctbrec.ui.sites.myfreecams;

import ctbrec.Config;
import ctbrec.sites.mfc.MyFreeCams;
import ctbrec.ui.DesktopIntegration;
import ctbrec.ui.SettingsTab;
import ctbrec.ui.sites.AbstractConfigUI;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
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
        GridPane layout = SettingsTab.createGridLayout();
        layout.add(new Label("MyFreeCams User"), 0, 0);
        TextField username = new TextField(Config.getInstance().getSettings().mfcUsername);
        username.textProperty().addListener((ob, o, n) -> {
            Config.getInstance().getSettings().mfcUsername = username.getText();
            save();
        });
        GridPane.setFillWidth(username, true);
        GridPane.setHgrow(username, Priority.ALWAYS);
        GridPane.setColumnSpan(username, 2);
        layout.add(username, 1, 0);

        layout.add(new Label("MyFreeCams Password"), 0, 1);
        PasswordField password = new PasswordField();
        password.setText(Config.getInstance().getSettings().mfcPassword);
        password.textProperty().addListener((ob, o, n) -> {
            Config.getInstance().getSettings().mfcPassword = password.getText();
            save();
        });
        GridPane.setFillWidth(password, true);
        GridPane.setHgrow(password, Priority.ALWAYS);
        GridPane.setColumnSpan(password, 2);
        layout.add(password, 1, 1);

        Button createAccount = new Button("Create new Account");
        createAccount.setOnAction((e) -> DesktopIntegration.open(myFreeCams.getAffiliateLink()));
        layout.add(createAccount, 1, 2);
        GridPane.setColumnSpan(createAccount, 2);
        GridPane.setMargin(username, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        GridPane.setMargin(password, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        GridPane.setMargin(createAccount, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        return layout;
    }
}
