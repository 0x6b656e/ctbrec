package ctbrec.sites.mfc;

import java.io.IOException;

import org.jsoup.select.Elements;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.recorder.Recorder;
import ctbrec.sites.AbstractSite;
import ctbrec.ui.DesktopIntergation;
import ctbrec.ui.HtmlParser;
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

public class MyFreeCams extends AbstractSite {

    public static final String BASE_URI = "https://www.myfreecams.com";

    private Recorder recorder;
    private MyFreeCamsClient client;
    private MyFreeCamsHttpClient httpClient;

    @Override
    public void init() throws IOException {
        client = MyFreeCamsClient.getInstance();
        client.setSite(this);
        client.start();
    }

    @Override
    public void login() throws IOException {
        getHttpClient().login();
    }

    @Override
    public String getName() {
        return "MyFreeCams";
    }

    @Override
    public String getBaseUrl() {
        return BASE_URI;
    }

    @Override
    public String getAffiliateLink() {
        return "";
    }

    @Override
    public void setRecorder(Recorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public TabProvider getTabProvider() {
        return new MyFreeCamsTabProvider(client, recorder, this);
    }

    @Override
    public MyFreeCamsModel createModel(String name) {
        MyFreeCamsModel model = new MyFreeCamsModel(this);
        model.setName(name);
        model.setUrl("https://profiles.myfreecams.com/" + name);
        return model;
    }

    @Override
    public Integer getTokenBalance() throws IOException {
        Request req = new Request.Builder().url(BASE_URI + "/php/account.php?request=status").build();
        Response resp = getHttpClient().execute(req, true);
        if(resp.isSuccessful()) {
            String content = resp.body().string();
            Elements tags = HtmlParser.getTags(content, "div.content > p > b");
            String tokens = tags.get(2).text();
            return Integer.parseInt(tokens);
        } else {
            resp.close();
            throw new IOException(resp.code() + " " + resp.message());
        }
    }

    @Override
    public String getBuyTokensLink() {
        return "https://www.myfreecams.com/php/purchase.php?request=tokens";
    }

    @Override
    public MyFreeCamsHttpClient getHttpClient() {
        if(httpClient == null) {
            httpClient = new MyFreeCamsHttpClient();
        }
        return httpClient;
    }

    @Override
    public void shutdown() {
        httpClient.shutdown();
    }

    @Override
    public boolean supportsFollow() {
        return true;
    }

    @Override
    public boolean supportsTips() {
        return true;
    }

    @Override
    public boolean isSiteForModel(Model m) {
        return m instanceof MyFreeCamsModel;
    }

    public MyFreeCamsClient getClient() {
        return client;
    }

    @Override
    public Node getConfigurationGui() {
        GridPane layout = SettingsTab.createGridLayout();
        layout.add(new Label("MyFreeCams User"), 0, 0);
        TextField username = new TextField(Config.getInstance().getSettings().mfcUsername);
        username.focusedProperty().addListener((e) -> Config.getInstance().getSettings().mfcUsername = username.getText());
        GridPane.setFillWidth(username, true);
        GridPane.setHgrow(username, Priority.ALWAYS);
        GridPane.setColumnSpan(username, 2);
        layout.add(username, 1, 0);

        layout.add(new Label("MyFreeCams Password"), 0, 1);
        PasswordField password = new PasswordField();
        password.setText(Config.getInstance().getSettings().mfcPassword);
        password.focusedProperty().addListener((e) -> Config.getInstance().getSettings().mfcPassword = password.getText());
        GridPane.setFillWidth(password, true);
        GridPane.setHgrow(password, Priority.ALWAYS);
        GridPane.setColumnSpan(password, 2);
        layout.add(password, 1, 1);

        Button createAccount = new Button("Create new Account");
        createAccount.setOnAction((e) -> DesktopIntergation.open(BASE_URI + "/php/signup.php?request=register"));
        layout.add(createAccount, 1, 2);
        GridPane.setColumnSpan(createAccount, 2);
        GridPane.setMargin(username, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        GridPane.setMargin(password, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        GridPane.setMargin(createAccount, new Insets(0, 0, 0, SettingsTab.CHECKBOX_MARGIN));
        return layout;
    }

    @Override
    public boolean credentialsAvailable() {
        String username = Config.getInstance().getSettings().mfcUsername;
        return username != null && !username.trim().isEmpty();
    }
}
