package ctbrec.sites.streamate;

import java.io.IOException;
import java.util.Collections;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.io.HttpClient;
import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class StreamateHttpClient extends HttpClient {

    private static final transient Logger LOG = LoggerFactory.getLogger(StreamateHttpClient.class);

    private String userId = "";
    private String saKey = "";

    public StreamateHttpClient() {
        super("streamate");

        // this cookie is needed for the search
        Cookie searchCookie = new Cookie.Builder()
                .domain("streamate.com")
                .name("Xld_rct")
                .value("1")
                .build();
        getCookieJar().saveFromResponse(HttpUrl.parse(Streamate.BASE_URL), Collections.singletonList(searchCookie));
    }

    @Override
    public synchronized boolean login() throws IOException {
        if(loggedIn) {
            return true;
        }

        boolean cookiesWorked = checkLoginSuccess();
        if(cookiesWorked) {
            loggedIn = true;
            LOG.debug("Logged in with cookies");
            return true;
        }

        JSONObject loginRequest = new JSONObject();
        loginRequest.put("email", Config.getInstance().getSettings().streamateUsername);
        loginRequest.put("password", Config.getInstance().getSettings().streamatePassword);
        loginRequest.put("referrerId", 0);
        loginRequest.put("siteId", 1);
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), loginRequest.toString());
        Request login = new Request.Builder()
                .url(Streamate.BASE_URL + "/api/member/login")
                .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                .addHeader("Accept", "application/json, text/javascript, */*")
                .addHeader("Accept-Language", "en")
                .addHeader("Referer", Streamate.BASE_URL)
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .post(body)
                .build();
        try (Response response = client.newCall(login).execute()) {
            String content = response.body().string();
            //LOG.debug(content);
            if(response.isSuccessful()) {
                JSONObject json = new JSONObject(content);
                LOG.debug(json.toString());
                loggedIn = json.has("sakey");
                saKey = json.optString("sakey");
                JSONObject account = json.getJSONObject("account");
                userId = Long.toString(account.getLong("userid"));
            } else {
                throw new IOException("Login failed: " + response.code() + " " + response.message());
            }
            response.close();
        }

        return loggedIn;
    }

    /**
     * Check, if the login worked
     * @throws IOException
     */
    public boolean checkLoginSuccess() throws IOException {
        //https://www.streamate.com/api/search/v1/favorites?host=streamate.com&domain=streamate.com&page_number=1&results_per_page=48&sakey=62857cfd1908cd28
        return false;
        //        String modelName = getAnyModelName();
        //        // we request the roomData of a random model, because it contains
        //        // user data, if the user is logged in, which we can use to verify, that the login worked
        //        String url = Streamate.BASE_URL + "/tools/amf.php";
        //        RequestBody body = new FormBody.Builder()
        //                .add("method", "getRoomData")
        //                .add("args[]", modelName)
        //                .add("args[]", "false")
        //                //.add("method", "ping")   // TODO alternative request, but
        //                //.add("args[]", <userId>) // where to get the userId
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
        //        try(Response response = execute(request)) {
        //            if(response.isSuccessful()) {
        //                JSONObject json = new JSONObject(response.body().string());
        //                if(json.optString("status").equals("success")) {
        //                    JSONObject userData = json.getJSONObject("userData");
        //                    userId = userData.optInt("userId");
        //                    return userId > 0;
        //                } else {
        //                    throw new IOException("Request was not successful: " + json.toString(2));
        //                }
        //            } else {
        //                throw new HttpException(response.code(), response.message());
        //            }
        //        }
    }

    public String getSaKey() {
        return saKey;
    }

    public String getUserId() {
        return userId;
    }
}
