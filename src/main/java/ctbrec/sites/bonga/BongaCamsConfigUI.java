package ctbrec.sites.bonga;

import ctbrec.Config;
import ctbrec.sites.ConfigUI;
import ctbrec.ui.DesktopIntergation;
import ctbrec.ui.SettingsTab;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public class BongaCamsConfigUI implements ConfigUI {

    private BongaCams bongaCams;

    public BongaCamsConfigUI(BongaCams bongaCams) {
        this.bongaCams = bongaCams;
    }

    @Override
    public Parent createConfigPanel() {
        GridPane layout = SettingsTab.createGridLayout();
        layout.add(new Label("BongaCams User"), 0, 0);
        TextField username = new TextField(Config.getInstance().getSettings().bongaUsername);
        username.focusedProperty().addListener((e) -> Config.getInstance().getSettings().bongaUsername = username.getText());
        GridPane.setFillWidth(username, true);
        GridPane.setHgrow(username, Priority.ALWAYS);
        GridPane.setColumnSpan(username, 2);
        layout.add(username, 1, 0);

        layout.add(new Label("BongaCams Password"), 0, 1);
        PasswordField password = new PasswordField();
        password.setText(Config.getInstance().getSettings().bongaPassword);
        password.focusedProperty().addListener((e) -> Config.getInstance().getSettings().bongaPassword = password.getText());
        GridPane.setFillWidth(password, true);
        GridPane.setHgrow(password, Priority.ALWAYS);
        GridPane.setColumnSpan(password, 2);
        layout.add(password, 1, 1);

        Button createAccount = new Button("Create new Account");
        createAccount.setOnAction((e) -> DesktopIntergation.open(bongaCams.getAffiliateLink()));
        layout.add(createAccount, 1, 2);
        GridPane.setColumnSpan(createAccount, 2);
        GridPane.setMargin(username, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        GridPane.setMargin(password, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        GridPane.setMargin(createAccount, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        return layout;
    }

}
