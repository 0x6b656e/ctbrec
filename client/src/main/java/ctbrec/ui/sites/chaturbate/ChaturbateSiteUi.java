package ctbrec.ui.sites.chaturbate;

import java.io.IOException;

import ctbrec.sites.ConfigUI;
import ctbrec.sites.chaturbate.Chaturbate;
import ctbrec.ui.SiteUI;
import ctbrec.ui.TabProvider;

public class ChaturbateSiteUi implements SiteUI {

    private ChaturbateTabProvider tabProvider;
    private ChaturbateConfigUi configUi;
    private Chaturbate chaturbate;

    public ChaturbateSiteUi(Chaturbate chaturbate) {
        this.chaturbate = chaturbate;
        tabProvider = new ChaturbateTabProvider(chaturbate);
        configUi = new ChaturbateConfigUi(chaturbate);
    }

    @Override
    public TabProvider getTabProvider() {
        return tabProvider;
    }

    @Override
    public ConfigUI getConfigUI() {
        return configUi;
    }

    @Override
    public synchronized boolean login() throws IOException {
        return chaturbate.login();
    }

}
