package ctbrec.ui.sites.streamate;

import java.io.IOException;

import ctbrec.sites.ConfigUI;
import ctbrec.sites.streamate.Streamate;
import ctbrec.ui.SiteUI;
import ctbrec.ui.TabProvider;

public class StreamateSiteUi implements SiteUI {

    private StreamateTabProvider tabProvider;

    public StreamateSiteUi(Streamate streamate) {
        tabProvider = new StreamateTabProvider(streamate);
    }

    @Override
    public TabProvider getTabProvider() {
        return tabProvider;
    }

    @Override
    public ConfigUI getConfigUI() {
        return null;
    }

    @Override
    public boolean login() throws IOException {
        return false;
    }

}
