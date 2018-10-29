package ctbrec.sites.cam4;

import java.util.ArrayList;
import java.util.List;

import ctbrec.recorder.Recorder;
import ctbrec.ui.TabProvider;
import ctbrec.ui.ThumbOverviewTab;
import javafx.scene.Scene;
import javafx.scene.control.Tab;

public class Cam4TabProvider extends TabProvider {

    private Cam4 cam4;
    private Recorder recorder;

    public Cam4TabProvider(Cam4 cam4, Recorder recorder) {
        this.cam4 = cam4;
        this.recorder = recorder;
    }

    @Override
    public List<Tab> getTabs(Scene scene) {
        List<Tab> tabs = new ArrayList<>();

        tabs.add(createTab("Female", cam4.getBaseUrl() + "/directoryResults?online=true&gender=female&orderBy=MOST_VIEWERS"));
        tabs.add(createTab("HD", cam4.getBaseUrl() + "/directoryResults?online=true&hd=true&orderBy=VIDEO_QUALITY"));
        if(cam4.credentialsAvailable()) {
            Cam4FollowedTab followed = new Cam4FollowedTab(cam4);
            followed.setRecorder(recorder);
            tabs.add(followed);
        }

        return tabs;
    }

    private Tab createTab(String name, String url) {
        Cam4UpdateService updateService = new Cam4UpdateService(url, false, cam4);
        ThumbOverviewTab tab = new ThumbOverviewTab(name, updateService, cam4);
        tab.setRecorder(recorder);
        return tab;
    }

}
