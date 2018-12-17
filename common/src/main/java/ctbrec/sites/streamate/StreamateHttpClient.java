package ctbrec.sites.streamate;

import java.io.IOException;
import java.util.Collections;
import java.util.NoSuchElementException;

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

    private Long userId;
    private String saKey = "";
    private String userNickname = "";

    public StreamateHttpClient() {
        super("streamate");

        // this cookie is needed for the search
        Cookie searchCookie = new Cookie.Builder()
                .domain("streamate.com")
                .name("Xld_rct")
                .value("1")
                .build();
        getCookieJar().saveFromResponse(HttpUrl.parse(Streamate.BASE_URL), Collections.singletonList(searchCookie));

        // try to load sakey from cookie
        try {
            Cookie cookie = getCookieJar().getCookie(HttpUrl.parse("https://www.streamate.com"), "sakey");
            saKey = cookie.value();
        } catch (NoSuchElementException e) {
            // ignore
        }
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

        loggedIn = loginWithoutCookies();
        return loggedIn;
    }

    private synchronized boolean loginWithoutCookies() throws IOException {
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
            if(response.isSuccessful()) {
                JSONObject json = new JSONObject(content);
                LOG.debug(json.toString(2));
                loggedIn = json.has("sakey");
                saKey = json.optString("sakey");
                JSONObject account = json.getJSONObject("account");
                userId = account.getLong("userid");
                userNickname = account.getString("nickname");
            } else {
                throw new IOException("Login failed: " + response.code() + " " + response.message());
            }
            response.close();
        }

        return loggedIn;
    }

    /**
     * Check, if the login worked by loading the favorites
     */
    public boolean checkLoginSuccess() {
        String url = Streamate.BASE_URL + "/api/search/v1/favorites?host=streamate.com&domain=streamate.com";
        url = url + "&page_number=1&results_per_page=48&sakey=" + saKey + "&userid=" + userId;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                .addHeader("Accept", "application/json, */*")
                .addHeader("Accept-Language", "en")
                .addHeader("Referer", Streamate.BASE_URL)
                .build();
        try(Response response = execute(request)) {
            if (response.isSuccessful()) {
                String content = response.body().string();
                JSONObject json = new JSONObject(content);
                if(json.optString("status").equals("SM_OK")) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } catch(Exception e) {
            return false;
        }
    }

    public String getSaKey() {
        return saKey;
    }

    public Long getUserId() throws IOException {
        if(userId == null) {
            loginWithoutCookies();
        }
        return userId;
    }

    public String getUserNickname() {
        return userNickname;
    }
}
