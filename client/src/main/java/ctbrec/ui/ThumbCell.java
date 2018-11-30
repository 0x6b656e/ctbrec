package ctbrec.ui;

import java.io.EOFException;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iheartradio.m3u8.ParseException;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.io.HttpException;
import ctbrec.recorder.Recorder;
import ctbrec.ui.controls.Toast;
import javafx.animation.FadeTransition;
import javafx.animation.FillTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import okhttp3.Request;
import okhttp3.Response;

public class ThumbCell extends StackPane {

    private static final transient Logger LOG = LoggerFactory.getLogger(ThumbCell.class);
    private static final Duration ANIMATION_DURATION = new Duration(250);

    private Model model;
    private ImageView iv;
    private Rectangle resolutionBackground;
    private final Paint resolutionOnlineColor = new Color(0.22, 0.8, 0.29, 1);
    private final Color resolutionOfflineColor = new Color(0.8, 0.28, 0.28, 1);
    private Rectangle nameBackground;
    private Rectangle topicBackground;
    private Rectangle selectionOverlay;
    private Text name;
    private Text topic;
    private Text resolutionTag;
    private Recorder recorder;
    private Circle recordingIndicator;
    private PauseIndicator pausedIndicator;
    private int index = 0;
    ContextMenu popup;
    private final Color colorNormal = Color.BLACK;
    private final Color colorHighlight = Color.WHITE;
    private final Color colorRecording = new Color(0.8, 0.28, 0.28, .8);
    private SimpleBooleanProperty selectionProperty = new SimpleBooleanProperty(false);
    private double imgAspectRatio = 3.0 / 4.0;

    private ObservableList<Node> thumbCellList;
    private boolean mouseHovering = false;
    private boolean recording = false;
    private static ExecutorService imageLoadingThreadPool = Executors.newFixedThreadPool(30);

    public ThumbCell(ThumbOverviewTab parent, Model model, Recorder recorder) {
        this.thumbCellList = parent.grid.getChildren();
        this.model = model;
        this.recorder = recorder;
        recording = recorder.isRecording(model);
        model.setSuspended(recorder.isSuspended(model));
        this.setStyle("-fx-background-color: -fx-base");

        iv = new ImageView();
        iv.setSmooth(true);
        iv.setPreserveRatio(true);
        setImage(model.getPreview());
        getChildren().add(iv);

        nameBackground = new Rectangle();
        nameBackground.setFill(recording ? colorRecording : colorNormal);
        nameBackground.setOpacity(.7);
        StackPane.setAlignment(nameBackground, Pos.BOTTOM_CENTER);
        getChildren().add(nameBackground);

        topicBackground = new Rectangle();
        topicBackground.setFill(Color.BLACK);
        topicBackground.setOpacity(0);
        StackPane.setAlignment(topicBackground, Pos.TOP_LEFT);
        getChildren().add(topicBackground);

        resolutionBackground = new Rectangle(34, 16);
        resolutionBackground.setFill(resolutionOnlineColor );
        resolutionBackground.setVisible(false);
        resolutionBackground.setArcHeight(5);
        resolutionBackground.setArcWidth(resolutionBackground.getArcHeight());
        StackPane.setAlignment(resolutionBackground, Pos.TOP_RIGHT);
        StackPane.setMargin(resolutionBackground, new Insets(2));
        getChildren().add(resolutionBackground);

        name = new Text(model.getName());
        name.setFill(Color.WHITE);
        name.setFont(new Font("Sansserif", 16));
        name.setTextAlignment(TextAlignment.CENTER);
        name.prefHeight(25);
        StackPane.setAlignment(name, Pos.BOTTOM_CENTER);
        getChildren().add(name);

        topic = new Text();
        String txt = recording ? "    " : "";
        txt += model.getDescription();
        topic.setText(txt);

        topic.setFill(Color.WHITE);
        topic.setFont(new Font("Sansserif", 13));
        topic.setTextAlignment(TextAlignment.LEFT);
        topic.setOpacity(0);
        int margin = 4;
        StackPane.setMargin(topic, new Insets(margin));
        StackPane.setAlignment(topic, Pos.TOP_CENTER);
        getChildren().add(topic);

        resolutionTag = new Text();
        resolutionTag.setFill(Color.WHITE);
        resolutionTag.setVisible(false);
        StackPane.setAlignment(resolutionTag, Pos.TOP_RIGHT);
        StackPane.setMargin(resolutionTag, new Insets(2, 4, 2, 2));
        getChildren().add(resolutionTag);

        recordingIndicator = new Circle(8);
        recordingIndicator.setFill(colorRecording);
        StackPane.setMargin(recordingIndicator, new Insets(3));
        StackPane.setAlignment(recordingIndicator, Pos.TOP_LEFT);
        getChildren().add(recordingIndicator);

        pausedIndicator = new PauseIndicator(colorRecording, 16);
        pausedIndicator.setVisible(false);
        StackPane.setMargin(pausedIndicator, new Insets(3));
        StackPane.setAlignment(pausedIndicator, Pos.TOP_LEFT);
        getChildren().add(pausedIndicator);

        selectionOverlay = new Rectangle();
        selectionOverlay.setOpacity(0);
        StackPane.setAlignment(selectionOverlay, Pos.TOP_LEFT);
        getChildren().add(selectionOverlay);

        setOnMouseEntered((e) -> {
            mouseHovering = true;
            Color normal = recording ? colorRecording : colorNormal;
            new ParallelTransition(changeColor(nameBackground, normal, colorHighlight), changeColor(name, colorHighlight, normal)).playFromStart();
            new ParallelTransition(changeOpacity(topicBackground, 0.7), changeOpacity(topic, 0.7)).playFromStart();
            if(Config.getInstance().getSettings().determineResolution) {
                resolutionBackground.setVisible(false);
                resolutionTag.setVisible(false);
            }
        });
        setOnMouseExited((e) -> {
            mouseHovering = false;
            Color normal = recording ? colorRecording : colorNormal;
            new ParallelTransition(changeColor(nameBackground, colorHighlight, normal), changeColor(name, normal, colorHighlight)).playFromStart();
            new ParallelTransition(changeOpacity(topicBackground, 0), changeOpacity(topic, 0)).playFromStart();
            if(Config.getInstance().getSettings().determineResolution && !resolutionTag.getText().isEmpty()) {
                resolutionBackground.setVisible(true);
                resolutionTag.setVisible(true);
            }
        });
        setThumbWidth(Config.getInstance().getSettings().thumbWidth);

        setRecording(recording);
        if(Config.getInstance().getSettings().determineResolution) {
            determineResolution();
        }

        update();
    }

