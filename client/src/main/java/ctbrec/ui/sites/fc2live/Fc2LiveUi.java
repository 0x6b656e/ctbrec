package ctbrec.ui.sites.fc2live;

import java.io.IOException;

import ctbrec.sites.ConfigUI;
import ctbrec.sites.fc2live.Fc2Live;
import ctbrec.ui.SiteUI;
import ctbrec.ui.TabProvider;

public class Fc2LiveUi implements SiteUI {

    private Fc2Live fc2live;
    private Fc2TabProvider tabProvider;

    public Fc2LiveUi(Fc2Live fc2live) {
        this.fc2live = fc2live;
        this.tabProvider = new Fc2TabProvider(fc2live);
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
