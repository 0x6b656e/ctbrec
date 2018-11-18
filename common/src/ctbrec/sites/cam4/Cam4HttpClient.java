package ctbrec.sites.cam4;

import java.io.IOException;
import java.util.Objects;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.io.HttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Cam4HttpClient extends HttpClient {

    private static final transient Logger LOG = LoggerFactory.getLogger(Cam4HttpClient.class);

    public Cam4HttpClient() {
        super("cam4");
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
     *  check, if the login worked by requesting unchecked mail
     * @throws IOException
     */
    public boolean checkLoginSuccess() throws IOException {
        String mailUrl = Cam4.BASE_URI + "/mail/unreadThreads";
        Request req = new Request.Builder()
                .url(mailUrl)
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .build();
        Response response = execute(req);
        if(response.isSuccessful() && response.body().contentLength() > 0) {
            JSONObject json = new JSONObject(response.body().string());
            return json.has("status") && Objects.equals("success", json.getString("status"));
        } else {
            response.close();
            return false;
        }
    }

    protected int getTokenBalance() throws IOException {
        if(!loggedIn) {
            login();
        }

        throw new RuntimeException("Not implemented, yet");
    }
}
