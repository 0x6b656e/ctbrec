package ctbrec.ui;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Model;
import ctbrec.recorder.Recorder;
import ctbrec.sites.Site;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

public class RecordedModelsTab extends Tab implements TabSelectionListener {
    private static final transient Logger LOG = LoggerFactory.getLogger(RecordedModelsTab.class);

    static BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    static ExecutorService threadPool = new ThreadPoolExecutor(2, 2, 10, TimeUnit.MINUTES, queue);

    private ScheduledService<List<Model>> updateService;
    private Recorder recorder;
    private Site site;

    FlowPane grid = new FlowPane();
    ScrollPane scrollPane = new ScrollPane();
    TableView<JavaFxModel> table = new TableView<JavaFxModel>();
    ObservableList<JavaFxModel> observableModels = FXCollections.observableArrayList();
    ContextMenu popup = createContextMenu();

    Label modelLabel = new Label("Model");
    TextField model = new TextField();
    Button addModelButton = new Button("Record");

    public RecordedModelsTab(String title, Recorder recorder, Site site) {
        super(title);
        this.recorder = recorder;
        this.site = site;
        createGui();
        setClosable(false);
        initializeUpdateService();
    }

    @SuppressWarnings("unchecked")
    private void createGui() {
        grid.setPadding(new Insets(5));
        grid.setHgap(5);
        grid.setVgap(5);

        scrollPane.setContent(grid);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);
        BorderPane.setMargin(scrollPane, new Insets(5));

