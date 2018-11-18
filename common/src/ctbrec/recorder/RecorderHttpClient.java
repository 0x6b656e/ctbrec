package ctbrec.recorder;

import java.io.IOException;

import ctbrec.io.HttpClient;

public class RecorderHttpClient extends HttpClient {

    public RecorderHttpClient() {
        super("recorder");
    }

    @Override
    public boolean login() throws IOException {
        return false;
    }
}
