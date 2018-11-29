package ctbrec.sites.bonga;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.io.HttpClient;
import ctbrec.io.HttpException;
import ctbrec.sites.AbstractSite;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BongaCams extends AbstractSite {

    private static final transient Logger LOG = LoggerFactory.getLogger(BongaCams.class);

    public static final String BASE_URL = "https://bongacams.com";

    private BongaCamsHttpClient httpClient;

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
        return "http://bongacams2.com/track?c=610249";
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
        int userId = ((BongaCamsHttpClient)getHttpClient()).getUserId();
        String url = BongaCams.BASE_URL + "/tools/amf.php";
        RequestBody body = new FormBody.Builder()
                .add("method", "ping")
                .add("args[]", Integer.toString(userId))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                .addHeader("Accept", "application/json, text/javascript, */*")
                .addHeader("Accept-Language", "en")
                .addHeader("Referer", BongaCams.BASE_URL)
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .post(body)
                .build();
        try(Response response = getHttpClient().execute(request)) {
            if(response.isSuccessful()) {
                JSONObject json = new JSONObject(response.body().string());
                if(json.optString("status").equals("online")) {
                    JSONObject userData = json.getJSONObject("userData");
                    return userData.getInt("balance");
                } else {
                    throw new IOException("Request was not successful: " + json.toString(2));
                }
            } else {
                throw new HttpException(response.code(), response.message());
            }
        }
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
            httpClient = new BongaCamsHttpClient();
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
        return false;
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
        String url = BASE_URL + "/tools/listing_v3.php?offset=0&model_search[display_name][text]=" + URLEncoder.encode(q, "utf-8");
        Request req = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                .addHeader("Accept", "application/json, text/javascript, */*")
                .addHeader("Accept-Language", "en")
                .addHeader("Referer", BongaCams.BASE_URL)
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .build();
        try(Response response = getHttpClient().execute(req)) {
            if(response.isSuccessful()) {
                String body = response.body().string();
                JSONObject json = new JSONObject(body);
                if(json.optString("status").equals("success")) {
                    List<Model> models = new ArrayList<>();
                    JSONArray results = json.getJSONArray("models");
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject result = results.getJSONObject(i);
                        Model model = createModel(result.getString("username"));
                        String thumb = result.getString("thumb_image");
                        if(thumb != null) {
                            model.setPreview("https:" + thumb);
                        }
                        models.add(model);
                    }
                    return models;
                } else {
                    LOG.warn("Search result: " + json.toString(2));
                    return Collections.emptyList();
                }
            } else {
                throw new HttpException(response.code(), response.message());
            }
        }
    }

    @Override
    public boolean isSiteForModel(Model m) {
        return m instanceof BongaCamsModel;
    }

    @Override
    public boolean credentialsAvailable() {
        String username = Config.getInstance().getSettings().bongaUsername;
        return username != null && !username.trim().isEmpty();
    }

    @Override
    public Model createModelFromUrl(String url) {
        Matcher m = Pattern.compile("https?://.*?bongacams.com(?:/profile)?/([^/]*?)/?").matcher(url);
        if(m.matches()) {
            String modelName = m.group(1);
            return createModel(modelName);
        } else {
            return super.createModelFromUrl(url);
        }
    }
}
