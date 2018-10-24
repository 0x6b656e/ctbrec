package ctbrec.sites.mfc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ctbrec.recorder.Recorder;
import ctbrec.ui.PaginatedScheduledService;
import ctbrec.ui.TabProvider;
import ctbrec.ui.ThumbOverviewTab;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.util.Duration;

public class MyFreeCamsTabProvider extends TabProvider {
    private Recorder recorder;
    private MyFreeCams myFreeCams;

    public MyFreeCamsTabProvider(MyFreeCamsClient client, Recorder recorder, MyFreeCams myFreeCams) {
        this.recorder = recorder;
        this.myFreeCams = myFreeCams;
    }

    @Override
    public List<Tab> getTabs(Scene scene) {
        List<Tab> tabs = new ArrayList<>();

        PaginatedScheduledService updateService = new OnlineCamsUpdateService();
        ThumbOverviewTab online = new ThumbOverviewTab("Online", updateService, myFreeCams);
        online.setRecorder(recorder);
        updateService.setPeriod(new Duration(TimeUnit.SECONDS.toMillis(10)));
        tabs.add(online);

        updateService = new FriendsUpdateService(myFreeCams);
        ThumbOverviewTab friends = new ThumbOverviewTab("Friends", updateService, myFreeCams);
        friends.setRecorder(recorder);
        updateService.setPeriod(new Duration(TimeUnit.SECONDS.toMillis(10)));
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


        return tabs;
    }
}
