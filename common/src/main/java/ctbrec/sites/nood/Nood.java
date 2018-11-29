package ctbrec.sites.nood;

import java.io.IOException;

import ctbrec.Model;
import ctbrec.io.HttpClient;
import ctbrec.sites.AbstractSite;

public class Nood extends AbstractSite {

    public static final String BASE_URL = "https://www.nood.tv";
    private NoodHttpClient client;

    @Override
    public String getName() {
        return "Nood";
    }

    @Override
    public String getBaseUrl() {
        return BASE_URL;
    }

    @Override
    public String getAffiliateLink() {
        return getBaseUrl();
    }

    @Override
    public Model createModel(String name) {
        NoodModel model = new NoodModel();
        model.setName(name);
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
    public boolean login() throws IOException {
        return false;
    }

    @Override
    public HttpClient getHttpClient() {
        if(client == null) {
            client = new NoodHttpClient();
        }
        return client;
    }

    @Override
    public void init() throws IOException {
    }

    @Override
    public void shutdown() {
        if(client != null) {
            client.shutdown();
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
        return m instanceof NoodModel;
    }

    @Override
    public boolean credentialsAvailable() {
        return false;
    }

}
