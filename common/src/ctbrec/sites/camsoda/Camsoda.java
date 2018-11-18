package ctbrec.sites.camsoda;

import java.io.IOException;

import org.json.JSONObject;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.io.HttpClient;
import ctbrec.io.HttpException;
import ctbrec.sites.AbstractSite;
import okhttp3.Request;
import okhttp3.Response;

public class Camsoda extends AbstractSite {

    public static final String BASE_URI = "https://www.camsoda.com";
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
    public String getBuyTokensLink() {
        return BASE_URI;
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
        if (!credentialsAvailable()) {
            throw new IOException("Account settings not available");
        }

        String username = Config.getInstance().getSettings().camsodaUsername;
        String url = BASE_URI + "/api/v1/user/" + username;
        Request request = new Request.Builder().url(url).build();
        try(Response response = getHttpClient().execute(request)) {
            if(response.isSuccessful()) {
                JSONObject json = new JSONObject(response.body().string());
                if(json.has("user")) {
                    JSONObject user = json.getJSONObject("user");
                    if(user.has("tokens")) {
                        return user.getInt("tokens");
                    }
                }
            } else {
                throw new HttpException(response.code(), response.message());
            }
        }
        throw new RuntimeException("Tokens not found in response");
    }

    @Override
    public boolean login() throws IOException {
        return credentialsAvailable() && getHttpClient().login();
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
        if(httpClient != null) {
            httpClient.shutdown();
        }
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
}
