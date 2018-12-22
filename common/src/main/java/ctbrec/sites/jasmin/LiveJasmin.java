package ctbrec.sites.jasmin;

import java.io.IOException;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.io.HttpClient;
import ctbrec.sites.AbstractSite;

public class LiveJasmin extends AbstractSite {

    public static final String BASE_URL = "https://www.livejasmin.com";
    private HttpClient httpClient;

    @Override
    public String getName() {
        return "LiveJasmin";
    }

    @Override
    public String getBaseUrl() {
        return BASE_URL;
    }

    @Override
    public String getAffiliateLink() {
        return "https://awejmp.com/?siteId=jasmin&categoryName=girl&pageName=listpage&performerName=&prm[psid]=0xb00bface&prm[pstool]=205_1&prm[psprogram]=pps&prm[campaign_id]=&subAffId={SUBAFFID}&filters=";
    }

    @Override
    public Model createModel(String name) {
        LiveJasminModel model = new LiveJasminModel();
        model.setName(name);
        model.setDescription("");
        model.setSite(this);
        model.setUrl(getBaseUrl() + "/en/chat/" + name);
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
        return getHttpClient().login();
    }

    @Override
    public HttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = new LiveJasminHttpClient();
        }
        return httpClient;
    }

    @Override
    public void init() throws IOException {
    }

    @Override
    public void shutdown() {
        if (httpClient != null) {
            httpClient.shutdown();
        }
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
        return m instanceof LiveJasminModel;
    }

    @Override
    public boolean credentialsAvailable() {
        return !Config.getInstance().getSettings().livejasminSession.isEmpty();
    }

}
