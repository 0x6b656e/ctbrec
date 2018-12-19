package ctbrec.ui.sites.jasmin;

import java.io.IOException;

import ctbrec.sites.ConfigUI;
import ctbrec.sites.jasmin.LiveJasmin;
import ctbrec.ui.SiteUI;
import ctbrec.ui.TabProvider;

public class LiveJasminSiteUi implements SiteUI {

    private LiveJasmin liveJasmin;
    private LiveJasminTabProvider tabProvider;

    public LiveJasminSiteUi(LiveJasmin liveJasmin) {
        this.liveJasmin = liveJasmin;
        tabProvider = new LiveJasminTabProvider(liveJasmin);
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
        return liveJasmin.login();
    }

}
