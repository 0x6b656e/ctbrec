package ctbrec.ui.sites.camsoda;

import ctbrec.sites.camsoda.Camsoda;
import ctbrec.ui.FollowedTab;
import ctbrec.ui.ThumbOverviewTab;
import javafx.concurrent.WorkerStateEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;

public class CamsodaFollowedTab extends ThumbOverviewTab implements FollowedTab {
    private Label status;
    boolean showOnline = true;

    public CamsodaFollowedTab(String title, Camsoda camsoda) {
        super(title, new CamsodaFollowedUpdateService(camsoda), camsoda);
        status = new Label("Logging in...");
        grid.getChildren().add(status);
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
        HBox.setMargin(online, new Insets(5, 5, 5, 40));
        HBox.setMargin(offline, new Insets(5, 5, 5, 5));
        online.setSelected(true);
        group.selectedToggleProperty().addListener((e) -> {
            queue.clear();
            ((CamsodaFollowedUpdateService)updateService).showOnline(online.isSelected());
            updateService.restart();
        });
    }

    @Override
    protected void onSuccess() {
        grid.getChildren().remove(status);
        super.onSuccess();
    }

    @Override
    protected void onFail(WorkerStateEvent event) {
        String msg = "";
        if (event.getSource().getException() != null) {
            msg = ": " + event.getSource().getException().getMessage();
        }
        status.setText("Login failed" + msg);
        super.onFail(event);
    }

    @Override
    public void selected() {
        status.setText("Logging in...");
        super.selected();
    }

    public void setScene(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (this.isSelected()) {
                if (event.getCode() == KeyCode.DELETE) {
                    follow(selectedThumbCells, false);
                }
            }
        });
    }
}
