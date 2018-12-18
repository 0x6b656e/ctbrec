package ctbrec.sites.fc2live;

import java.io.IOException;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.io.HttpClient;
import ctbrec.recorder.download.MergedHlsDownload;

public class Fc2MergedHlsDownload extends MergedHlsDownload {

    private Fc2WebSocketClient ws;

    public Fc2MergedHlsDownload(HttpClient client) {
        super(client);

    }

    @Override
    public void start(Model model, Config config) throws IOException {
        super.start(model, config);
    }

    @Override
    public void stop() {
        super.stop();
    }
}
