package ctbrec.ui;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Model;
import ctbrec.Recording;
import ctbrec.recorder.Recorder;
import ctbrec.sites.Site;
import ctbrec.ui.controls.AutoFillTextField;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
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
import javafx.scene.control.Tooltip;
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

    private ScheduledService<List<JavaFxModel>> updateService;
    private Recorder recorder;
    private List<Site> sites;

    FlowPane grid = new FlowPane();
    ScrollPane scrollPane = new ScrollPane();
    TableView<JavaFxModel> table = new TableView<JavaFxModel>();
    ObservableList<JavaFxModel> observableModels = FXCollections.observableArrayList();
    ContextMenu popup;

    Label modelLabel = new Label("Model");
    AutoFillTextField model;
    Button addModelButton = new Button("Record");

    public RecordedModelsTab(String title, Recorder recorder, List<Site> sites) {
        super(title);
        this.recorder = recorder;
        this.sites = sites;
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
        online.setPrefWidth(100);
        TableColumn<JavaFxModel, Boolean> recording = new TableColumn<>("Recording");
        recording.setCellValueFactory((cdf) -> cdf.getValue().getRecordingProperty());
        recording.setCellFactory(CheckBoxTableCell.forTableColumn(recording));
        recording.setPrefWidth(100);
        TableColumn<JavaFxModel, Boolean> paused = new TableColumn<>("Paused");
        paused.setCellValueFactory((cdf) -> cdf.getValue().getPausedProperty());
        paused.setCellFactory(CheckBoxTableCell.forTableColumn(paused));
        paused.setPrefWidth(100);
        table.getColumns().addAll(name, url, online, recording, paused);
        table.setItems(observableModels);
        table.addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> {
            popup = createContextMenu();
            if(popup != null) {
                popup.show(table, event.getScreenX(), event.getScreenY());
            }
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
        modelLabel.setPadding(new Insets(5, 0, 0, 0));
        ObservableList<String> suggestions = FXCollections.observableArrayList();
        sites.forEach(site -> suggestions.add(site.getName()));
        model = new AutoFillTextField(suggestions);
        model.setPrefWidth(300);
        model.setPromptText("e.g. MyFreeCams:ModelName");
        model.onActionHandler(e -> addModel(e));
        model.setTooltip(new Tooltip("To add a model enter SiteName:ModelName\n" +
                "press ENTER to confirm a suggested site name"));
        BorderPane.setMargin(addModelBox, new Insets(5));
        addModelButton.setOnAction((e) -> addModel(e));
        addModelBox.getChildren().addAll(modelLabel, model, addModelButton);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(5));
        root.setTop(addModelBox);
        root.setCenter(scrollPane);
        setContent(root);
    }

    private void addModel(ActionEvent e) {
        String[] parts = model.getText().trim().split(":");
        if (parts.length != 2) {
            Alert alert = new AutosizeAlert(Alert.AlertType.ERROR);
            alert.setTitle("Wrong format");
            alert.setHeaderText("Couldn't add model");
            alert.setContentText("Use something like \"MyFreeCams:ModelName\"");
            alert.showAndWait();
            return;
        }

        String siteName = parts[0];
        String modelName = parts[1];
        for (Site site : sites) {
            if (Objects.equals(siteName.toLowerCase(), site.getClass().getSimpleName().toLowerCase())) {
                try {
                    Model m = site.createModel(modelName);
                    recorder.startRecording(m);
                } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | IllegalStateException e1) {
                    Alert alert = new AutosizeAlert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Couldn't add model");
                    alert.setContentText("The model " + modelName + " could not be added: " + e1.getLocalizedMessage());
                    alert.showAndWait();
                }
                return;
            }
        }

        Alert alert = new AutosizeAlert(Alert.AlertType.ERROR);
        alert.setTitle("Unknown site");
        alert.setHeaderText("Couldn't add model");
        alert.setContentText("The site you entered is unknown");
        alert.showAndWait();
    };


    void initializeUpdateService() {
        updateService = createUpdateService();
        updateService.setPeriod(new Duration(TimeUnit.SECONDS.toMillis(2)));
        updateService.setOnSucceeded((event) -> {
            List<JavaFxModel> models = updateService.getValue();
            if(models == null) {
                return;
            }

            for (JavaFxModel updatedModel : models) {
                int index = observableModels.indexOf(updatedModel);
                if (index == -1) {
                    observableModels.add(updatedModel);
                } else {
                    // make sure to update the JavaFX online property, so that the table cell is updated
                    JavaFxModel oldModel = observableModels.get(index);
                    oldModel.setSuspended(updatedModel.isSuspended());
                    oldModel.getOnlineProperty().set(updatedModel.getOnlineProperty().get());
                    oldModel.getRecordingProperty().set(updatedModel.getRecordingProperty().get());
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

    private ScheduledService<List<JavaFxModel>> createUpdateService() {
        ScheduledService<List<JavaFxModel>> updateService = new ScheduledService<List<JavaFxModel>>() {
            @Override
            protected Task<List<JavaFxModel>> createTask() {
                return new Task<List<JavaFxModel>>() {
                    @Override
                    public List<JavaFxModel> call() throws InvalidKeyException, NoSuchAlgorithmException, IllegalStateException, IOException {
                        LOG.trace("Updating recorded models");
                        List<Recording> recordings = recorder.getRecordings();
                        List<Model> onlineModels = recorder.getOnlineModels();
                        return recorder.getModelsRecording()
                                .stream()
                                .map(m -> new JavaFxModel(m))
                                .peek(fxm -> {
                                    for (Recording recording : recordings) {
                                        if(recording.getStatus() == Recording.STATUS.RECORDING &&
                                                recording.getModelName().equals(fxm.getName()))
                                        {
                                            fxm.getRecordingProperty().set(true);
                                            break;
                                        }
                                    }

                                    for (Model onlineModel : onlineModels) {
                                        if(Objects.equals(onlineModel, fxm)) {
                                            fxm.getOnlineProperty().set(true);
                                            break;
                                        }
                                    }
                                })
                                .collect(Collectors.toList());
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
        JavaFxModel selectedModel = table.getSelectionModel().getSelectedItem();
        if(selectedModel == null) {
            return null;
        }
        MenuItem stop = new MenuItem("Remove Model");
        stop.setOnAction((e) -> stopAction());

        MenuItem copyUrl = new MenuItem("Copy URL");
        copyUrl.setOnAction((e) -> {
            Model selected = selectedModel;
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(selected.getUrl());
            clipboard.setContent(content);
        });

        MenuItem pauseRecording = new MenuItem("Pause Recording");
        pauseRecording.setOnAction((e) -> pauseRecording());
        MenuItem resumeRecording = new MenuItem("Resume Recording");
        resumeRecording.setOnAction((e) -> resumeRecording());
        MenuItem openInBrowser = new MenuItem("Open in Browser");
        openInBrowser.setOnAction((e) -> DesktopIntegration.open(selectedModel.getUrl()));
        MenuItem openInPlayer = new MenuItem("Open in Player");
        openInPlayer.setOnAction((e) -> openInPlayer(selectedModel));
        MenuItem switchStreamSource = new MenuItem("Switch resolution");
        switchStreamSource.setOnAction((e) -> switchStreamSource(selectedModel));

        ContextMenu menu = new ContextMenu(stop);
        menu.getItems().add(selectedModel.isSuspended() ? resumeRecording : pauseRecording);
        menu.getItems().addAll(copyUrl, openInPlayer, openInBrowser, switchStreamSource);
        return menu;
    }

    private void openInPlayer(JavaFxModel selectedModel) {
        table.setCursor(Cursor.WAIT);
        new Thread(() -> {
            Player.play(selectedModel);
            Platform.runLater(() -> table.setCursor(Cursor.DEFAULT));
        }).start();
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
        StreamSourceSelectionDialog.show(fxModel.getDelegate(), onSuccess, onFail);
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

    private void pauseRecording() {
        JavaFxModel model = table.getSelectionModel().getSelectedItem();
        Model delegate = table.getSelectionModel().getSelectedItem().getDelegate();
        if (delegate != null) {
            table.setCursor(Cursor.WAIT);
            new Thread() {
                @Override
                public void run() {
                    try {
                        recorder.suspendRecording(delegate);
                        Platform.runLater(() -> model.setSuspended(true));
                    } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | IllegalStateException e1) {
                        LOG.error("Couldn't pause recording", e1);
                        Platform.runLater(() -> {
                            Alert alert = new AutosizeAlert(Alert.AlertType.ERROR);
                            alert.setTitle("Error");
                            alert.setHeaderText("Couldn't pause recording");
                            alert.setContentText("Error while pausing the recording: " + e1.getLocalizedMessage());
                            alert.showAndWait();
                        });
                    } finally {
                        table.setCursor(Cursor.DEFAULT);
                    }
                }
            }.start();
        }
    };

    private void resumeRecording() {
        JavaFxModel model = table.getSelectionModel().getSelectedItem();
        Model delegate = table.getSelectionModel().getSelectedItem().getDelegate();
        if (delegate != null) {
            table.setCursor(Cursor.WAIT);
            new Thread() {
                @Override
                public void run() {
                    try {
                        recorder.resumeRecording(delegate);
                        Platform.runLater(() -> model.setSuspended(false));
                    } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | IllegalStateException e1) {
                        LOG.error("Couldn't resume recording", e1);
                        Platform.runLater(() -> {
                            Alert alert = new AutosizeAlert(Alert.AlertType.ERROR);
                            alert.setTitle("Error");
                            alert.setHeaderText("Couldn't resume recording");
                            alert.setContentText("Error while resuming the recording: " + e1.getLocalizedMessage());
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
