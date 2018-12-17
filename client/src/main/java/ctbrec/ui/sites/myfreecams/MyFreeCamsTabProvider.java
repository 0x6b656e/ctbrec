package ctbrec.ui.sites.myfreecams;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ctbrec.recorder.Recorder;
import ctbrec.sites.mfc.MyFreeCams;
import ctbrec.ui.PaginatedScheduledService;
import ctbrec.ui.TabProvider;
import ctbrec.ui.ThumbOverviewTab;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.util.Duration;

public class MyFreeCamsTabProvider extends TabProvider {
    private Recorder recorder;
    private MyFreeCams myFreeCams;
    private MyFreeCamsFriendsTab friends;

    public MyFreeCamsTabProvider(MyFreeCams myFreeCams) {
        this.myFreeCams = myFreeCams;
        this.recorder = myFreeCams.getRecorder();
    }

    @Override
    public List<Tab> getTabs(Scene scene) {
        List<Tab> tabs = new ArrayList<>();

        PaginatedScheduledService updateService = new OnlineCamsUpdateService();
        ThumbOverviewTab online = new ThumbOverviewTab("Online", updateService, myFreeCams);
        online.setRecorder(recorder);
        updateService.setPeriod(new Duration(TimeUnit.SECONDS.toMillis(10)));
        tabs.add(online);

        friends = new MyFreeCamsFriendsTab(myFreeCams);
        friends.setRecorder(recorder);
        tabs.add(friends);

        updateService = new HDCamsUpdateService();
        ThumbOverviewTab hd = new ThumbOverviewTab("HD", updateService, myFreeCams);
        hd.setRecorder(recorder);
        updateService.setPeriod(new Duration(TimeUnit.SECONDS.toMillis(10)));
        tabs.add(hd);

        updateService = new PopularModelService();
        ThumbOverviewTab pop = new ThumbOverviewTab("Most Popular", updateService, myFreeCams);
        pop.setRecorder(recorder);
        updateService.setPeriod(new Duration(TimeUnit.SECONDS.toMillis(10)));
        tabs.add(pop);

        MyFreeCamsTableTab table = new MyFreeCamsTableTab(myFreeCams);
        tabs.add(table);

        return tabs;
    }

    @Override
    public Tab getFollowedTab() {
        return friends;
    }
}
