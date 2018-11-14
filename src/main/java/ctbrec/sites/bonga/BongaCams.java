package ctbrec.sites.bonga;

import java.io.IOException;

import org.json.JSONObject;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.io.HttpClient;
import ctbrec.io.HttpException;
import ctbrec.recorder.Recorder;
import ctbrec.sites.AbstractSite;
import ctbrec.sites.ConfigUI;
import ctbrec.ui.TabProvider;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BongaCams extends AbstractSite {

    public static final String BASE_URL = "https://bongacams.com";

    private BongaCamsHttpClient httpClient;
    private Recorder recorder;
    private BongaCamsTabProvider tabProvider;

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
    public void setRecorder(Recorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public TabProvider getTabProvider() {
        if(tabProvider == null) {
            tabProvider = new BongaCamsTabProvider(recorder, this);
        }
        return tabProvider;
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
        try(Response response = getHttpClient().execute(request, true)) {
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
    public void login() throws IOException {
        getHttpClient().login();
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
    public boolean isSiteForModel(Model m) {
        return m instanceof BongaCamsModel;
    }

    @Override
    public ConfigUI getConfigurationGui() {
        return new BongaCamsConfigUI(this);
    }

    @Override
    public boolean credentialsAvailable() {
        String username = Config.getInstance().getSettings().bongaUsername;
        return username != null && !username.trim().isEmpty();
    }

}
