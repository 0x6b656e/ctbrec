package ctbrec.ui.sites.chaturbate;

import java.util.ArrayList;
import java.util.List;

import ctbrec.recorder.Recorder;
import ctbrec.sites.chaturbate.Chaturbate;
import ctbrec.ui.TabProvider;
import ctbrec.ui.ThumbOverviewTab;
import javafx.scene.Scene;
import javafx.scene.control.Tab;

public class ChaturbateTabProvider extends TabProvider {

    private Chaturbate chaturbate;
    private Recorder recorder;
    private ChaturbateFollowedTab followedTab;

    public ChaturbateTabProvider(Chaturbate chaturbate) {
        this.chaturbate = chaturbate;
        this.recorder = chaturbate.getRecorder();
        this.followedTab = new ChaturbateFollowedTab("Followed", chaturbate.getBaseUrl() + "/followed-cams/", chaturbate);
    }

    @Override
    public List<Tab> getTabs(Scene scene) {
        List<Tab> tabs = new ArrayList<>();
        tabs.add(createTab("Featured", chaturbate.getBaseUrl() + "/"));
        tabs.add(createTab("Female", chaturbate.getBaseUrl() + "/female-cams/"));
        tabs.add(createTab("Male", chaturbate.getBaseUrl() + "/male-cams/"));
        tabs.add(createTab("Couples", chaturbate.getBaseUrl() + "/couple-cams/"));
        tabs.add(createTab("Trans", chaturbate.getBaseUrl() + "/trans-cams/"));
        followedTab.setScene(scene);
        followedTab.setRecorder(recorder);
        tabs.add(followedTab);
        return tabs;
    }

    @Override
    public Tab getFollowedTab() {
        return followedTab;
    }

    private Tab createTab(String title, String url) {
        ChaturbateUpdateService updateService = new ChaturbateUpdateService(url, false, chaturbate);
        ThumbOverviewTab tab = new ThumbOverviewTab(title, updateService, chaturbate);
        tab.setRecorder(recorder);
        return tab;
    }
}
