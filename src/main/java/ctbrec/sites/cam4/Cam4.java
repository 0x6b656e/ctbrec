package ctbrec.sites.cam4;

import java.io.IOException;

import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.io.HttpClient;
import ctbrec.recorder.Recorder;
import ctbrec.sites.AbstractSite;
import ctbrec.ui.DesktopIntergation;
import ctbrec.ui.SettingsTab;
import ctbrec.ui.TabProvider;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public class Cam4 extends AbstractSite {

    public static final String BASE_URI = "https://www.cam4.com";

    public static final String AFFILIATE_LINK = BASE_URI + "/?referrerId=1514a80d87b5effb456cca02f6743aa1";

    private HttpClient httpClient;
    private Recorder recorder;

    @Override
    public String getName() {
        return "Cam4";
    }

    @Override
    public String getBaseUrl() {
        return BASE_URI;
    }

    @Override
    public String getAffiliateLink() {
        return AFFILIATE_LINK;
    }

    @Override
    public void setRecorder(Recorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public TabProvider getTabProvider() {
        return new Cam4TabProvider(this, recorder);
    }

    @Override
    public Model createModel(String name) {
        Cam4Model m = new Cam4Model();
        m.setSite(this);
        m.setName(name);
        m.setUrl(getBaseUrl() + '/' + name + '/');
        return m;
    }

    @Override
    public Integer getTokenBalance() throws IOException {
        if (!credentialsAvailable()) {
            throw new IOException("Not logged in");
        }
        return ((Cam4HttpClient)getHttpClient()).getTokenBalance();
    }

    @Override
    public String getBuyTokensLink() {
        return getAffiliateLink();
    }

    @Override
    public void login() throws IOException {
        if (credentialsAvailable()) {
            boolean success = getHttpClient().login();
            LoggerFactory.getLogger(getClass()).debug("Login success: {}", success);
        }
    }

    @Override
    public HttpClient getHttpClient() {
        if(httpClient == null) {
            httpClient = new Cam4HttpClient();
        }
        return httpClient;
    }

    @Override
    public void shutdown() {
        getHttpClient().shutdown();
    }

    @Override
    public void init() throws IOException {
    }

    @Override
    public boolean supportsTips() {
        return false;
    }

    @Override
    public boolean supportsFollow() {
        return true;
    }

    @Override
    public boolean isSiteForModel(Model m) {
        return m instanceof Cam4Model;
    }

    @Override
    public boolean credentialsAvailable() {
        String username = Config.getInstance().getSettings().cam4Username;
        return username != null && !username.trim().isEmpty();
    }

    @Override
    public Node getConfigurationGui() {
        GridPane layout = SettingsTab.createGridLayout();
        layout.add(new Label("Cam4 User"), 0, 0);
        TextField username = new TextField(Config.getInstance().getSettings().cam4Username);
        username.focusedProperty().addListener((e) -> Config.getInstance().getSettings().cam4Username = username.getText());
        GridPane.setFillWidth(username, true);
        GridPane.setHgrow(username, Priority.ALWAYS);
        GridPane.setColumnSpan(username, 2);
        layout.add(username, 1, 0);

        layout.add(new Label("Cam4 Password"), 0, 1);
        PasswordField password = new PasswordField();
        password.setText(Config.getInstance().getSettings().cam4Password);
        password.focusedProperty().addListener((e) -> Config.getInstance().getSettings().cam4Password = password.getText());
        GridPane.setFillWidth(password, true);
        GridPane.setHgrow(password, Priority.ALWAYS);
        GridPane.setColumnSpan(password, 2);
        layout.add(password, 1, 1);

        Button createAccount = new Button("Create new Account");
        createAccount.setOnAction((e) -> DesktopIntergation.open(Cam4.AFFILIATE_LINK));
        layout.add(createAccount, 1, 2);
        GridPane.setColumnSpan(createAccount, 2);
        GridPane.setMargin(username, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        GridPane.setMargin(password, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        GridPane.setMargin(createAccount, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        return layout;
    }
}
