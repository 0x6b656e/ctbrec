package ctbrec.sites.chaturbate;

import java.util.ArrayList;
import java.util.List;

import ctbrec.recorder.Recorder;
import ctbrec.ui.FollowedTab;
import ctbrec.ui.TabProvider;
import ctbrec.ui.ThumbOverviewTab;
import javafx.scene.Scene;
import javafx.scene.control.Tab;

public class ChaturbateTabProvider extends TabProvider {

    private Chaturbate chaturbate;
    private Recorder recorder;

    public ChaturbateTabProvider(Chaturbate chaturbate, Recorder recorder) {
        this.chaturbate = chaturbate;
        this.recorder = recorder;
    }

    @Override
    public List<Tab> getTabs(Scene scene) {
        final String BASE_URI = chaturbate.getBaseUrl();
        List<Tab> tabs = new ArrayList<>();
        tabs.add(createTab("Featured", BASE_URI + "/"));
        tabs.add(createTab("Female", BASE_URI + "/female-cams/"));
        tabs.add(createTab("Male", BASE_URI + "/male-cams/"));
        tabs.add(createTab("Couples", BASE_URI + "/couple-cams/"));
        tabs.add(createTab("Trans", BASE_URI + "/trans-cams/"));
        FollowedTab followedTab = new FollowedTab("Followed", BASE_URI + "/followed-cams/", chaturbate);
        followedTab.setRecorder(recorder);
        followedTab.setScene(scene);
        tabs.add(followedTab);
        return tabs;
    }

    private Tab createTab(String title, String url) {
        ChaturbateUpdateService updateService = new ChaturbateUpdateService(url, false, chaturbate);
        ThumbOverviewTab tab = new ThumbOverviewTab(title, updateService, chaturbate);
        tab.setRecorder(recorder);
        return tab;
    }
}
