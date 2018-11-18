package ctbrec.sites.camsoda;

import java.io.IOException;
import java.util.Objects;

import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.io.HtmlParser;
import ctbrec.io.HttpClient;
import ctbrec.io.HttpException;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;

public class CamsodaHttpClient extends HttpClient {

    private static final transient Logger LOG = LoggerFactory.getLogger(CamsodaHttpClient.class);
    private String csrfToken = null;

    public CamsodaHttpClient() {
        super("camsoda");
    }

    @Override
    public boolean login() throws IOException {
        if(loggedIn) {
            return true;
        }

        // persisted cookies might let us log in
        if(checkLoginSuccess()) {
            loggedIn = true;
            LOG.debug("Logged in with cookies");
            return true;
        }

        String url = Camsoda.BASE_URI + "/api/v1/auth/login";
        FormBody body = new FormBody.Builder()
                .add("username", Config.getInstance().getSettings().camsodaUsername)
                .add("password", Config.getInstance().getSettings().camsodaPassword)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        try (Response response = execute(request)) {
            if (response.isSuccessful()) {
                JSONObject resp = new JSONObject(response.body().string());
                if (resp.has("error")) {
                    String error = resp.getString("error");
                    if (Objects.equals(error, "Please confirm that you are not a robot.")) {
                        // return loginWithDialog();
                        throw new IOException("CamSoda requested to solve a captcha. Please try again in a while (maybe 15 min).");
                    } else {
                        throw new IOException(resp.getString("error"));
                    }
                } else {
                    return true;
                }
            } else {
                throw new HttpException(response.code(), response.message());
            }
        }
    }

    /**
     *  check, if the login worked
     * @throws IOException
     */
    public boolean checkLoginSuccess() throws IOException {
        String url = Camsoda.BASE_URI + "/api/v1/user/current";
        Request request = new Request.Builder().url(url).build();
        try(Response response = execute(request)) {
            if(response.isSuccessful()) {
                JSONObject resp = new JSONObject(response.body().string());
                return resp.optBoolean("status");
            } else {
                return false;
            }
        }
    }

    protected String getCsrfToken() throws IOException {
        if(csrfToken == null) {
            String url = Camsoda.BASE_URI;
            Request request = new Request.Builder().url(url).build();
            try(Response response = execute(request)) {
                if(response.isSuccessful()) {
                    Element meta = HtmlParser.getTag(response.body().string(), "meta[name=\"_token\"]");
                    csrfToken = meta.attr("content");
                } else {
                    throw new HttpException(response.code(), response.message());
                }
            }
        }
        return csrfToken;
    }
}
