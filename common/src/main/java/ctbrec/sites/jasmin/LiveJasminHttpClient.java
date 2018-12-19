package ctbrec.sites.jasmin;

import java.io.IOException;

import ctbrec.io.HttpClient;

public class LiveJasminHttpClient extends HttpClient {

    protected LiveJasminHttpClient() {
        super("livejasmin");
    }

    @Override
    public boolean login() throws IOException {
        return false;
    }

}
