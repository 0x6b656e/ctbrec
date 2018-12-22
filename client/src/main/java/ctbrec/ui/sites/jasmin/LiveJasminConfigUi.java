package ctbrec.ui.sites.jasmin;

import ctbrec.Config;
import ctbrec.Settings;
import ctbrec.sites.jasmin.LiveJasmin;
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

public class LiveJasminConfigUi extends AbstractConfigUI {
    private LiveJasmin liveJasmin;

    public LiveJasminConfigUi(LiveJasmin liveJasmin) {
        this.liveJasmin = liveJasmin;
    }

    @Override
    public Parent createConfigPanel() {
        Settings settings = Config.getInstance().getSettings();
        GridPane layout = SettingsTab.createGridLayout();

        int row = 0;
        Label l = new Label("Active");
        layout.add(l, 0, row);
        CheckBox enabled = new CheckBox();
        enabled.setSelected(!settings.disabledSites.contains(liveJasmin.getName()));
        enabled.setOnAction((e) -> {
            if(enabled.isSelected()) {
                settings.disabledSites.remove(liveJasmin.getName());
            } else {
                settings.disabledSites.add(liveJasmin.getName());
            }
            save();
        });
        GridPane.setMargin(enabled, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        layout.add(enabled, 1, row++);

        layout.add(new Label("LiveJasmin User"), 0, row);
        TextField username = new TextField(Config.getInstance().getSettings().livejasminUsername);
        username.textProperty().addListener((ob, o, n) -> {
            if(!n.equals(Config.getInstance().getSettings().livejasminUsername)) {
                Config.getInstance().getSettings().livejasminUsername = n;
                liveJasmin.getHttpClient().logout();
                save();
            }
        });
        GridPane.setFillWidth(username, true);
        GridPane.setHgrow(username, Priority.ALWAYS);
        GridPane.setColumnSpan(username, 2);
        layout.add(username, 1, row++);

        layout.add(new Label("LiveJasmin Password"), 0, row);
        PasswordField password = new PasswordField();
        password.setText(Config.getInstance().getSettings().livejasminPassword);
        password.textProperty().addListener((ob, o, n) -> {
            if(!n.equals(Config.getInstance().getSettings().livejasminPassword)) {
                Config.getInstance().getSettings().livejasminPassword = n;
                liveJasmin.getHttpClient().logout();
                save();
            }
        });
        GridPane.setFillWidth(password, true);
        GridPane.setHgrow(password, Priority.ALWAYS);
        GridPane.setColumnSpan(password, 2);
        layout.add(password, 1, row++);

        layout.add(new Label("LiveJasmin Session ID"), 0, row);
        TextField sessionId = new TextField();
        sessionId.setText(Config.getInstance().getSettings().livejasminSession);
        sessionId.textProperty().addListener((ob, o, n) -> {
            if(!n.equals(Config.getInstance().getSettings().livejasminSession)) {
                Config.getInstance().getSettings().livejasminSession = n;
                save();
            }
        });
        GridPane.setFillWidth(sessionId, true);
        GridPane.setHgrow(sessionId, Priority.ALWAYS);
        GridPane.setColumnSpan(sessionId, 2);
        layout.add(sessionId, 1, row++);

        Button createAccount = new Button("Create new Account");
        createAccount.setOnAction((e) -> DesktopIntegration.open(liveJasmin.getAffiliateLink()));
        layout.add(createAccount, 1, row++);
        GridPane.setColumnSpan(createAccount, 2);
        GridPane.setMargin(username, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        GridPane.setMargin(password, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        GridPane.setMargin(sessionId, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        GridPane.setMargin(createAccount, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));

        username.setPrefWidth(300);

        return layout;
    }
}
