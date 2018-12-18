package ctbrec.sites.fc2live;

import java.io.IOException;

import ctbrec.Model;
import ctbrec.io.HttpClient;
import ctbrec.sites.AbstractSite;

public class Fc2Live extends AbstractSite {

    public static final String BASE_URL = "https://live.fc2.com";
    private Fc2HttpClient httpClient;

    @Override
    public String getName() {
        return "FC2Live";
    }

    @Override
    public String getBaseUrl() {
        return BASE_URL;
    }

    @Override
    public String getAffiliateLink() {
        return BASE_URL + "/?afid=98987181";
    }

    @Override
    public Model createModel(String name) {
        Fc2Model model = new Fc2Model();
        model.setSite(this);
        model.setName(name);
        return model;
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
    public boolean login() throws IOException {
        return false;
    }

    @Override
    public HttpClient getHttpClient() {
        if(httpClient == null) {
            httpClient = new Fc2HttpClient();
        }
        return httpClient;
    }

    @Override
    public void init() throws IOException {
    }

    @Override
    public void shutdown() {
        if(httpClient != null) {
            httpClient.shutdown();
        }
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
        return m instanceof Fc2Model;
    }

    @Override
    public boolean credentialsAvailable() {
        return false;
    }

}
