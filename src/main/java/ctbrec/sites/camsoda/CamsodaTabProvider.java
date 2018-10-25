package ctbrec.sites.camsoda;

import static ctbrec.sites.camsoda.Camsoda.*;

import java.util.ArrayList;
import java.util.List;

import ctbrec.recorder.Recorder;
import ctbrec.ui.TabProvider;
import ctbrec.ui.ThumbOverviewTab;
import javafx.scene.Scene;
import javafx.scene.control.Tab;

public class CamsodaTabProvider extends TabProvider {

    private Camsoda camsoda;
    private Recorder recorder;

    public CamsodaTabProvider(Camsoda camsoda, Recorder recorder) {
        this.camsoda = camsoda;
        this.recorder = recorder;
    }

    @Override
    public List<Tab> getTabs(Scene scene) {
        List<Tab> tabs = new ArrayList<>();
        tabs.add(createTab("Featured", BASE_URI + "/api/v1/browse/online"));
        //        ChaturbateFollowedTab followedTab = new ChaturbateFollowedTab("Followed", BASE_URI + "/followed-cams/", chaturbate);
        //        followedTab.setRecorder(recorder);
        //        followedTab.setScene(scene);
        //        tabs.add(followedTab);
        return tabs;
    }

    private Tab createTab(String title, String url) {
        CamsodaUpdateService updateService = new CamsodaUpdateService(url, false, camsoda);
        ThumbOverviewTab tab = new ThumbOverviewTab(title, updateService, camsoda);
        tab.setRecorder(recorder);
        return tab;
    }

}
