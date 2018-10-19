package ctbrec.sites.mfc;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.Site;
import ctbrec.recorder.Recorder;
import ctbrec.ui.CookieJarImpl;
import ctbrec.ui.TabProvider;
import okhttp3.ConnectionPool;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MyFreeCams implements Site {

    private static final transient Logger LOG = LoggerFactory.getLogger(MyFreeCams.class);

    private Recorder recorder;
    private MyFreeCamsClient client;
    public static OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(Config.getInstance().getSettings().httpTimeout, TimeUnit.MILLISECONDS)
            .readTimeout(Config.getInstance().getSettings().httpTimeout, TimeUnit.MILLISECONDS)
            .connectionPool(new ConnectionPool(50, 10, TimeUnit.MINUTES))
            .cookieJar(new CookieJarImpl())
            .build();

    public MyFreeCams() throws IOException {
        client = MyFreeCamsClient.getInstance();
        client.setSite(this);
        client.start();

        login();
    }

    public void login() throws IOException {
        RequestBody body = new FormBody.Builder()
                .add("username", "affenhubert")
                .add("password", "hampel81")
                .add("tz", "2")
                .add("ss", "1920x1080")
                .add("submit_login", "97")
                .build();
        Request req = new Request.Builder()
                .url(getBaseUrl() + "/php/login.php")
                .header("Referer", getBaseUrl())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(body)
                .build();
        Response resp = httpClient.newCall(req).execute();
        if(!resp.isSuccessful()) {
            LOG.error("Login failed {} {}", resp.code(), resp.message());
        }
        resp.close();
    }

    @Override
    public String getName() {
        return "MyFreeCams";
    }

    @Override
    public String getBaseUrl() {
        return "https://www.myfreecams.com";
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
        MyFreeCamsModel model = new MyFreeCamsModel();
        model.setName(name);
        model.setUrl("https://profiles.myfreecams.com/" + name);
        return model;
    }
}
