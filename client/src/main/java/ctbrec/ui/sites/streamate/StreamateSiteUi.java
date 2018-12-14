package ctbrec.ui.sites.streamate;

import java.io.IOException;

import ctbrec.sites.ConfigUI;
import ctbrec.sites.streamate.Streamate;
import ctbrec.ui.SiteUI;
import ctbrec.ui.TabProvider;

public class StreamateSiteUi implements SiteUI {

    private StreamateTabProvider tabProvider;
    private StreamateConfigUI configUi;
    private Streamate streamate;

    public StreamateSiteUi(Streamate streamate) {
        this.streamate = streamate;
        tabProvider = new StreamateTabProvider(streamate);
        configUi = new StreamateConfigUI(streamate);
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
    public boolean login() throws IOException {
        return streamate.login();
    }

}
