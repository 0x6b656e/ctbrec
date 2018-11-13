package ctbrec.sites.mfc;

import java.io.IOException;

import org.jsoup.select.Elements;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.recorder.Recorder;
import ctbrec.sites.AbstractSite;
import ctbrec.sites.ConfigUI;
import ctbrec.ui.HtmlParser;
import ctbrec.ui.TabProvider;
import okhttp3.Request;
import okhttp3.Response;

public class MyFreeCams extends AbstractSite {

    public static final String BASE_URI = "https://www.myfreecams.com";

    private Recorder recorder;
    private MyFreeCamsClient client;
    private MyFreeCamsHttpClient httpClient;
    private MyFreeCamsTabProvider tabProvider;

    @Override
    public void init() throws IOException {
        client = MyFreeCamsClient.getInstance();
        client.setSite(this);
        client.start();
    }

    @Override
    public void login() throws IOException {
        getHttpClient().login();
    }

    @Override
    public String getName() {
        return "MyFreeCams";
    }

    @Override
    public String getBaseUrl() {
        return BASE_URI;
    }

    @Override
    public String getAffiliateLink() {
        return BASE_URI + "/?baf=8127165";
    }

    @Override
    public void setRecorder(Recorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public TabProvider getTabProvider() {
        if(tabProvider == null) {
            tabProvider = new MyFreeCamsTabProvider(client, recorder, this);
        }
        return tabProvider;
    }

    @Override
    public MyFreeCamsModel createModel(String name) {
        MyFreeCamsModel model = new MyFreeCamsModel(this);
        model.setName(name);
        model.setUrl("https://profiles.myfreecams.com/" + name);
        return model;
    }

    @Override
    public Integer getTokenBalance() throws IOException {
        Request req = new Request.Builder().url(BASE_URI + "/php/account.php?request=status").build();
        Response resp = getHttpClient().execute(req, true);
        if(resp.isSuccessful()) {
            String content = resp.body().string();
            Elements tags = HtmlParser.getTags(content, "div.content > p > b");
            String tokens = tags.get(2).text();
            return Integer.parseInt(tokens);
        } else {
            resp.close();
            throw new IOException(resp.code() + " " + resp.message());
        }
    }

    @Override
    public String getBuyTokensLink() {
        return BASE_URI + "/php/purchase.php?request=tokens";
    }

    @Override
    public MyFreeCamsHttpClient getHttpClient() {
        if(httpClient == null) {
            httpClient = new MyFreeCamsHttpClient();
        }
        return httpClient;
    }

    @Override
    public void shutdown() {
        httpClient.shutdown();
    }

    @Override
    public boolean supportsFollow() {
        return true;
    }

    @Override
    public boolean supportsTips() {
        return true;
    }

    @Override
    public boolean isSiteForModel(Model m) {
        return m instanceof MyFreeCamsModel;
    }

    public MyFreeCamsClient getClient() {
        return client;
    }

    @Override
    public ConfigUI getConfigurationGui() {
        return new MyFreeCamsConfigUI(this);
    }

    @Override
    public boolean credentialsAvailable() {
        String username = Config.getInstance().getSettings().mfcUsername;
        return username != null && !username.trim().isEmpty();
    }
}
