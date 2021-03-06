package ctbrec.ui.sites.bonga;

import java.util.ArrayList;
import java.util.List;

import ctbrec.recorder.Recorder;
import ctbrec.sites.bonga.BongaCams;
import ctbrec.ui.PaginatedScheduledService;
import ctbrec.ui.TabProvider;
import ctbrec.ui.ThumbOverviewTab;
import javafx.scene.Scene;
import javafx.scene.control.Tab;

public class BongaCamsTabProvider extends TabProvider {

    private BongaCams bongaCams;
    private Recorder recorder;
    private Tab friendsTab;

    public BongaCamsTabProvider(BongaCams bongaCams) {
        this.bongaCams = bongaCams;
        this.recorder = bongaCams.getRecorder();
    }

    @Override
    public List<Tab> getTabs(Scene scene) {
        List<Tab> tabs = new ArrayList<>();

        // female
        String url = BongaCams.BASE_URL + "/tools/listing_v3.php?livetab=female&online_only=true&is_mobile=true&offset=";
        BongaCamsUpdateService updateService = new BongaCamsUpdateService(bongaCams, url);
        tabs.add(createTab("Female", updateService));

        // male
        url = BongaCams.BASE_URL + "/tools/listing_v3.php?livetab=male&online_only=true&is_mobile=true&offset=";
        updateService = new BongaCamsUpdateService(bongaCams, url);
        tabs.add(createTab("Male", updateService));

        // couples
        url = BongaCams.BASE_URL + "/tools/listing_v3.php?livetab=couples&online_only=true&is_mobile=true&offset=";
        updateService = new BongaCamsUpdateService(bongaCams, url);
        tabs.add(createTab("Couples", updateService));

        // trans
        url = BongaCams.BASE_URL + "/tools/listing_v3.php?livetab=transsexual&online_only=true&is_mobile=true&offset=";
        updateService = new BongaCamsUpdateService(bongaCams, url);
        tabs.add(createTab("Transsexual", updateService));

        // new
        url = BongaCams.BASE_URL + "/tools/listing_v3.php?livetab=new-models&online_only=true&is_mobile=true&offset=";
        updateService = new BongaCamsUpdateService(bongaCams, url);
        tabs.add(createTab("New", updateService));

        // friends
        url = BongaCams.BASE_URL + "/tools/listing_v3.php?livetab=friends&online_only=true&offset=";
        updateService = new BongaCamsUpdateService(bongaCams, url);
        friendsTab = createTab("Friends", updateService);
        tabs.add(friendsTab);

        return tabs;
    }

    @Override
    public Tab getFollowedTab() {
        return friendsTab;
    }

    private Tab createTab(String title, PaginatedScheduledService updateService) {
        ThumbOverviewTab tab = new ThumbOverviewTab(title, updateService, bongaCams);
        tab.setRecorder(recorder);
        return tab;
    }

}
