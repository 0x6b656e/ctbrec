package ctbrec.ui;

import ctbrec.sites.Site;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class SiteTabPane extends TabPane {

    public SiteTabPane(Site site, Scene scene) {
        setSide(Side.LEFT);

        // add all tabs
        for (Tab tab : site.getTabProvider().getTabs(scene)) {
            getTabs().add(tab);
        }

        // register changelistener to activate / deactivate tabs, when the user switches between them
        getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
            @Override
            public void changed(ObservableValue<? extends Tab> ov, Tab from, Tab to) {
                if (from != null && from instanceof TabSelectionListener) {
                    ((TabSelectionListener) from).deselected();
                }
                if (to != null && to instanceof TabSelectionListener) {
                    ((TabSelectionListener) to).selected();
                }
            }
        });
    }

    public void selected() {
        Tab selectedTab = getSelectionModel().getSelectedItem();
        if(selectedTab instanceof TabSelectionListener) {
            ((TabSelectionListener) selectedTab).selected();
        }
    }

    public void deselected() {
        Tab selectedTab = getSelectionModel().getSelectedItem();
        if(selectedTab instanceof TabSelectionListener) {
            ((TabSelectionListener) selectedTab).deselected();
        }
    }
}
