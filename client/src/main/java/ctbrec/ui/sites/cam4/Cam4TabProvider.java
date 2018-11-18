package ctbrec.ui.sites.cam4;

import java.util.ArrayList;
import java.util.List;

import ctbrec.recorder.Recorder;
import ctbrec.sites.cam4.Cam4;
import ctbrec.ui.TabProvider;
import ctbrec.ui.ThumbOverviewTab;
import javafx.scene.Scene;
import javafx.scene.control.Tab;

public class Cam4TabProvider extends TabProvider {

    private Cam4 cam4;
    private Recorder recorder;
    private Cam4FollowedTab followed;

    public Cam4TabProvider(Cam4 cam4) {
        this.cam4 = cam4;
        this.recorder = cam4.getRecorder();
    }

    @Override
    public List<Tab> getTabs(Scene scene) {
        List<Tab> tabs = new ArrayList<>();

        tabs.add(createTab("Female",    cam4.getBaseUrl() + "/directoryResults?online=true&gender=female&orderBy=MOST_VIEWERS"));
        tabs.add(createTab("Male",      cam4.getBaseUrl() + "/directoryResults?online=true&gender=male&orderBy=MOST_VIEWERS"));
        tabs.add(createTab("Couples",   cam4.getBaseUrl() + "/directoryResults?online=true&broadcastType=male_group&broadcastType=female_group&broadcastType=male_female_group&orderBy=MOST_VIEWERS"));
        tabs.add(createTab("HD",        cam4.getBaseUrl() + "/directoryResults?online=true&hd=true&orderBy=MOST_VIEWERS"));

        followed = new Cam4FollowedTab(cam4);
        followed.setRecorder(recorder);
        tabs.add(followed);

        return tabs;
    }

    @Override
    public Tab getFollowedTab() {
        return followed;
    }

    private Tab createTab(String name, String url) {
        Cam4UpdateService updateService = new Cam4UpdateService(url, false, cam4);
        ThumbOverviewTab tab = new ThumbOverviewTab(name, updateService, cam4);
        tab.setRecorder(recorder);
        return tab;
    }

}
