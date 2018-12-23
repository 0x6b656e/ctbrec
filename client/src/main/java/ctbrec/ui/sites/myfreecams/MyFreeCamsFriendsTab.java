package ctbrec.ui.sites.myfreecams;
import static ctbrec.ui.sites.myfreecams.FriendsUpdateService.Mode.*;

import ctbrec.sites.mfc.MyFreeCams;
import ctbrec.ui.FollowedTab;
import ctbrec.ui.ThumbOverviewTab;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;

public class MyFreeCamsFriendsTab extends ThumbOverviewTab implements FollowedTab {
    public MyFreeCamsFriendsTab(MyFreeCams mfc) {
        super("Friends", new FriendsUpdateService(mfc), mfc);
    }

    @Override
    protected void createGui() {
        super.createGui();
        addOnlineOfflineSelector();
    }

    private void addOnlineOfflineSelector() {
        ToggleGroup group = new ToggleGroup();
        RadioButton online = new RadioButton("online");
        online.setToggleGroup(group);
        RadioButton offline = new RadioButton("offline");
        offline.setToggleGroup(group);
        pagination.getChildren().add(online);
        pagination.getChildren().add(offline);
        HBox.setMargin(online, new Insets(5,5,5,40));
        HBox.setMargin(offline, new Insets(5,5,5,5));
        online.setSelected(true);
        group.selectedToggleProperty().addListener((e) -> {
            if(online.isSelected()) {
                ((FriendsUpdateService)updateService).setMode(ONLINE);
            } else {
                ((FriendsUpdateService)updateService).setMode(OFFLINE);
            }
            queue.clear();
            updateService.restart();
        });
    }

    public void setScene(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if(this.isSelected()) {
                if(event.getCode() == KeyCode.DELETE) {
                    follow(selectedThumbCells, false);
                }
            }
        });
    }
}
