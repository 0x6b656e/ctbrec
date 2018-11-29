package ctbrec.sites.nood;

import java.io.IOException;

import ctbrec.Config;
import ctbrec.io.HttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NoodHttpClient extends HttpClient {

    protected NoodHttpClient() {
        super("nood");
    }

    @Override
    public boolean login() throws IOException {
        return false;
    }

    @Override
    public Response execute(Request req) throws IOException {
        Request clone = req.newBuilder()
                .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                .addHeader("Referer", Nood.BASE_URL)
                .addHeader("Origin", Nood.BASE_URL)
                .build();
        return super.execute(clone);
    }
}
