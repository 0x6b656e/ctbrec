package ctbrec.ui;

import ctbrec.sites.Site;
import javafx.scene.Scene;
import javafx.scene.control.Tab;

public class SiteTab extends Tab implements TabSelectionListener {

    private SiteTabPane siteTabPane;

    public SiteTab(Site site, Scene scene) {
        super(site.getName());
        setClosable(false);
        siteTabPane = new SiteTabPane(site, scene);
        setContent(siteTabPane);
    }

    @Override
    public void selected() {
        siteTabPane.selected();
    }

    @Override
    public void deselected() {
        siteTabPane.deselected();
    }
}
