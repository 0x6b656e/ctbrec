package ctbrec.sites.cam4;

import java.io.IOException;

import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.io.HttpClient;
import ctbrec.recorder.Recorder;
import ctbrec.sites.AbstractSite;
import ctbrec.sites.ConfigUI;
import ctbrec.ui.TabProvider;

public class Cam4 extends AbstractSite {

    public static final String BASE_URI = "https://www.cam4.com";

    public static final String AFFILIATE_LINK = BASE_URI + "/?referrerId=1514a80d87b5effb456cca02f6743aa1";

    private HttpClient httpClient;
    private Recorder recorder;
    private Cam4TabProvider tabProvider;

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
        if(tabProvider == null) {
            tabProvider = new Cam4TabProvider(this, recorder);
        }
        return tabProvider;
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
    public ConfigUI getConfigurationGui() {
        return new Cam4ConfigUI();
    }
}
