package ctbrec.sites.camsoda;

import java.io.IOException;

import ctbrec.Model;
import ctbrec.io.HttpClient;
import ctbrec.recorder.Recorder;
import ctbrec.sites.Site;
import ctbrec.ui.TabProvider;
import javafx.scene.Node;

public class Camsoda implements Site {

    private Recorder recorder;
    private HttpClient httpClient;

    @Override
    public String getName() {
        return "CamSoda";
    }

    @Override
    public String getBaseUrl() {
        return "https://www.camsoda.com";
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
        return new CamsodaTabProvider();
    }

    @Override
    public Model createModel(String name) {
        CamsodaModel model = new CamsodaModel();
        model.setName(name);
        model.setUrl(getBaseUrl() + "/" + name);
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
