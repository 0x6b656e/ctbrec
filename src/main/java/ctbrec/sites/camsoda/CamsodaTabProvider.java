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
    CamsodaFollowedTab followedTab;

    public CamsodaTabProvider(Camsoda camsoda, Recorder recorder) {
        this.camsoda = camsoda;
        this.recorder = recorder;
        followedTab = new CamsodaFollowedTab("Followed", camsoda);
    }

    @Override
    public List<Tab> getTabs(Scene scene) {
        List<Tab> tabs = new ArrayList<>();
        tabs.add(createTab("Online", BASE_URI + "/api/v1/browse/online"));
        followedTab.setRecorder(recorder);
        followedTab.setScene(scene);
        tabs.add(followedTab);
        tabs.add(new CamsodaShowsTab(camsoda, recorder));
        return tabs;
    }

    @Override
    public Tab getFollowedTab() {
        return followedTab;
    }

    private Tab createTab(String title, String url) {
        CamsodaUpdateService updateService = new CamsodaUpdateService(url, false, camsoda);
        ThumbOverviewTab tab = new ThumbOverviewTab(title, updateService, camsoda);
        tab.setRecorder(recorder);
        return tab;
    }

}
