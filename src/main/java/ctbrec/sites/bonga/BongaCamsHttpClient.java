package ctbrec.sites.bonga;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.io.HttpClient;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BongaCamsHttpClient extends HttpClient {

    private static final transient Logger LOG = LoggerFactory.getLogger(BongaCamsHttpClient.class);

    @Override
    public boolean login() throws IOException {
        String url = BongaCams.BASE_URL + "/login";
        String dateTime = new SimpleDateFormat("d.MM.yyyy', 'HH:mm:ss").format(new Date());
        RequestBody body = new FormBody.Builder()
                .add("security_log_additional_info","{\"language\":\"en\",\"cookieEnabled\":true,\"javaEnabled\":false,\"flashVersion\":\"31.0.0\",\"dateTime\":\""+dateTime+"\",\"ips\":[\"192.168.0.1\"]}")
                .add("log_in[username]", Config.getInstance().getSettings().bongaUsername)
                .add("log_in[password]", Config.getInstance().getSettings().bongaPassword)
                .add("log_in[remember]", "1")
                .add("log_in[bfpt]", "")
                .add("header_form", "1")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("User-Agent", "Mozilla/5.0 (Android 9.0; Mobile; rv:61.0) Gecko/61.0 Firefox/61.0")
                .addHeader("Accept","application/json")
                .addHeader("Accept-Language", "en")
                .addHeader("Referer", BongaCams.BASE_URL)
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .build();
        try(Response response = execute(request)) {
            if(response.isSuccessful()) {
                JSONObject json = new JSONObject(response.body().string());
                if(json.optString("status").equals("success")) {
                    return true;
                } else {
                    LOG.debug("Login response: {}", json.toString(2));
                    throw new IOException("Login not successful");
                }
            } else {
                throw new IOException(response.code() + " " + response.message());
            }
        }
    }

}