    public void setSelected(boolean selected) {
        selectionProperty.set(selected);
        selectionOverlay.getStyleClass().add("selection-background");
        selectionOverlay.setOpacity(selected ? .75 : 0);
    }

    public boolean isSelected() {
        return selectionProperty.get();
    }

    public ObservableValue<Boolean> selectionProperty() {
        return selectionProperty;
    }

    private void determineResolution() {
        if(ThumbOverviewTab.resolutionProcessing.contains(model)) {
            LOG.trace("Already fetching resolution for model {}. Queue size {}", model.getName(), ThumbOverviewTab.resolutionProcessing.size());
            return;
        }

        ThumbOverviewTab.threadPool.submit(() -> {
            try {
                ThumbOverviewTab.resolutionProcessing.add(model);
                int[] resolution = model.getStreamResolution(false);
                updateResolutionTag(resolution);

                // the model is online, but the resolution is 0. probably something went wrong
                // when we first requested the stream info, so we remove this invalid value from the "cache"
                // so that it is requested again
                if (model.isOnline() && resolution[1] == 0) {
                    LOG.trace("Removing invalid resolution value for {}", model.getName());
                    model.invalidateCacheEntries();
                }

                Thread.sleep(500);
            } catch (IOException | InterruptedException e1) {
                LOG.warn("Couldn't update resolution tag for model {}", model.getName(), e1);
            } catch(ExecutionException e) {
                if(e.getCause() instanceof EOFException) {
                    LOG.warn("Couldn't update resolution tag for model {}. Playlist empty", model.getName());
                } else if(e.getCause() instanceof ParseException) {
                    LOG.warn("Couldn't update resolution tag for model {} - {}", model.getName(), e.getMessage());
                } else {
                    LOG.warn("Couldn't update resolution tag for model {}", model.getName(), e);
                }
            } finally {
                ThumbOverviewTab.resolutionProcessing.remove(model);
            }
        });
    }

    private void updateResolutionTag(int[] resolution) throws IOException, ExecutionException, InterruptedException {
        String _res = "n/a";
        Paint resolutionBackgroundColor = resolutionOnlineColor;
        String state = model.getOnlineState(false);
        if (model.isOnline()) {
            LOG.trace("Model resolution {} {}x{}", model.getName(), resolution[0], resolution[1]);
            LOG.trace("Resolution queue size: {}", ThumbOverviewTab.queue.size());
            final int w = resolution[1];
            _res = w > 0 ? w != Integer.MAX_VALUE ? Integer.toString(w) : "HD" : state;
        } else {
            _res = model.getOnlineState(false);
            resolutionBackgroundColor = resolutionOfflineColor;
        }
        final String resText = _res;
        final Paint c = resolutionBackgroundColor;
        Platform.runLater(() -> {
            resolutionTag.setText(resText);
            if(!mouseHovering) {
                resolutionTag.setVisible(true);
                resolutionBackground.setVisible(true);
            }
            resolutionBackground.setWidth(resolutionTag.getBoundsInLocal().getWidth() + 4);
            resolutionBackground.setFill(c);
        });
    }

