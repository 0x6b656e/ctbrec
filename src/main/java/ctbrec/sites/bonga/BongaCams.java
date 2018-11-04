package ctbrec.sites.bonga;

import java.io.IOException;

import ctbrec.Model;
import ctbrec.io.HttpClient;
import ctbrec.recorder.Recorder;
import ctbrec.sites.AbstractSite;
import ctbrec.ui.TabProvider;
import javafx.scene.Node;

public class BongaCams extends AbstractSite {

    public static final String BASE_URL = "https://bongacams.com";

    private BongaCamsHttpClient httpClient;

    private Recorder recorder;

    @Override
    public String getName() {
        return "BongaCams";
    }

    @Override
    public String getBaseUrl() {
        return BASE_URL;
    }

    @Override
    public String getAffiliateLink() {
        return BASE_URL;
    }

    @Override
    public void setRecorder(Recorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public TabProvider getTabProvider() {
        return new BongaCamsTabProvider(recorder, this);
    }

    @Override
    public Model createModel(String name) {
        BongaCamsModel model = new BongaCamsModel();
        model.setName(name);
        model.setUrl(BASE_URL + '/' + name);
        model.setDescription("");
        model.setSite(this);
        return model;
    }

    @Override
    public Integer getTokenBalance() throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getBuyTokensLink() {
        // TODO Auto-generated method stub
        return getBaseUrl();
    }

    @Override
    public void login() throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public HttpClient getHttpClient() {
        if(httpClient == null) {
            httpClient = new BongaCamsHttpClient();
        }
        return httpClient;
    }

    @Override
    public void init() throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void shutdown() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean supportsTips() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsFollow() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isSiteForModel(Model m) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Node getConfigurationGui() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean credentialsAvailable() {
        // TODO Auto-generated method stub
        return false;
    }

}
