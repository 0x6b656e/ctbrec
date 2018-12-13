package ctbrec.ui.sites.streamate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.recorder.Recorder;
import ctbrec.sites.streamate.Streamate;
import ctbrec.ui.TabProvider;
import ctbrec.ui.ThumbOverviewTab;
import javafx.scene.Scene;
import javafx.scene.control.Tab;

public class StreamateTabProvider extends TabProvider {
    private static final transient Logger LOG = LoggerFactory.getLogger(StreamateTabProvider.class);
    private Streamate streamate;
    private Recorder recorder;

    public StreamateTabProvider(Streamate streamate) {
        this.streamate = streamate;
        this.recorder = streamate.getRecorder();
    }

    @Override
    public List<Tab> getTabs(Scene scene) {
        List<Tab> tabs = new ArrayList<>();
        try {
            tabs.add(createTab("Girls", "/ctbrec/ui/sites/streamate/girls.sml"));
        } catch (IOException e) {
            LOG.error("Couldn't create streamate tab", e);
        }
        return tabs;
    }

    @Override
    public Tab getFollowedTab() {
        return null;
    }

    private Tab createTab(String title, String queryFile) throws IOException {
        StreamateUpdateService updateService = new StreamateUpdateService(loadQuery(queryFile), streamate);
        ThumbOverviewTab tab = new ThumbOverviewTab(title, updateService, streamate);
        tab.setRecorder(recorder);
        return tab;
    }

    private String loadQuery(String file) throws IOException {
        InputStream is = getClass().getResourceAsStream(file);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] b = new byte[1024];
        int len = -1;
        while( (len = is.read(b)) >= 0) {
            bos.write(b, 0, len);
        }
        return new String(bos.toByteArray(), "utf-8");
    }
}
