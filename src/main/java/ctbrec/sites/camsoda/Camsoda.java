package ctbrec.sites.camsoda;

import java.io.IOException;

import ctbrec.Model;
import ctbrec.io.HttpClient;
import ctbrec.recorder.Recorder;
import ctbrec.sites.AbstractSite;
import ctbrec.ui.TabProvider;
import javafx.scene.Node;

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
        return "";
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
        return 0;
    }

    @Override
    public String getBuyTokensLink() {
        return getBaseUrl();
    }

    @Override
    public void login() throws IOException {
        httpClient.login();
    }

    @Override
    public HttpClient getHttpClient() {
        return httpClient;
    }

    @Override
    public void init() throws IOException {
        httpClient = new HttpClient() {
            @Override
            public boolean login() throws IOException {
                return false;
            }

        };
    }

    @Override
    public void shutdown() {
        httpClient.shutdown();
    }

    @Override
    public boolean supportsTips() {
        return false;
    }

    @Override
    public boolean supportsFollow() {
        return false;
    }

    @Override
    public boolean isSiteForModel(Model m) {
        return m instanceof CamsodaModel;
    }

    @Override
    public Node getConfigurationGui() {
        return null;
    }

    @Override
    public boolean credentialsAvailable() {
        return false;
    }
}
