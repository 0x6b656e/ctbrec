package ctbrec.ui;
import static ctbrec.Settings.ProxyType.*;

import java.util.ArrayList;
import java.util.List;

import ctbrec.Config;
import ctbrec.Settings.ProxyType;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;

public class ProxySettingsPane extends TitledPane implements EventHandler<ActionEvent> {

    private ComboBox<ProxyType> proxyType;
    private TextField proxyHost = new TextField();
    private TextField proxyPort = new TextField();
    private TextField proxyUser = new TextField();
    private PasswordField proxyPassword = new PasswordField();
    private SettingsTab settingsTab;

    public ProxySettingsPane(SettingsTab settingsTab) {
        this.settingsTab = settingsTab;
        createGui();
        loadConfig();
    }

    private void createGui() {
        setText("Proxy");
        setCollapsible(false);
        GridPane layout = SettingsTab.createGridLayout();
        setContent(layout);

        Label l = new Label("Type");
        layout.add(l, 0, 0);
        List<ProxyType> proxyTypes = new ArrayList<>();
        proxyTypes.add(DIRECT);
        proxyTypes.add(HTTP);
        proxyTypes.add(SOCKS4);
        proxyTypes.add(SOCKS5);
        proxyType = new ComboBox<>(FXCollections.observableList(proxyTypes));
        proxyType.setOnAction(this);
        layout.add(proxyType, 1, 0);

        l = new Label("Host");
        layout.add(l, 0, 1);
        layout.add(proxyHost, 1, 1);

        l = new Label("Port");
        layout.add(l, 0, 2);
        layout.add(proxyPort, 1, 2);

        l = new Label("Username");
        layout.add(l, 0, 3);
        layout.add(proxyUser, 1, 3);

        l = new Label("Password");
        layout.add(l, 0, 4);
        layout.add(proxyPassword, 1, 4);
    }

    private void loadConfig() {
        proxyType.valueProperty().set(Config.getInstance().getSettings().proxyType);
        proxyHost.setText(Config.getInstance().getSettings().proxyHost);
        proxyPort.setText(Config.getInstance().getSettings().proxyPort);
        proxyUser.setText(Config.getInstance().getSettings().proxyUser);
        proxyPassword.setText(Config.getInstance().getSettings().proxyPassword);
        setComponentDisableState();
    }

    void saveConfig() {
        Config.getInstance().getSettings().proxyType = proxyType.getValue();
        Config.getInstance().getSettings().proxyHost = proxyHost.getText();
        Config.getInstance().getSettings().proxyPort = proxyPort.getText();
        Config.getInstance().getSettings().proxyUser = proxyUser.getText();
        Config.getInstance().getSettings().proxyPassword = proxyPassword.getText();
    }

    @Override
    public void handle(ActionEvent event) {
        setComponentDisableState();
        settingsTab.showRestartRequired();
    }

    private void setComponentDisableState() {
        if(proxyType.getValue() == DIRECT) {
            proxyHost.setDisable(true);
            proxyPort.setDisable(true);
            proxyUser.setDisable(true);
            proxyPassword.setDisable(true);
        } else {
            proxyHost.setDisable(false);
            proxyPort.setDisable(false);
            proxyUser.setDisable(proxyType.getValue() == SOCKS4);
            proxyPassword.setDisable(proxyType.getValue() == SOCKS4);
        }
    }
}
