package ctbrec.sites.mfc;

import java.io.IOException;

import ctbrec.recorder.Recorder;
import ctbrec.sites.Site;
import ctbrec.ui.TabProvider;

public class MyFreeCams implements Site {

    public static final String BASE_URI = "https://www.myfreecams.com";

    private Recorder recorder;
    private MyFreeCamsClient client;
    private MyFreeCamsHttpClient httpClient = new MyFreeCamsHttpClient();

    public MyFreeCams() throws IOException {
        client = MyFreeCamsClient.getInstance();
        client.setSite(this);
        client.start();

        login();
    }

    @Override
    public void login() throws IOException {

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
        throw new RuntimeException("Not implemented for MFC");
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
}
