package ctbrec.sites.cam4;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.StringUtil;
import ctbrec.io.HttpClient;
import ctbrec.io.HttpException;
import ctbrec.sites.AbstractSite;
import okhttp3.Request;
import okhttp3.Response;

public class Cam4 extends AbstractSite {

    public static final String BASE_URI = "https://www.cam4.com";
    public static final String AFFILIATE_LINK = BASE_URI + "/?referrerId=1514a80d87b5effb456cca02f6743aa1";

    private HttpClient httpClient;

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
    public boolean login() throws IOException {
        return credentialsAvailable() && getHttpClient().login();
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
    public boolean supportsSearch() {
        return true;
    }

    @Override
    public boolean searchRequiresLogin() {
        return true;
    }

    @Override
    public List<Model> search(String q) throws IOException, InterruptedException {
        List<Model> result = new ArrayList<>();
        search(q, false, result);
        search(q, true, result);
        return result;
    }

    private void search(String q, boolean offline, List<Model> models) throws IOException {
        String url = BASE_URI + "/usernameSearch?username=" + URLEncoder.encode(q, "utf-8");
        if(offline) {
            url += "&offline=true";
        }
        Request req = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                .build();
        try(Response response = getHttpClient().execute(req)) {
            if(response.isSuccessful()) {
                String body = response.body().string();
                JSONArray results = new JSONArray(body);
                for (int i = 0; i < results.length(); i++) {
                    JSONObject result = results.getJSONObject(i);
                    Model model = createModel(result.getString("username"));
                    String thumb = null;
                    if(result.has("thumbnailId")) {
                        thumb = "https://snapshots.xcdnpro.com/thumbnails/" + model.getName() + "?s=" + result.getString("thumbnailId");
                    } else {
                        thumb = result.getString("profileImageLink");
                    }
                    if(StringUtil.isNotBlank(thumb)) {
                        model.setPreview(thumb);
                    }
                    models.add(model);
                }
            } else {
                throw new HttpException(response.code(), response.message());
            }
        }
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
}
