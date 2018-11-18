package ctbrec.sites.chaturbate;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.io.HtmlParser;
import ctbrec.io.HttpClient;
import okhttp3.Cookie;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChaturbateHttpClient extends HttpClient {

    private static final transient Logger LOG = LoggerFactory.getLogger(ChaturbateHttpClient.class);
    protected  String token;

    public ChaturbateHttpClient() {
        super("chaturbate");
    }

    private void extractCsrfToken(Request request) {
        try {
            Cookie csrfToken = cookieJar.getCookie(request.url(), "csrftoken");
            token = csrfToken.value();
        } catch(NoSuchElementException e) {
            LOG.trace("CSRF token not found in cookies");
        }
    }

    public String getToken() throws IOException {
        if(token == null) {
            login();
        }
        return token;
    }

    @Override
    public boolean login() throws IOException {
        if(loggedIn) {
            return true;
        }

        if(checkLogin()) {
            loggedIn = true;
            LOG.debug("Logged in with cookies");
            return true;
        }

        try {
            Request login = new Request.Builder()
                    .url(Chaturbate.BASE_URI + "/auth/login/")
                    .build();
            Response response = client.newCall(login).execute();
            String content = response.body().string();
            token = HtmlParser.getTag(content, "input[name=csrfmiddlewaretoken]").attr("value");
            LOG.debug("csrf token is {}", token);

            RequestBody body = new FormBody.Builder()
                    .add("username", Config.getInstance().getSettings().username)
                    .add("password", Config.getInstance().getSettings().password)
                    .add("next", "")
                    .add("csrfmiddlewaretoken", token)
                    .build();
            login = new Request.Builder()
                    .url(Chaturbate.BASE_URI + "/auth/login/")
                    .header("Referer", Chaturbate.BASE_URI + "/auth/login/")
                    .post(body)
                    .build();

            response = client.newCall(login).execute();
            if(response.isSuccessful()) {
                content = response.body().string();
                if(content.contains("Login, Chaturbate login")) {
                    loggedIn = false;
                } else {
                    loggedIn = true;
                    extractCsrfToken(login);
                }
            } else {
                if(loginTries++ < 3) {
                    login();
                } else {
                    throw new IOException("Login failed: " + response.code() + " " + response.message());
                }
            }
            response.close();
        } finally {
            loginTries = 0;
        }
        return loggedIn;
    }

    private boolean checkLogin() throws IOException {
        String url = "https://chaturbate.com/p/" + Config.getInstance().getSettings().username + "/";
        Request req = new Request.Builder().url(url).build();
        Response resp = execute(req);
        if (resp.isSuccessful()) {
            String profilePage = resp.body().string();
            try {
                HtmlParser.getText(profilePage, "span.tokencount");
                return true;
            } catch(Exception e) {
                LOG.debug("Token tag not found. Login failed");
                return false;
            }
        } else {
            throw new IOException("HTTP response: " + resp.code() + " - " + resp.message());
        }
    }

    @Override
    public Response execute(Request req) throws IOException {
        Response resp = super.execute(req);
        extractCsrfToken(req);
        return resp;
    }
}
