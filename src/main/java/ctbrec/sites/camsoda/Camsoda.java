package ctbrec.sites.camsoda;

import java.io.IOException;

import org.json.JSONObject;

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
import okhttp3.Request;
import okhttp3.Response;

public class Camsoda extends AbstractSite {

    public static final String BASE_URI = "https://www.camsoda.com";
    private Recorder recorder;
    private HttpClient httpClient;

    @Override
    public String getName() {
        return "CamSoda";
    }

    @Override
    public String getBaseUrl() {
        return BASE_URI;
    }

    @Override
    public String getAffiliateLink() {
        return BASE_URI;
    }

    @Override
    public void setRecorder(Recorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public TabProvider getTabProvider() {
        return new CamsodaTabProvider(this, recorder);
    }

    @Override
    public Model createModel(String name) {
        CamsodaModel model = new CamsodaModel();
        model.setName(name);
        model.setUrl(getBaseUrl() + "/" + name);
        model.setSite(this);
        return model;
    }

    @Override
    public Integer getTokenBalance() throws IOException {
        String username = Config.getInstance().getSettings().camsodaUsername;
        if (username == null || username.trim().isEmpty()) {
            throw new IOException("Not logged in");
        }

        String url = BASE_URI + "/api/v1/user/" + username;
        Request request = new Request.Builder().url(url).build();
        Response response = getHttpClient().execute(request, true);
        if(response.isSuccessful()) {
            JSONObject json = new JSONObject(response.body().string());
            if(json.has("user")) {
                JSONObject user = json.getJSONObject("user");
                if(user.has("tokens")) {
                    return user.getInt("tokens");
                }
            }
        } else {
            throw new IOException(response.code() + " " + response.message());
        }
        throw new RuntimeException("Tokens not found in response");
    }

    @Override
    public String getBuyTokensLink() {
        return getBaseUrl();
    }

    @Override
    public void login() throws IOException {
        if(credentialsAvailable()) {
            getHttpClient().login();
        }
    }

    @Override
    public HttpClient getHttpClient() {
        if(httpClient == null) {
            httpClient = new CamsodaHttpClient();
        }
        return httpClient;
    }

    @Override
    public void init() throws IOException {
    }

    @Override
    public void shutdown() {
        httpClient.shutdown();
    }

    @Override
    public boolean supportsTips() {
        return true;
    }

    @Override
    public boolean supportsFollow() {
        return true;
    }

    @Override
    public boolean isSiteForModel(Model m) {
        return m instanceof CamsodaModel;
    }

    @Override
    public boolean credentialsAvailable() {
        String username = Config.getInstance().getSettings().camsodaUsername;
        return username != null && !username.trim().isEmpty();
    }

    @Override
    public Node getConfigurationGui() {
        GridPane layout = SettingsTab.createGridLayout();
        layout.add(new Label("CamSoda User"), 0, 0);
        TextField username = new TextField(Config.getInstance().getSettings().camsodaUsername);
        username.focusedProperty().addListener((e) -> Config.getInstance().getSettings().camsodaUsername = username.getText());
        GridPane.setFillWidth(username, true);
        GridPane.setHgrow(username, Priority.ALWAYS);
        GridPane.setColumnSpan(username, 2);
        layout.add(username, 1, 0);

        layout.add(new Label("CamSoda Password"), 0, 1);
        PasswordField password = new PasswordField();
        password.setText(Config.getInstance().getSettings().camsodaPassword);
        password.focusedProperty().addListener((e) -> Config.getInstance().getSettings().camsodaPassword = password.getText());
        GridPane.setFillWidth(password, true);
        GridPane.setHgrow(password, Priority.ALWAYS);
        GridPane.setColumnSpan(password, 2);
        layout.add(password, 1, 1);

        Button createAccount = new Button("Create new Account");
        createAccount.setOnAction((e) -> DesktopIntergation.open(getAffiliateLink()));
        layout.add(createAccount, 1, 2);
        GridPane.setColumnSpan(createAccount, 2);
        GridPane.setMargin(username, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        GridPane.setMargin(password, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        GridPane.setMargin(createAccount, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        return layout;
    }
}
