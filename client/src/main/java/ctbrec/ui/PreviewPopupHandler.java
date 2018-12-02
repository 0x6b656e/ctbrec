package ctbrec.ui;

import java.io.InterruptedIOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.io.HttpException;
import ctbrec.recorder.download.StreamSource;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Popup;

public class PreviewPopupHandler implements EventHandler<MouseEvent> {
    private static final transient Logger LOG = LoggerFactory.getLogger(PreviewPopupHandler.class);

    private static final int offset = 10;
    private long timeForPopupOpen = TimeUnit.SECONDS.toMillis(1);
    private long timeForPopupClose = 400;
    private Popup popup = new Popup();
    private Node parent;
    private ImageView preview = new ImageView();
    private MediaView videoPreview;
    private MediaPlayer videoPlayer;
    private Media video;
    private JavaFxModel model;
    private volatile long openCountdown = -1;
    private volatile long closeCountdown = -1;
    private volatile long lastModelChange = -1;
    private volatile boolean changeModel = false;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> future;
    private ProgressIndicator progressIndicator;
    private StackPane pane;

    public PreviewPopupHandler(Node parent) {
        this.parent = parent;

        videoPreview = new MediaView();
        videoPreview.setFitWidth(Config.getInstance().getSettings().thumbWidth);
        videoPreview.setFitHeight(videoPreview.getFitWidth() * 9 / 16);
        videoPreview.setPreserveRatio(true);
        StackPane.setMargin(videoPreview, new Insets(5));

        preview.setFitWidth(Config.getInstance().getSettings().thumbWidth);
        preview.setPreserveRatio(true);
        preview.setSmooth(true);
        preview.setStyle("-fx-background-radius: 10px, 10px, 10px, 10px;");
        preview.visibleProperty().bind(videoPreview.visibleProperty().not());
        StackPane.setMargin(preview, new Insets(5));

        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.prefWidthProperty().bind(videoPreview.fitWidthProperty());

        Region veil = new Region();
        veil.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8)");
        veil.visibleProperty().bind(progressIndicator.visibleProperty());
        StackPane.setMargin(veil, new Insets(5));

        pane = new StackPane();
        pane.getChildren().addAll(preview, videoPreview, veil, progressIndicator);
        pane.setStyle("-fx-background-color: -fx-outer-border, -fx-inner-border, -fx-base;"+
                "-fx-background-insets: 0 0 -1 0, 0, 1, 2;" +
                "-fx-background-radius: 10px, 10px, 10px, 10px;" +
                "-fx-padding: 1;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.8), 20, 0, 0, 0);");
        popup.getContent().add(pane);

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
                        future.cancel(true);
                        progressIndicator.setVisible(true);
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
        if(future != null && !future.isDone()) {
            future.cancel(true);
        }
        future = executor.submit(() -> {
            try {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(true);
                    popup.show(parent.getScene().getWindow());
                });
                List<StreamSource> sources = model.getStreamSources();
                Collections.sort(sources);
                StreamSource best = sources.get(0);
                checkInterrupt();
                LOG.debug("Preview url for {} is {}", model.getName(), best.getMediaPlaylistUrl());
                video = new Media(best.getMediaPlaylistUrl());
                if(videoPlayer != null) {
                    videoPlayer.dispose();
                }
                videoPlayer = new MediaPlayer(video);
                videoPlayer.setMute(true);
                checkInterrupt();
                videoPlayer.setOnReady(() -> {
                    if(!future.isCancelled()) {
                        Platform.runLater(() -> {
                            double aspect = (double)video.getWidth() / video.getHeight();
                            double w = Config.getInstance().getSettings().thumbWidth;
                            double h = w / aspect;
                            resize(w, h);
                            progressIndicator.setVisible(false);
                            videoPreview.setVisible(true);
                            videoPreview.setMediaPlayer(videoPlayer);
                            videoPlayer.play();
                        });
                    }
                });
                videoPlayer.setOnError(() -> onError(videoPlayer));
            } catch (IllegalStateException e) {
                if(e.getMessage().equals("Stream url unknown")) {
                    // fine hls url for mfc not known yet
                } else {
                    LOG.warn("Couldn't start preview video: {}", e.getMessage());
                }
                showTestImage();
            } catch (HttpException e) {
                if(e.getResponseCode() != 404) {
                    LOG.warn("Couldn't start preview video: {}", e.getMessage());
                }
                showTestImage();
            } catch (InterruptedException | InterruptedIOException e) {
                // future has been canceled, that's fine
            } catch (ExecutionException e) {
                if(e.getCause() instanceof InterruptedException || e.getCause() instanceof InterruptedIOException) {
                    // future has been canceled, that's fine
                } else {
                    LOG.warn("Couldn't start preview video: {}", e.getMessage());
                    showTestImage();
                }
            } catch (Exception e) {
                LOG.warn("Couldn't start preview video: {}", e.getMessage());
                showTestImage();
            }
        });
    }

    private void onError(MediaPlayer videoPlayer) {
        LOG.error("Error while starting preview stream", videoPlayer.getError());
        if(videoPlayer.getError().getCause() != null) {
            LOG.error("Error while starting preview stream root cause:", videoPlayer.getError().getCause());
        }
        videoPlayer.dispose();
        Platform.runLater(() -> {
            showTestImage();
        });
    }

    private void resize(double w, double h) {
        preview.setFitWidth(w);
        preview.setFitHeight(h);
        videoPreview.setFitWidth(w);
        videoPreview.setFitHeight(h);
        pane.setPrefSize(w, h);
        popup.setWidth(w);
        popup.setHeight(h);
    }

    private void checkInterrupt() throws InterruptedException {
        if(Thread.interrupted()) {
            throw new InterruptedException();
        }
    }

    private void showTestImage() {
        Platform.runLater(() -> {
            videoPreview.setVisible(false);
            Image img = new Image(getClass().getResource("/image_not_found.png").toString(), true);
            preview.setImage(img);
            double aspect = img.getWidth() / img.getHeight();
            double w = Config.getInstance().getSettings().thumbWidth;
            double h = w / aspect;
            resize(w, h);
            progressIndicator.setVisible(false);
        });
    }

    private void hidePopup() {
        if(future != null && !future.isDone()) {
            future.cancel(true);
        }
        Platform.runLater(() -> {
            popup.setX(-1000);
            popup.setY(-1000);
            popup.hide();
            if(videoPlayer != null) {
                videoPlayer.dispose();
            }
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