    private void setImage(String url) {
        if(!Objects.equals(System.getenv("CTBREC_DEV"), "1")) {
            boolean updateThumbs = Config.getInstance().getSettings().updateThumbnails;
            if(updateThumbs || iv.getImage() == null) {
                imageLoadingThreadPool.submit(() -> {
                    Request req = new Request.Builder()
                            .url(url)
                            .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                            .build();
                    try(Response resp = CamrecApplication.httpClient.execute(req)) {
                        if(resp.isSuccessful()) {
                            Image img = new Image(resp.body().byteStream());
                            if(img.progressProperty().get() == 1.0) {
                                Platform.runLater(() -> {
                                    iv.setImage(img);
                                    setThumbWidth(Config.getInstance().getSettings().thumbWidth);
                                });
                            } else {
                                img.progressProperty().addListener(new ChangeListener<Number>() {
                                    @Override
                                    public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                                        if(newValue.doubleValue() == 1.0) {
                                            iv.setImage(img);
                                            setThumbWidth(Config.getInstance().getSettings().thumbWidth);
                                        }
                                    }
                                });
                            }
                        } else {
                            throw new HttpException(resp.code(), resp.message());
                        }
                    } catch (IOException e) {
                        LOG.error("Error loading image", e);
                    }
                });
            }
        }
    }

    Image getImage() {
        return iv.getImage();
    }

    private Transition changeColor(Shape shape, Color from, Color to) {
        FillTransition transition = new FillTransition(ANIMATION_DURATION, from, to);
        transition.setShape(shape);
        return transition;
    }

    private Transition changeOpacity(Shape shape, double opacity) {
        FadeTransition transition = new FadeTransition(ANIMATION_DURATION, shape);
        transition.setFromValue(shape.getOpacity());
        transition.setToValue(opacity);
        return transition;
    }

    void startPlayer() {
        setCursor(Cursor.WAIT);
        new Thread(() -> {
            boolean started = Player.play(model);
            Platform.runLater(() -> {
                setCursor(Cursor.DEFAULT);
                if (started && Config.getInstance().getSettings().showPlayerStarting) {
                    Toast.makeText(getScene(), "Starting Player", 2000, 500, 500);
                }
            });
        }).start();
    }

    private void setRecording(boolean recording) {
        this.recording = recording;
        if(recording) {
            Color c = mouseHovering ? colorHighlight : colorRecording;
            nameBackground.setFill(c);
        } else {
            Color c = mouseHovering ? colorHighlight : colorNormal;
            nameBackground.setFill(c);
        }

        if(recording) {
            recordingIndicator.setVisible(!model.isSuspended());
            pausedIndicator.setVisible(model.isSuspended());
        } else {
            recordingIndicator.setVisible(false);
            pausedIndicator.setVisible(false);
        }
    }

    void startStopAction(boolean start) {
        setCursor(Cursor.WAIT);

        boolean selectSource = Config.getInstance().getSettings().chooseStreamQuality;
        if(selectSource && start) {
            Function<Model, Void> onSuccess = (model) -> {
                _startStopAction(model, start);
                return null;
            };
            Function<Throwable, Void> onFail = (throwable) -> {
                Alert alert = new AutosizeAlert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Couldn't start/stop recording");
                alert.setContentText("I/O error while starting/stopping the recording: " + throwable.getLocalizedMessage());
                alert.showAndWait();
                return null;
            };
            StreamSourceSelectionDialog.show(model, onSuccess, onFail);
        } else {
            _startStopAction(model, start);
        }
    }

    void pauseResumeAction(boolean pause) {
        setCursor(Cursor.WAIT);
        new Thread(() -> {
            try {
                if(pause) {
                    recorder.suspendRecording(model);
                } else {
                    recorder.resumeRecording(model);
                }
                setRecording(recording);
            } catch (Exception e1) {
                LOG.error("Couldn't pause/resume recording", e1);
                Platform.runLater(() -> {
                    Alert alert = new AutosizeAlert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Couldn't pause/resume recording");
                    alert.setContentText("I/O error while pausing/resuming the recording: " + e1.getLocalizedMessage());
                    alert.showAndWait();
                });
            } finally {
                setCursor(Cursor.DEFAULT);
            }
        }).start();
    }

    private void _startStopAction(Model model, boolean start) {
        new Thread(() -> {
            try {
                if(start) {
                    recorder.startRecording(model);
                    setRecording(true);
                } else {
                    recorder.stopRecording(model);
                    setRecording(false);
                }
            } catch (Exception e1) {
                LOG.error("Couldn't start/stop recording", e1);
                Platform.runLater(() -> {
                    Alert alert = new AutosizeAlert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Couldn't start/stop recording");
                    alert.setContentText("I/O error while starting/stopping the recording: " + e1.getLocalizedMessage());
                    alert.showAndWait();
                });
            } finally {
                setCursor(Cursor.DEFAULT);
            }
        }).start();
    }

    CompletableFuture<Boolean> follow(boolean follow) {
        setCursor(Cursor.WAIT);
        return CompletableFuture.supplyAsync(() -> {
            try {
                if(follow) {
                    SiteUiFactory.getUi(model.getSite()).login();
                    boolean followed = model.follow();
                    if(followed) {
                        return true;
                    } else {
                        Platform.runLater(() -> {
                            Alert alert = new AutosizeAlert(Alert.AlertType.ERROR);
                            alert.setTitle("Error");
                            alert.setHeaderText("Couldn't follow model");
                            alert.setContentText("");
                            alert.showAndWait();
                        });
                        return false;
                    }
                } else {
                    SiteUiFactory.getUi(model.getSite()).login();
                    boolean unfollowed = model.unfollow();
                    if(unfollowed) {
                        Platform.runLater(() -> thumbCellList.remove(ThumbCell.this));
                        return true;
                    } else {
                        Platform.runLater(() -> {
                            Alert alert = new AutosizeAlert(Alert.AlertType.ERROR);
                            alert.setTitle("Error");
                            alert.setHeaderText("Couldn't unfollow model");
                            alert.setContentText("");
                            alert.showAndWait();
                        });
                        return false;
                    }
                }
            } catch (Exception e1) {
                LOG.error("Couldn't follow/unfollow model {}", model.getName(), e1);
                Platform.runLater(() -> {
                    Alert alert = new AutosizeAlert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Couldn't follow/unfollow model");
                    alert.setContentText("I/O error while following/unfollowing model " + model.getName() + ": " + e1.getLocalizedMessage());
                    alert.showAndWait();
                });
                return false;
            } finally {
                setCursor(Cursor.DEFAULT);
            }
        });
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model.setName(model.getName());
        this.model.setDescription(model.getDescription());
        this.model.setPreview(model.getPreview());
        this.model.setTags(model.getTags());
        this.model.setUrl(model.getUrl());
        this.model.setSuspended(model.isSuspended());
        update();
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    private void update() {
        model.setSuspended(recorder.isSuspended(model));
        setRecording(recorder.isRecording(model));
        setImage(model.getPreview());
        String txt = recording ? "    " : "";
        txt += model.getDescription() != null ? model.getDescription() : "";
        topic.setText(txt);

        if(Config.getInstance().getSettings().determineResolution) {
            determineResolution();
        } else {
            resolutionBackground.setVisible(false);
            resolutionTag.setVisible(false);
        }
        requestLayout();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((model == null) ? 0 : model.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ThumbCell other = (ThumbCell) obj;
        if (model == null) {
            if (other.model != null)
                return false;
        } else if (!model.equals(other.model))
            return false;
        return true;
    }

    public void setThumbWidth(int width) {
        int height = (int) (width * imgAspectRatio);
        setPrefSize(width, height);
        setSize(width, height);
    }

    private void setSize(int w, int h) {
        if(iv.getImage() != null) {
            double aspectRatio = iv.getImage().getWidth() / iv.getImage().getHeight();
            if(aspectRatio > 1) {
                iv.setFitWidth(w);
            } else {
                iv.setFitHeight(h);
            }
        }
        setMinSize(w, h);
        setPrefSize(w, h);
        nameBackground.setWidth(w);
        nameBackground.setHeight(20);
        topicBackground.setWidth(w);
        topicBackground.setHeight(getHeight()-nameBackground.getHeight());
        topic.prefHeight(getHeight()-25);
        topic.maxHeight(getHeight()-25);
        int margin = 4;
        topic.maxWidth(w-margin*2);
        topic.setWrappingWidth(w-margin*2);
        selectionOverlay.setWidth(w);
        selectionOverlay.setHeight(getHeight());

        Rectangle clip = new Rectangle(w, h);
        clip.setArcWidth(10);
        clip.arcHeightProperty().bind(clip.arcWidthProperty());
        this.setClip(clip);
    }
}
