package ctbrec.sites.camsoda;

import java.io.IOException;

import org.json.JSONObject;

import ctbrec.Config;
import ctbrec.io.HttpClient;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;

public class CamsodaHttpClient extends HttpClient {

    @Override
    public boolean login() throws IOException {
        String url = Camsoda.BASE_URI + "/api/v1/auth/login";
        FormBody body = new FormBody.Builder()
                .add("username", Config.getInstance().getSettings().camsodaUsername)
                .add("password", Config.getInstance().getSettings().camsodaPassword)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Response response = execute(request);
        if(response.isSuccessful()) {
            JSONObject resp = new JSONObject(response.body().string());
            if(resp.has("error")) {
                throw new IOException(resp.getString("error"));
            } else {
                return true;
            }
        } else {
            throw new IOException(response.code() + " " + response.message());
        }
    }
}
