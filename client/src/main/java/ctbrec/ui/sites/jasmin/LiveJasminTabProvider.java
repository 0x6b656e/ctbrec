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

    public LiveJasminTabProvider(LiveJasmin liveJasmin) {
        this.liveJasmin = liveJasmin;
    }

    @Override
    public List<Tab> getTabs(Scene scene) {
        List<Tab> tabs = new ArrayList<>();

        long ts = System.currentTimeMillis();
        LiveJasminUpdateService s = new LiveJasminUpdateService(liveJasmin, liveJasmin.getBaseUrl() + "/en/girls/?listPageOrderType=most_popular&_dc=" + ts);
        ThumbOverviewTab tab = new ThumbOverviewTab("Girls", s, liveJasmin);
        tab.setRecorder(liveJasmin.getRecorder());
        tabs.add(tab);
        return tabs;
    }

    @Override
    public Tab getFollowedTab() {
        return null;
    }
}
