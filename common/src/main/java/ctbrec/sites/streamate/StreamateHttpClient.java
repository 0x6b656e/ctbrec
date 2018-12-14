package ctbrec.sites.streamate;

import java.io.IOException;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.io.HttpClient;
import okhttp3.Cookie;
import okhttp3.HttpUrl;

public class StreamateHttpClient extends HttpClient {

    private static final transient Logger LOG = LoggerFactory.getLogger(StreamateHttpClient.class);

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

        return false;
    }

    /**
     * Check, if the login worked
     * @throws IOException
     */
    public boolean checkLoginSuccess() throws IOException {
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
}
