package ctbrec.ui.sites.myfreecams;

import java.io.IOException;

import ctbrec.sites.ConfigUI;
import ctbrec.sites.mfc.MyFreeCams;
import ctbrec.ui.SiteUI;
import ctbrec.ui.TabProvider;

public class MyFreeCamsSiteUi implements SiteUI {

    private MyFreeCamsTabProvider tabProvider;
    private MyFreeCamsConfigUI configUi;
    private MyFreeCams myFreeCams;

    public MyFreeCamsSiteUi(MyFreeCams myFreeCams) {
        this.myFreeCams = myFreeCams;
        tabProvider = new MyFreeCamsTabProvider(myFreeCams);
        configUi = new MyFreeCamsConfigUI(myFreeCams);
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
        return myFreeCams.login();
    }

}
