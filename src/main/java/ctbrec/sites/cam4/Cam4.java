package ctbrec.sites.cam4;

import java.io.IOException;

import ctbrec.Model;
import ctbrec.io.HttpClient;
import ctbrec.recorder.Recorder;
import ctbrec.sites.AbstractSite;
import ctbrec.ui.TabProvider;
import javafx.scene.Node;

public class Cam4 extends AbstractSite {

    public static final String BASE_URI = "https://www.cam4.com";

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
        return getBaseUrl() + "/?referrerId=1514a80d87b5effb456cca02f6743aa1";
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
        return 0;
    }

    @Override
    public String getBuyTokensLink() {
        return getAffiliateLink();
    }

    @Override
    public void login() throws IOException {
        getHttpClient().login();
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
        return false;
    }

    @Override
    public boolean isSiteForModel(Model m) {
        return m instanceof Cam4Model;
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
