package ctbrec.sites.bonga;

import java.util.ArrayList;
import java.util.List;

import ctbrec.recorder.Recorder;
import ctbrec.ui.PaginatedScheduledService;
import ctbrec.ui.TabProvider;
import ctbrec.ui.ThumbOverviewTab;
import javafx.scene.Scene;
import javafx.scene.control.Tab;

public class BongaCamsTabProvider extends TabProvider {

    private BongaCams bongaCams;
    private Recorder recorder;

    public BongaCamsTabProvider(Recorder recorder, BongaCams bongaCams) {
        this.recorder = recorder;
        this.bongaCams = bongaCams;
    }

    @Override
    public List<Tab> getTabs(Scene scene) {
        List<Tab> tabs = new ArrayList<>();

        BongaCamsUpdateService updateService = new BongaCamsUpdateService(bongaCams);
        tabs.add(createTab("Online", updateService));

        return tabs;
    }

    private Tab createTab(String title, PaginatedScheduledService updateService) {
        ThumbOverviewTab tab = new ThumbOverviewTab(title, updateService, bongaCams);
        tab.setRecorder(recorder);
        return tab;
    }

}
