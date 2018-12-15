package ctbrec.ui.controls;

import java.io.InterruptedIOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.io.HttpException;
import ctbrec.recorder.download.StreamSource;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

public class StreamPreview extends StackPane {
    private static final transient Logger LOG = LoggerFactory.getLogger(StreamPreview.class);

    private ImageView preview = new ImageView();
    private MediaView videoPreview;
    private MediaPlayer videoPlayer;
    private Media video;
    private ProgressIndicator progressIndicator;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> future;

    public StreamPreview() {
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

        getChildren().addAll(preview, videoPreview, veil, progressIndicator);
    }

    public void startStream(Model model) {
        if(future != null && !future.isDone()) {
            future.cancel(true);
        }
        future = executor.submit(() -> {
            try {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(true);
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
                            resizeToFitContent(w, h);
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

    private void resizeToFitContent(double w, double h) {
        setPrefSize(w,  h);
        preview.setFitWidth(w);
        preview.setFitHeight(h);
        videoPreview.setFitWidth(w);
        videoPreview.setFitHeight(h);
    }

    public void stop() {
        if(future != null && !future.isDone()) {
            future.cancel(true);
        }
        Platform.runLater(() -> {
            if(videoPlayer != null) {
                videoPlayer.dispose();
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

    private void showTestImage() {
        Platform.runLater(() -> {
            videoPreview.setVisible(false);
            Image img = new Image(getClass().getResource("/image_not_found.png").toString(), true);
            preview.setImage(img);
            double aspect = img.getWidth() / img.getHeight();
            double w = Config.getInstance().getSettings().thumbWidth;
            double h = w / aspect;
            resizeToFitContent(w, h);
            progressIndicator.setVisible(false);
        });
    }

    private void checkInterrupt() throws InterruptedException {
        if(Thread.interrupted()) {
            throw new InterruptedException();
        }
    }
}
