package ctbrec.ui.sites.nood;

import java.io.IOException;

import ctbrec.sites.ConfigUI;
import ctbrec.sites.nood.Nood;
import ctbrec.ui.SiteUI;
import ctbrec.ui.TabProvider;

public class NoodSiteUi implements SiteUI {

    private NoodTabProvider tabProvider;

    public NoodSiteUi(Nood nood) {
        tabProvider = new NoodTabProvider(nood);
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
