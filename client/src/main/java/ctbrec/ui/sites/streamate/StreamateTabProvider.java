package ctbrec.ui.sites.streamate;

import java.io.IOException;
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
    private ThumbOverviewTab followedTab;

    public StreamateTabProvider(Streamate streamate) {
        this.streamate = streamate;
        this.recorder = streamate.getRecorder();
    }

    @Override
    public List<Tab> getTabs(Scene scene) {
        List<Tab> tabs = new ArrayList<>();
        try {
            tabs.add(createTab("Girls",         Streamate.BASE_URL + "/api/search/list?domain=streamate.com&index=availperf&filters=gender:f"));
            tabs.add(createTab("Guys",          Streamate.BASE_URL + "/api/search/list?domain=streamate.com&index=availperf&filters=gender:m"));
            tabs.add(createTab("Couples",       Streamate.BASE_URL + "/api/search/list?domain=streamate.com&index=availperf&filters=gender:mf"));
            tabs.add(createTab("Lesbian",       Streamate.BASE_URL + "/api/search/list?domain=streamate.com&index=availperf&filters=gender:ff"));
            tabs.add(createTab("Gay",           Streamate.BASE_URL + "/api/search/list?domain=streamate.com&index=availperf&filters=gender:mm"));
            tabs.add(createTab("Groups",        Streamate.BASE_URL + "/api/search/list?domain=streamate.com&index=availperf&filters=gender:g"));
            tabs.add(createTab("Trans female",  Streamate.BASE_URL + "/api/search/list?domain=streamate.com&index=availperf&filters=gender:tm2f"));
            tabs.add(createTab("Trans male",    Streamate.BASE_URL + "/api/search/list?domain=streamate.com&index=availperf&filters=gender:tf2m"));
            tabs.add(createTab("New",           Streamate.BASE_URL + "/api/search/list?domain=streamate.com&index=availperf&filters=new:true"));

            followedTab = new StreamateFollowedTab(streamate);
            followedTab.setRecorder(recorder);
            tabs.add(followedTab);
        } catch (IOException e) {
            LOG.error("Couldn't create streamate tab", e);
        }
        return tabs;
    }

    @Override
    public Tab getFollowedTab() {
        return followedTab;
    }

    private Tab createTab(String title, String url) throws IOException {
        StreamateUpdateService updateService = new StreamateUpdateService(streamate, url);
        ThumbOverviewTab tab = new ThumbOverviewTab(title, updateService, streamate);
        tab.setRecorder(recorder);
        return tab;
    }
}
