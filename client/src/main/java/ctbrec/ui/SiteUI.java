package ctbrec.ui;

import java.io.IOException;

import ctbrec.sites.ConfigUI;

public interface SiteUI {

    public TabProvider getTabProvider();
    public ConfigUI getConfigUI();
    public boolean login() throws IOException;
}