        table.setEditable(false);
        TableColumn<JavaFxModel, String> name = new TableColumn<>("Model");
        name.setPrefWidth(200);
        name.setCellValueFactory(new PropertyValueFactory<JavaFxModel, String>("name"));
        TableColumn<JavaFxModel, String> url = new TableColumn<>("URL");
        url.setCellValueFactory(new PropertyValueFactory<JavaFxModel, String>("url"));
        url.setPrefWidth(400);
        TableColumn<JavaFxModel, Boolean> online = new TableColumn<>("Online");
        online.setCellValueFactory((cdf) -> cdf.getValue().getOnlineProperty());
        online.setCellFactory(CheckBoxTableCell.forTableColumn(online));
        online.setPrefWidth(60);
        table.getColumns().addAll(name, url, online);
        table.setItems(observableModels);
        table.addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> {
            popup = createContextMenu();
            popup.show(table, event.getScreenX(), event.getScreenY());
            event.consume();
        });
        table.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if(popup != null) {
                popup.hide();
            }
        });
        table.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if(event.getCode() == KeyCode.DELETE) {
                stopAction();
            }
        });
        scrollPane.setContent(table);

        HBox addModelBox = new HBox(5);
        addModelBox.getChildren().addAll(modelLabel, model, addModelButton);
        modelLabel.setPadding(new Insets(5, 0, 0, 0));
        model.setPrefWidth(300);
        BorderPane.setMargin(addModelBox, new Insets(5));
        addModelButton.setOnAction((e) -> {
            Model m = site.createModel(model.getText());
            try {
                recorder.startRecording(m);
            } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | IllegalStateException e1) {
                Alert alert = new AutosizeAlert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Couldn't add model");
                alert.setContentText("The model " + m.getName() + " could not be added: " + e1.getLocalizedMessage());
                alert.showAndWait();
            }
        });

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(5));
        root.setTop(addModelBox);
        root.setCenter(scrollPane);
        setContent(root);
    }

    void initializeUpdateService() {
        updateService = createUpdateService();
        updateService.setPeriod(new Duration(TimeUnit.SECONDS.toMillis(2)));
        updateService.setOnSucceeded((event) -> {
            List<Model> models = updateService.getValue();
            if(models == null) {
                return;
            }
            queue.clear();
            for (Model model : models) {
                int index = observableModels.indexOf(model);
                if (index == -1) {
                    observableModels.add(new JavaFxModel(model));
                } else {
                    // make sure to update the JavaFX online property, so that the table cell is updated
                    JavaFxModel javaFxModel = observableModels.get(index);
                    threadPool.submit(() -> {
                        try {
                            javaFxModel.getOnlineProperty().set(javaFxModel.isOnline());
                        } catch (IOException | ExecutionException | InterruptedException e) {}
                    });
                }
            }
            for (Iterator<JavaFxModel> iterator = observableModels.iterator(); iterator.hasNext();) {
                Model model = iterator.next();
                if (!models.contains(model)) {
                    iterator.remove();
                }
            }

        });
        updateService.setOnFailed((event) -> {
            LOG.info("Couldn't get list of models from recorder", event.getSource().getException());
        });
    }

    private ScheduledService<List<Model>> createUpdateService() {
        ScheduledService<List<Model>> updateService = new ScheduledService<List<Model>>() {
            @Override
            protected Task<List<Model>> createTask() {
                return new Task<List<Model>>() {
                    @Override
                    public List<Model> call() {
                        LOG.debug("Updating recorded models");
                        return recorder.getModelsRecording();
                    }
                };
            }
        };
        ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("RecordedModelsTab UpdateService");
                return t;
            }
        });
        updateService.setExecutor(executor);
        return updateService;
    }

    @Override
    public void selected() {
        if (updateService != null) {
            updateService.reset();
            updateService.restart();
        }
    }

    @Override
    public void deselected() {
        if (updateService != null) {
            updateService.cancel();
        }
    }

    private ContextMenu createContextMenu() {
        MenuItem stop = new MenuItem("Stop Recording");
        stop.setOnAction((e) -> stopAction());

        MenuItem copyUrl = new MenuItem("Copy URL");
        copyUrl.setOnAction((e) -> {
            Model selected = table.getSelectionModel().getSelectedItem();
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(selected.getUrl());
            clipboard.setContent(content);
        });

        MenuItem openInBrowser = new MenuItem("Open in Browser");
        openInBrowser.setOnAction((e) -> DesktopIntergation.open(table.getSelectionModel().getSelectedItem().getUrl()));
        MenuItem openInPlayer = new MenuItem("Open in Player");
        openInPlayer.setOnAction((e) -> Player.play(table.getSelectionModel().getSelectedItem().getUrl()));
        MenuItem switchStreamSource = new MenuItem("Switch resolution");
        switchStreamSource.setOnAction((e) -> switchStreamSource(table.getSelectionModel().getSelectedItem()));

        return new ContextMenu(stop, copyUrl, openInBrowser, switchStreamSource);
    }

    private void switchStreamSource(JavaFxModel fxModel) {
        try {
            if(!fxModel.isOnline()) {
                Alert alert = new AutosizeAlert(Alert.AlertType.INFORMATION);
                alert.setTitle("Switch resolution");
                alert.setHeaderText("Couldn't switch stream resolution");
                alert.setContentText("The resolution can only be changed, when the model is online");
                alert.showAndWait();
                return;
            }
        } catch (IOException | ExecutionException | InterruptedException e1) {
            Alert alert = new AutosizeAlert(Alert.AlertType.INFORMATION);
            alert.setTitle("Switch resolution");
            alert.setHeaderText("Couldn't switch stream resolution");
            alert.setContentText("An error occured while checking, if the model is online");
            alert.showAndWait();
            return;
        }

        Function<Model, Void> onSuccess = (m) -> {
            try {
                recorder.switchStreamSource(m);
            } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalStateException | IOException e) {
                LOG.error("Error while switching stream resolution", e);
                showStreamSwitchErrorDialog(e);
            }
            return null;
        };
        Function<Throwable, Void> onFail = (t) -> {
            LOG.error("Error while switching stream resolution", t);
            showStreamSwitchErrorDialog(t);
            return null;
        };
        StreamSourceSelectionDialog.show(fxModel.getDelegate(), site.getHttpClient(), onSuccess, onFail);
    }

    private void showStreamSwitchErrorDialog(Throwable throwable) {
        Alert alert = new AutosizeAlert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Couldn't switch stream resolution");
        alert.setContentText("Error while switching stream resolution: " + throwable.getLocalizedMessage());
        alert.showAndWait();
    }

    private void stopAction() {
        Model selected = table.getSelectionModel().getSelectedItem().getDelegate();
        if (selected != null) {
            table.setCursor(Cursor.WAIT);
            new Thread() {
                @Override
                public void run() {
                    try {
                        recorder.stopRecording(selected);
                        observableModels.remove(selected);
                    } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | IllegalStateException e1) {
                        LOG.error("Couldn't stop recording", e1);
                        Platform.runLater(() -> {
                            Alert alert = new AutosizeAlert(Alert.AlertType.ERROR);
                            alert.setTitle("Error");
                            alert.setHeaderText("Couldn't stop recording");
                            alert.setContentText("Error while stopping the recording: " + e1.getLocalizedMessage());
                            alert.showAndWait();
                        });
                    } finally {
                        table.setCursor(Cursor.DEFAULT);
                    }
                }
            }.start();
        }
    };
}
