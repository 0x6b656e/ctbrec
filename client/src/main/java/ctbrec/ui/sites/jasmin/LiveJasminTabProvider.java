package ctbrec.ui.sites.jasmin;

import java.util.ArrayList;
import java.util.List;

import ctbrec.sites.jasmin.LiveJasmin;
import ctbrec.ui.TabProvider;
import ctbrec.ui.ThumbOverviewTab;
import javafx.scene.Scene;
import javafx.scene.control.Tab;

public class LiveJasminTabProvider extends TabProvider {

    private LiveJasmin liveJasmin;
    private LiveJasminFollowedTab followedTab;

    public LiveJasminTabProvider(LiveJasmin liveJasmin) {
        this.liveJasmin = liveJasmin;
    }

    @Override
    public List<Tab> getTabs(Scene scene) {
        List<Tab> tabs = new ArrayList<>();

        tabs.add(createTab("Girls", liveJasmin.getBaseUrl() + "/en/girls/?listPageOrderType=most_popular"));
        tabs.add(createTab("Girls HD", liveJasmin.getBaseUrl() + "/en/girls/hd/?listPageOrderType=most_popular"));
        tabs.add(createTab("Boys", liveJasmin.getBaseUrl() + "/en/boys/?listPageOrderType=most_popular"));
        tabs.add(createTab("Boys HD", liveJasmin.getBaseUrl() + "/en/boys/hd/?listPageOrderType=most_popular"));

        followedTab = new LiveJasminFollowedTab(liveJasmin);
        followedTab.setRecorder(liveJasmin.getRecorder());
        tabs.add(followedTab);

        return tabs;
    }

    @Override
    public Tab getFollowedTab() {
        return followedTab;
    }

    private ThumbOverviewTab createTab(String title, String url) {
        LiveJasminUpdateService s = new LiveJasminUpdateService(liveJasmin, url);
        ThumbOverviewTab tab = new ThumbOverviewTab(title, s, liveJasmin);
        tab.setRecorder(liveJasmin.getRecorder());
        return tab;
    }
}
