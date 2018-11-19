package ctbrec.ui.sites.fc2live;

import java.util.ArrayList;
import java.util.List;

import ctbrec.sites.fc2live.Fc2Live;
import ctbrec.ui.TabProvider;
import ctbrec.ui.ThumbOverviewTab;
import javafx.scene.Scene;
import javafx.scene.control.Tab;

public class Fc2TabProvider extends TabProvider {

    private Fc2Live fc2live;

    public Fc2TabProvider(Fc2Live fc2live) {
        this.fc2live = fc2live;
    }

    @Override
    public List<Tab> getTabs(Scene scene) {
        List<Tab> tabs = new ArrayList<>();
        tabs.add(createTab("Online", Fc2Live.BASE_URL + "/adult/contents/allchannellist.php"));
        return tabs;
    }

    private Tab createTab(String title, String url) {
        Fc2UpdateService updateService = new Fc2UpdateService(url, fc2live);
        ThumbOverviewTab tab = new ThumbOverviewTab(title, updateService, fc2live);
        tab.setRecorder(fc2live.getRecorder());
        return tab;
    }

    @Override
    public Tab getFollowedTab() {
        return null;
    }
}
