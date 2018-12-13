package ctbrec.sites.streamate;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Model;
import ctbrec.io.HttpClient;
import ctbrec.sites.AbstractSite;

public class Streamate extends AbstractSite {

    private static final transient Logger LOG = LoggerFactory.getLogger(Streamate.class);

    public static final String BASE_URL = "https://www.streamate.com";

    private StreamateHttpClient httpClient;

    @Override
    public String getName() {
        return "Streamate";
    }

    @Override
    public String getBaseUrl() {
        return BASE_URL;
    }

    @Override
    public String getAffiliateLink() {
        return BASE_URL + "/landing/click/?AFNO=2-11330.2";
    }

    @Override
    public Model createModel(String name) {
        StreamateModel model = new StreamateModel();
        model.setName(name);
        model.setUrl(BASE_URL + "/cam/" + name);
        model.setDescription("");
        model.setSite(this);
        return model;
    }

    @Override
    public Integer getTokenBalance() throws IOException {
        //        int userId = ((StreamateHttpClient)getHttpClient()).getUserId();
        //        String url = Streamate.BASE_URL + "/tools/amf.php";
        //        RequestBody body = new FormBody.Builder()
        //                .add("method", "ping")
        //                .add("args[]", Integer.toString(userId))
        //                .build();
        //        Request request = new Request.Builder()
        //                .url(url)
        //                .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
        //                .addHeader("Accept", "application/json, text/javascript, */*")
        //                .addHeader("Accept-Language", "en")
        //                .addHeader("Referer", Streamate.BASE_URL)
        //                .addHeader("X-Requested-With", "XMLHttpRequest")
        //                .post(body)
        //                .build();
        //        try(Response response = getHttpClient().execute(request)) {
        //            if(response.isSuccessful()) {
        //                JSONObject json = new JSONObject(response.body().string());
        //                if(json.optString("status").equals("online")) {
        //                    JSONObject userData = json.getJSONObject("userData");
        //                    return userData.getInt("balance");
        //                } else {
        //                    throw new IOException("Request was not successful: " + json.toString(2));
        //                }
        //            } else {
        //                throw new HttpException(response.code(), response.message());
        //            }
        //        }
        return 0;
    }

    @Override
    public String getBuyTokensLink() {
        return getAffiliateLink();
    }

    @Override
    public synchronized boolean login() throws IOException {
        return credentialsAvailable() && getHttpClient().login();
    }

    @Override
    public HttpClient getHttpClient() {
        if(httpClient == null) {
            httpClient = new StreamateHttpClient();
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
    public boolean supportsSearch() {
        return false;
    }

    @Override
    public boolean searchRequiresLogin() {
        return true;
    }

    @Override
    public List<Model> search(String q) throws IOException, InterruptedException {
        //        String url = BASE_URL + "/tools/listing_v3.php?offset=0&model_search[display_name][text]=" + URLEncoder.encode(q, "utf-8");
        //        Request req = new Request.Builder()
        //                .url(url)
        //                .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
        //                .addHeader("Accept", "application/json, text/javascript, */*")
        //                .addHeader("Accept-Language", "en")
        //                .addHeader("Referer", Streamate.BASE_URL)
        //                .addHeader("X-Requested-With", "XMLHttpRequest")
        //                .build();
        //        try(Response response = getHttpClient().execute(req)) {
        //            if(response.isSuccessful()) {
        //                String body = response.body().string();
        //                JSONObject json = new JSONObject(body);
        //                if(json.optString("status").equals("success")) {
        //                    List<Model> models = new ArrayList<>();
        //                    JSONArray results = json.getJSONArray("models");
        //                    for (int i = 0; i < results.length(); i++) {
        //                        JSONObject result = results.getJSONObject(i);
        //                        Model model = createModel(result.getString("username"));
        //                        String thumb = result.getString("thumb_image");
        //                        if(thumb != null) {
        //                            model.setPreview("https:" + thumb);
        //                        }
        //                        if(result.has("display_name")) {
        //                            model.setDisplayName(result.getString("display_name"));
        //                        }
        //                        models.add(model);
        //                    }
        //                    return models;
        //                } else {
        //                    LOG.warn("Search result: " + json.toString(2));
        //                    return Collections.emptyList();
        //                }
        //            } else {
        //                throw new HttpException(response.code(), response.message());
        //            }
        //        }
        return Collections.emptyList();
    }

    @Override
    public boolean isSiteForModel(Model m) {
        return m instanceof StreamateModel;
    }

    @Override
    public boolean credentialsAvailable() {
        return false;
    }

    @Override
    public Model createModelFromUrl(String url) {
        Matcher m = Pattern.compile("https?://.*?streamate.com/cam/([^/]*?)/?").matcher(url);
        if(m.matches()) {
            String modelName = m.group(1);
            return createModel(modelName);
        } else {
            return super.createModelFromUrl(url);
        }
    }
}
