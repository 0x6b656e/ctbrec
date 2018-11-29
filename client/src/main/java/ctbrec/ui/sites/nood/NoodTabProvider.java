package ctbrec.ui.sites.nood;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ctbrec.recorder.Recorder;
import ctbrec.sites.nood.Nood;
import ctbrec.ui.PaginatedScheduledService;
import ctbrec.ui.TabProvider;
import ctbrec.ui.ThumbOverviewTab;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.util.Duration;

public class NoodTabProvider extends TabProvider {
    private Recorder recorder;
    private Nood nood;

    public NoodTabProvider(Nood nood) {
        this.nood = nood;
        this.recorder = nood.getRecorder();
    }

    @Override
    public List<Tab> getTabs(Scene scene) {
        List<Tab> tabs = new ArrayList<>();

        PaginatedScheduledService updateService = new NoodUpdateService(nood, nood.getBaseUrl());
        ThumbOverviewTab online = new ThumbOverviewTab("Online", updateService, nood);
        online.setRecorder(recorder);
        updateService.setPeriod(new Duration(TimeUnit.SECONDS.toMillis(10)));
        tabs.add(online);
        return tabs;
    }

    @Override
    public Tab getFollowedTab() {
        return null;
    }
}
