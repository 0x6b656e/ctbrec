package ctbrec.sites.mfc;

import java.io.IOException;

import org.jsoup.select.Elements;

import ctbrec.recorder.Recorder;
import ctbrec.sites.Site;
import ctbrec.ui.HtmlParser;
import ctbrec.ui.TabProvider;
import okhttp3.Request;
import okhttp3.Response;

public class MyFreeCams implements Site {

    public static final String BASE_URI = "https://www.myfreecams.com";

    private Recorder recorder;
    private MyFreeCamsClient client;
    private MyFreeCamsHttpClient httpClient = new MyFreeCamsHttpClient();

    public MyFreeCams() throws IOException {
        client = MyFreeCamsClient.getInstance();
        client.setSite(this);
        client.start();
    }

    @Override
    public void login() throws IOException {
        httpClient.login();
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
        return "";
    }

    @Override
    public void setRecorder(Recorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public TabProvider getTabProvider() {
        return new MyFreeCamsTabProvider(client, recorder, this);
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
        Response resp = httpClient.execute(req, true);
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
        return "https://www.myfreecams.com/php/purchase.php?request=tokens";
    }

    @Override
    public MyFreeCamsHttpClient getHttpClient() {
        return httpClient;
    }

    @Override
    public void shutdown() {
        httpClient.shutdown();
    }

    @Override
    public boolean supportsFollow() {
        return false;
    }

    @Override
    public boolean supportsTips() {
        return true;
    }
}
