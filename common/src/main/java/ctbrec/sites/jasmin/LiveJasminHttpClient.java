package ctbrec.sites.jasmin;

import java.io.IOException;
import java.util.Collections;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.io.HttpClient;
import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LiveJasminHttpClient extends HttpClient {

    private static final transient Logger LOG = LoggerFactory.getLogger(LiveJasminHttpClient.class);

    protected LiveJasminHttpClient() {
        super("livejasmin");
    }

    @Override
    public synchronized boolean login() throws IOException {
        if (loggedIn) {
            return true;
        }

        // set session cookie, if session id is available
        if(!Config.getInstance().getSettings().livejasminSession.isEmpty()) {
            Cookie captchaCookie = new Cookie.Builder()
                    .domain("livejasmin.com")
                    .name("session")
                    .value(Config.getInstance().getSettings().livejasminSession)
                    .build();
            getCookieJar().saveFromResponse(HttpUrl.parse("https://livejasmin.com"), Collections.singletonList(captchaCookie));
            getCookieJar().saveFromResponse(HttpUrl.parse("https://www.livejasmin.com"), Collections.singletonList(captchaCookie));
            getCookieJar().saveFromResponse(HttpUrl.parse("https://m.livejasmin.com"), Collections.singletonList(captchaCookie));
        }


        //        loadMainPage(); // to get initial cookies
        //        Cookie captchaCookie = new Cookie.Builder()
        //                .domain("livejasmin.com")
        //                .name("captchaRequired")
        //                .value("0")
        //                .build();
        //        getCookieJar().saveFromResponse(HttpUrl.parse("https://livejasmin.com"), Collections.singletonList(captchaCookie));
        //        getCookieJar().saveFromResponse(HttpUrl.parse("https://www.livejasmin.com"), Collections.singletonList(captchaCookie));
        //        getCookieJar().saveFromResponse(HttpUrl.parse("https://m.livejasmin.com"), Collections.singletonList(captchaCookie));
        //        Map<String, String> formParams = getLoginFormParameters();
        //        getCookieJar().saveFromResponse(HttpUrl.parse("https://livejasmin.com"), Collections.singletonList(captchaCookie));
        //        getCookieJar().saveFromResponse(HttpUrl.parse("https://m.livejasmin.com"), Collections.singletonList(captchaCookie));
        //        String action = formParams.get("action");
        //        formParams.remove("action");
        //        Builder formBuilder = new FormBody.Builder();
        //        for (Entry<String, String> param : formParams.entrySet()) {
        //            formBuilder.add(param.getKey(), param.getValue());
        //        }
        //        formBuilder.add("username", Config.getInstance().getSettings().livejasminUsername);
        //        formBuilder.add("password", Config.getInstance().getSettings().livejasminPassword);
        //        FormBody form = formBuilder.build();
        //        Buffer b = new Buffer();
        //        form.writeTo(b);
        //        LOG.debug("Form: {}", b.readUtf8());
        //        Map<String, List<Cookie>> cookies = getCookieJar().getCookies();
        //        for (Entry<String, List<Cookie>> domain : cookies.entrySet()) {
        //            LOG.debug("{}", domain.getKey());
        //            List<Cookie> cks = domain.getValue();
        //            for (Cookie cookie : cks) {
        //                LOG.debug("  {}", cookie);
        //            }
        //        }
        //        Request request = new Request.Builder()
        //                .url(LiveJasmin.BASE_URL + action)
        //                .header("User-Agent", USER_AGENT)
        //                .header("Accept", "*/*")
        //                .header("Accept-Language", "en")
        //                .header("Referer", LiveJasmin.BASE_URL + "/en/girls/")
        //                .header("X-Requested-With", "XMLHttpRequest")
        //                .post(form)
        //                .build();
        //        try(Response response = execute(request)) {
        //            System.out.println("login " + response.code() + " - " + response.message());
        //            System.out.println("login " + response.body().string());
        //        }

        boolean cookiesWorked = checkLoginSuccess();
        if (cookiesWorked) {
            loggedIn = true;
            LOG.debug("Logged in with cookies");
            return true;
        }

        return false;
    }

    //    private void loadMainPage() throws IOException {
    //        Request request = new Request.Builder()
    //                .url(LiveJasmin.BASE_URL)
    //                .header("User-Agent", USER_AGENT)
    //                .build();
    //        try(Response response = execute(request)) {
    //        }
    //    }
    //
    //    private Map<String, String> getLoginFormParameters() throws IOException {
    //        long ts = System.currentTimeMillis();
    //        String url = LiveJasmin.BASE_URL + "/en/auth/overlay/get-login-block?_dc="+ts;
    //        Request request = new Request.Builder()
    //                .url(url)
    //                .addHeader("User-Agent", USER_AGENT)
    //                .addHeader("Accept", "application/json, text/javascript, */*")
    //                .addHeader("Accept-Language", "en")
    //                .addHeader("Referer", LiveJasmin.BASE_URL)
    //                .addHeader("X-Requested-With", "XMLHttpRequest")
    //                .build();
    //        try(Response response = execute(request)) {
    //            if(response.isSuccessful()) {
    //                String body = response.body().string();
    //                JSONObject json = new JSONObject(body);
    //                if(json.optBoolean("success")) {
    //                    JSONObject data = json.getJSONObject("data");
    //                    String content = data.getString("content");
    //                    Map<String, String> params = new HashMap<>();
    //                    Element form = HtmlParser.getTag(content, "form");
    //                    params.put("action", form.attr("action"));
    //                    Elements hiddenInputs = HtmlParser.getTags(content, "input[type=hidden]");
    //                    for (Element input : hiddenInputs) {
    //                        String name = input.attr("name");
    //                        String value = input.attr("value");
    //                        params.put(name, value);
    //                    }
    //                    params.put("keepmeloggedin", "1");
    //                    params.put("captcha", "");
    //                    params.remove("captcha_needed");
    //                    return params;
    //                } else {
    //                    throw new IOException("Request was not successful: " + body);
    //                }
    //            } else {
    //                throw new HttpException(response.code(), response.message());
    //            }
    //        }
    //    }

    public boolean checkLoginSuccess() throws IOException {
        OkHttpClient temp = client.newBuilder()
                .followRedirects(false)
                .followSslRedirects(false)
                .build();
        String url = LiveJasmin.BASE_URL + "/en/free/favourite/get-favourite-list";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                .addHeader("Accept", "application/json, text/javascript, */*")
                .addHeader("Accept-Language", "en")
                .addHeader("Referer", LiveJasmin.BASE_URL)
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .build();
        try(Response response = temp.newCall(request).execute()) {
            LOG.debug("Login Check {}: {} - {}", url, response.code(), response.message());
            if(response.isSuccessful()) {
                return true;
            } else {
                return false;
            }
        }
    }

    public String getSessionId() {
        Cookie sessionCookie = getCookieJar().getCookie(HttpUrl.parse("https://www.livejasmin.com"), "session");
        if(sessionCookie != null) {
            return sessionCookie.value();
        } else {
            throw new NoSuchElementException("session cookie not found");
        }
    }
}
