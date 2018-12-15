package ctbrec.ui;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.ui.controls.StreamPreview;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;

public class PreviewPopupHandler implements EventHandler<MouseEvent> {
    private static final transient Logger LOG = LoggerFactory.getLogger(PreviewPopupHandler.class);

    private static final int offset = 10;
    private long timeForPopupOpen = TimeUnit.SECONDS.toMillis(1);
    private long timeForPopupClose = 400;
    private Popup popup = new Popup();
    private Node parent;
    private StreamPreview streamPreview;
    private JavaFxModel model;
    private volatile long openCountdown = -1;
    private volatile long closeCountdown = -1;
    private volatile long lastModelChange = -1;
    private volatile boolean changeModel = false;

    public PreviewPopupHandler(Node parent) {
        this.parent = parent;

        streamPreview = new StreamPreview();
        streamPreview.setStyle("-fx-background-color: -fx-outer-border, -fx-inner-border, -fx-base;"+
                "-fx-background-insets: 0 0 -1 0, 0, 1, 2;" +
                "-fx-background-radius: 10px, 10px, 10px, 10px;" +
                "-fx-padding: 1;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.8), 20, 0, 0, 0);");
        popup.getContent().add(streamPreview);
        StackPane.setMargin(streamPreview, new Insets(5));

        createTimerThread();
    }

    @Override
    public void handle(MouseEvent event) {
        if(!isInPreviewColumn(event)) {
            closeCountdown = timeForPopupClose;
            return;
        }

        if(event.getEventType() == MouseEvent.MOUSE_CLICKED && event.getButton() == MouseButton.PRIMARY) {
            model = getModel(event);
            popup.setX(event.getScreenX()+ offset);
            popup.setY(event.getScreenY()+ offset);
            showPopup();
            openCountdown = -1;
        } else if(event.getEventType() == MouseEvent.MOUSE_ENTERED) {
            popup.setX(event.getScreenX()+ offset);
            popup.setY(event.getScreenY()+ offset);
            JavaFxModel model = getModel(event);
            if(model != null) {
                closeCountdown = -1;
                boolean modelChanged = model != this.model;
                this.model = model;
                if(popup.isShowing()) {
                    openCountdown = -1;
                    if(modelChanged) {
                        lastModelChange = System.currentTimeMillis();
                        changeModel = true;
                        streamPreview.stop();
                    }
                } else {
                    openCountdown = timeForPopupOpen;
                }
            }
        } else if(event.getEventType() == MouseEvent.MOUSE_EXITED) {
            openCountdown = -1;
            closeCountdown = timeForPopupClose;
            model = null;
        } else if(event.getEventType() == MouseEvent.MOUSE_MOVED) {
            popup.setX(event.getScreenX() + offset);
            popup.setY(event.getScreenY() + offset);
        }
    }

    private boolean isInPreviewColumn(MouseEvent event) {
        @SuppressWarnings("unchecked")
        TableRow<JavaFxModel> row = (TableRow<JavaFxModel>) event.getSource();
        TableView<JavaFxModel> table = row.getTableView();
        double offset = 0;
        double width = 0;
        for (TableColumn<JavaFxModel, ?> col : table.getColumns()) {
            offset += width;
            width = col.getWidth();
            if(Objects.equals(col.getId(), "preview")) {
                Point2D screenToLocal = table.screenToLocal(event.getScreenX(), event.getScreenY());
                double x = screenToLocal.getX();
                return x >= offset && x <= offset + width;
            }
        }
        return false;
    }

    private JavaFxModel getModel(MouseEvent event) {
        @SuppressWarnings("unchecked")
        TableRow<JavaFxModel> row = (TableRow<JavaFxModel>) event.getSource();
        TableView<JavaFxModel> table = row.getTableView();
        int rowIndex = row.getIndex();
        if(rowIndex < table.getItems().size()) {
            return table.getItems().get(rowIndex);
        } else {
            return null;
        }
    }

    private void showPopup() {
        startStream(model);
    }

    private void startStream(JavaFxModel model) {
        Platform.runLater(() -> {
            streamPreview.startStream(model);
            popup.show(parent.getScene().getWindow());
        });

    }

    private void hidePopup() {
        Platform.runLater(() -> {
            popup.setX(-1000);
            popup.setY(-1000);
            popup.hide();
            streamPreview.stop();
        });
    }

    private void createTimerThread() {
        Thread timerThread = new Thread(() -> {
            while(true) {
                openCountdown--;
                if(openCountdown == 0) {
                    openCountdown = -1;
                    if(model != null) {
                        showPopup();
                    }
                }

                closeCountdown--;
                if(closeCountdown == 0) {
                    hidePopup();
                    closeCountdown = -1;
                }

                openCountdown = Math.max(openCountdown, -1);
                closeCountdown = Math.max(closeCountdown, -1);

                long now = System.currentTimeMillis();
                long diff = (now - lastModelChange);
                if(changeModel && diff > 400) {
                    changeModel = false;
                    if(model != null) {
                        startStream(model);
                    }
                }

                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    LOG.error("PreviewPopupTimer interrupted");
                    break;
                }
            }
        });
        timerThread.setDaemon(true);
        timerThread.setPriority(Thread.MIN_PRIORITY);
        timerThread.setName("PreviewPopupTimer");
        timerThread.start();
    }
}
