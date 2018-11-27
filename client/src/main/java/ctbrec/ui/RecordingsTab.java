package ctbrec.ui;

import static javafx.scene.control.ButtonType.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iheartradio.m3u8.ParseException;
import com.iheartradio.m3u8.PlaylistException;

import ctbrec.Config;
import ctbrec.Recording;
import ctbrec.Recording.STATUS;
import ctbrec.StringUtil;
import ctbrec.recorder.Recorder;
import ctbrec.recorder.download.MergedHlsDownload;
import ctbrec.sites.Site;
import ctbrec.ui.controls.Toast;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import javafx.util.Duration;

public class RecordingsTab extends Tab implements TabSelectionListener {
    private static final transient Logger LOG = LoggerFactory.getLogger(RecordingsTab.class);

    private ScheduledService<List<JavaFxRecording>> updateService;
    private Config config;
    private Recorder recorder;
    @SuppressWarnings("unused")
    private List<Site> sites;
    private long spaceTotal = -1;
    private long spaceFree = -1;

    FlowPane grid = new FlowPane();
    ScrollPane scrollPane = new ScrollPane();
    TableView<JavaFxRecording> table = new TableView<JavaFxRecording>();
    ObservableList<JavaFxRecording> observableRecordings = FXCollections.observableArrayList();
    ContextMenu popup;
    ProgressBar spaceLeft;
    Label spaceLabel;

    public RecordingsTab(String title, Recorder recorder, Config config, List<Site> sites) {
        super(title);
        this.recorder = recorder;
        this.config = config;
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
        TableColumn<JavaFxRecording, String> name = new TableColumn<>("Model");
        name.setPrefWidth(200);
        name.setCellValueFactory(new PropertyValueFactory<JavaFxRecording, String>("modelName"));
        TableColumn<JavaFxRecording, Instant> date = new TableColumn<>("Date");
        date.setCellValueFactory((cdf) -> {
            Instant instant = cdf.getValue().getStartDate();
            return new SimpleObjectProperty<Instant>(instant);
        });
        date.setCellFactory(new Callback<TableColumn<JavaFxRecording, Instant>, TableCell<JavaFxRecording, Instant>>() {
            @Override
            public TableCell<JavaFxRecording, Instant> call(TableColumn<JavaFxRecording, Instant> param) {
                TableCell<JavaFxRecording, Instant> cell = new TableCell<JavaFxRecording, Instant>() {
                    @Override
                    protected void updateItem(Instant instant, boolean empty) {
                        if(empty || instant == null) {
                            setText(null);
                        } else {
                            ZonedDateTime time = instant.atZone(ZoneId.systemDefault());
                            DateTimeFormatter dtf = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.MEDIUM);
                            setText(dtf.format(time));
                        }
                    }
                };
                return cell;
            }
        });
        date.setPrefWidth(200);
        TableColumn<JavaFxRecording, String> status = new TableColumn<>("Status");
        status.setCellValueFactory((cdf) -> cdf.getValue().getStatusProperty());
        status.setPrefWidth(300);
        TableColumn<JavaFxRecording, String> progress = new TableColumn<>("Progress");
        progress.setCellValueFactory((cdf) -> cdf.getValue().getProgressProperty());
        progress.setPrefWidth(100);
        TableColumn<JavaFxRecording, Number> size = new TableColumn<>("Size");
        size.setStyle("-fx-alignment: CENTER-RIGHT;");
        size.setPrefWidth(100);
        size.setCellValueFactory(cdf -> cdf.getValue().getSizeProperty());
        size.setCellFactory(new Callback<TableColumn<JavaFxRecording, Number>, TableCell<JavaFxRecording, Number>>() {
            @Override
            public TableCell<JavaFxRecording, Number> call(TableColumn<JavaFxRecording, Number> param) {
                TableCell<JavaFxRecording, Number> cell = new TableCell<JavaFxRecording, Number>() {
                    @Override
                    protected void updateItem(Number sizeInByte, boolean empty) {
                        if(empty || sizeInByte == null) {
                            setText(null);
                        } else {
                            DecimalFormat df = new DecimalFormat("0.00");
                            String unit = "Bytes";
                            double size = sizeInByte.doubleValue();
                            if(size > 1024.0 * 1024 * 1024) {
                                size = size / 1024.0 / 1024 / 1024;
                                unit = "GiB";
                            } else if(size > 1024.0 * 1024) {
                                size = size / 1024.0 / 1024;
                                unit = "MiB";
                            } else if(size > 1024.0) {
                                size = size / 1024.0;
                                unit = "KiB";
                            }
                            setText(df.format(size) + ' ' + unit);
                        }
                    }
                };
                return cell;
            }
        });

        table.getColumns().addAll(name, date, status, progress, size);
        table.setItems(observableRecordings);
        table.addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> {
            Recording recording = table.getSelectionModel().getSelectedItem();
            if(recording != null) {
                popup = createContextMenu(recording);
                if(!popup.getItems().isEmpty()) {
                    popup.show(table, event.getScreenX(), event.getScreenY());
                }
            }
            event.consume();
        });
        table.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if(popup != null) {
                popup.hide();
            }
        });
        table.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if(event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                Recording recording = table.getSelectionModel().getSelectedItem();
                if(recording != null) {
                    play(recording);
                }
            }
        });
        table.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            JavaFxRecording recording = table.getSelectionModel().getSelectedItem();
            if (recording != null) {
                if (event.getCode() == KeyCode.DELETE) {
                    delete(recording);
                } else if (event.getCode() == KeyCode.ENTER) {
                    if(recording.getStatus() == STATUS.FINISHED) {
                        play(recording);
                    }
                }
            }
        });
        scrollPane.setContent(table);

        HBox spaceBox = new HBox(5);
        Label l = new Label("Space left on device");
        HBox.setMargin(l, new Insets(2, 0, 0, 0));
        spaceBox.getChildren().add(l);
        spaceLeft = new ProgressBar(0);
        spaceLeft.setPrefSize(200, 22);
        spaceLabel = new Label();
        spaceLabel.setFont(Font.font(11));
        StackPane stack = new StackPane(spaceLeft, spaceLabel);
        spaceBox.getChildren().add(stack);
        BorderPane.setMargin(spaceBox, new Insets(5));

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(5));
        root.setTop(spaceBox);
        root.setCenter(scrollPane);
        setContent(root);

        restoreState();
    }

    void initializeUpdateService() {
        updateService = createUpdateService();
        updateService.setPeriod(new Duration(TimeUnit.SECONDS.toMillis(2)));
        updateService.setOnSucceeded((event) -> {
            updateRecordingsTable();
            updateFreeSpaceDisplay();
        });
        updateService.setOnFailed((event) -> {
            LOG.info("Couldn't get list of recordings from recorder", event.getSource().getException());
            AutosizeAlert autosizeAlert = new AutosizeAlert(AlertType.ERROR);
            autosizeAlert.setTitle("Whoopsie!");
            autosizeAlert.setHeaderText("Recordings not available");
            autosizeAlert.setContentText("An error occured while retrieving the list of recordings");
            autosizeAlert.showAndWait();
        });
    }

    private void updateFreeSpaceDisplay() {
        if(spaceTotal != -1 && spaceFree != -1) {
            double free = ((double)spaceFree) / spaceTotal;
            spaceLeft.setProgress(free);
            double totalGiB = ((double) spaceTotal) / 1024 / 1024 / 1024;
            double freeGiB = ((double) spaceFree) / 1024 / 1024 / 1024;
            DecimalFormat df = new DecimalFormat("0.00");
            String tt = df.format(freeGiB) + " / " + df.format(totalGiB) + " GiB";
            spaceLeft.setTooltip(new Tooltip(tt));
            spaceLabel.setText(tt);
        }
    }

    private void updateRecordingsTable() {
        List<JavaFxRecording> recordings = updateService.getValue();
        if (recordings == null) {
            return;
        }

        for (Iterator<JavaFxRecording> iterator = observableRecordings.iterator(); iterator.hasNext();) {
            JavaFxRecording old = iterator.next();
            if (!recordings.contains(old)) {
                // remove deleted recordings
                iterator.remove();
            }
        }
        for (JavaFxRecording recording : recordings) {
            if (!observableRecordings.contains(recording)) {
                // add new recordings
                observableRecordings.add(recording);
            } else {
                // update existing ones
                int index = observableRecordings.indexOf(recording);
                JavaFxRecording old = observableRecordings.get(index);
                old.update(recording);
            }
        }
        table.sort();
    }

    private ScheduledService<List<JavaFxRecording>> createUpdateService() {
        ScheduledService<List<JavaFxRecording>>  updateService = new ScheduledService<List<JavaFxRecording>>() {
            @Override
            protected Task<List<JavaFxRecording>> createTask() {
                return new Task<List<JavaFxRecording>>() {
                    @Override
                    public List<JavaFxRecording> call() throws IOException, InvalidKeyException, NoSuchAlgorithmException, IllegalStateException {
                        updateSpace();

                        List<JavaFxRecording> recordings = new ArrayList<>();
                        for (Recording rec : recorder.getRecordings()) {
                            recordings.add(new JavaFxRecording(rec));
                        }
                        return recordings;
                    }

                    private void updateSpace() {
                        try {
                            spaceTotal = recorder.getTotalSpaceBytes();
                            spaceFree = recorder.getFreeSpaceBytes();
                        } catch (IOException e) {
                            LOG.error("Couldn't update free space", e);
                        }
                    }
                };
            }
        };
        ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("RecordingsTab UpdateService");
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

    private ContextMenu createContextMenu(Recording recording) {
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.setHideOnEscape(true);
        contextMenu.setAutoHide(true);
        contextMenu.setAutoFix(true);

        MenuItem openInPlayer = new MenuItem("Open in Player");
        openInPlayer.setOnAction((e) -> {
            play(recording);
        });
        if(recording.getStatus() == STATUS.FINISHED || Config.getInstance().getSettings().localRecording) {
            contextMenu.getItems().add(openInPlayer);
        }

        // TODO find a way to reenable this
        //        MenuItem stopRecording = new MenuItem("Stop recording");
        //        stopRecording.setOnAction((e) -> {
        //            Model m = site.createModel(recording.getModelName());
        //            try {
        //                recorder.stopRecording(m);
        //            } catch (Exception e1) {
        //                showErrorDialog("Stop recording", "Couldn't stop recording of model " + m.getName(), e1);
        //            }
        //        });
        //        if(recording.getStatus() == STATUS.RECORDING) {
        //            contextMenu.getItems().add(stopRecording);
        //        }

        MenuItem deleteRecording = new MenuItem("Delete");
        deleteRecording.setOnAction((e) -> {
            delete(recording);
        });
        if(recording.getStatus() == STATUS.FINISHED) {
            contextMenu.getItems().add(deleteRecording);
        }

        MenuItem openDir = new MenuItem("Open directory");
        openDir.setOnAction((e) -> {
            String recordingsDir = Config.getInstance().getSettings().recordingsDir;
            String path = recording.getPath();
            File tsFile = new File(recordingsDir, path);
            new Thread(() -> {
                DesktopIntegration.open(tsFile.getParent());
            }).start();
        });
        if(Config.getInstance().getSettings().localRecording) {
            contextMenu.getItems().add(openDir);
        }

        MenuItem downloadRecording = new MenuItem("Download");
        downloadRecording.setOnAction((e) -> {
            try {
                download(recording);
            } catch (IOException | ParseException | PlaylistException e1) {
                showErrorDialog("Error while downloading recording", "The recording could not be downloaded", e1);
                LOG.error("Error while downloading recording", e1);
            }
        });
        if (!Config.getInstance().getSettings().localRecording && recording.getStatus() == STATUS.FINISHED) {
            contextMenu.getItems().add(downloadRecording);
        }

        return contextMenu;
    }

    private void download(Recording recording) throws IOException, ParseException, PlaylistException {
        String filename = recording.getPath().replaceAll("/", "-") + ".ts";
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(filename);
        if(config.getSettings().lastDownloadDir != null && !config.getSettings().lastDownloadDir.equals("")) {
            File dir = new File(config.getSettings().lastDownloadDir);
            while(!dir.exists()) {
                dir = dir.getParentFile();
            }
            chooser.setInitialDirectory(dir);
        }
        File target = chooser.showSaveDialog(null);
        if(target != null) {
            config.getSettings().lastDownloadDir = target.getParent();
            String hlsBase = "http://" + config.getSettings().httpServer + ":" + config.getSettings().httpPort + "/hls";
            URL url = new URL(hlsBase + "/" + recording.getPath() + "/playlist.m3u8");
            LOG.info("Downloading {}", recording.getPath());

            Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        MergedHlsDownload download = new MergedHlsDownload(CamrecApplication.httpClient);
                        download.start(url.toString(), target, (progress) -> {
                            Platform.runLater(() -> {
                                if (progress == 100) {
                                    recording.setStatus(STATUS.FINISHED);
                                    recording.setProgress(-1);
                                    LOG.debug("Download finished for recording {}", recording.getPath());
                                } else {
                                    recording.setStatus(STATUS.DOWNLOADING);
                                    recording.setProgress(progress);
                                }
                            });
                        });
                    } catch (FileNotFoundException e) {
                        showErrorDialog("Error while downloading recording", "The target file couldn't be created", e);
                        LOG.error("Error while downloading recording", e);
                    } catch (IOException e) {
                        showErrorDialog("Error while downloading recording", "The recording could not be downloaded", e);
                        LOG.error("Error while downloading recording", e);
                    } finally {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                recording.setStatus(STATUS.FINISHED);
                                recording.setProgress(-1);
                            }
                        });
                    }
                }
            };
            t.setDaemon(true);
            t.setName("Download Thread " + recording.getPath());
            t.start();

            recording.setStatus(STATUS.DOWNLOADING);
            recording.setProgress(0);
        }
    }

    //    private void download(Recording recording) throws IOException, ParseException, PlaylistException {
    //        String filename = recording.getPath().replaceAll("/", "-") + ".ts";
    //        FileChooser chooser = new FileChooser();
    //        chooser.setInitialFileName(filename);
    //        if(config.getSettings().lastDownloadDir != null && !config.getSettings().lastDownloadDir.equals("")) {
    //            File dir = new File(config.getSettings().lastDownloadDir);
    //            while(!dir.exists()) {
    //                dir = dir.getParentFile();
    //            }
    //            chooser.setInitialDirectory(dir);
    //        }
    //        File target = chooser.showSaveDialog(null);
    //        if(target != null) {
    //            config.getSettings().lastDownloadDir = target.getParent();
    //            String hlsBase = "http://" + config.getSettings().httpServer + ":" + config.getSettings().httpPort + "/hls";
    //            URL url = new URL(hlsBase + "/" + recording.getPath() + "/playlist.m3u8");
    //            LOG.info("Downloading {}", recording.getPath());
    //
    //            PlaylistParser parser = new PlaylistParser(url.openStream(), Format.EXT_M3U, Encoding.UTF_8);
    //            Playlist playlist = parser.parse();
    //            MediaPlaylist mediaPlaylist = playlist.getMediaPlaylist();
    //            List<TrackData> tracks = mediaPlaylist.getTracks();
    //            List<String> segmentUris = new ArrayList<>();
    //            for (TrackData trackData : tracks) {
    //                String segmentUri = hlsBase + "/" + recording.getPath() + "/" + trackData.getUri();
    //                segmentUris.add(segmentUri);
    //            }
    //
    //            Thread t = new Thread() {
    //                @Override
    //                public void run() {
    //                    try(FileOutputStream fos = new FileOutputStream(target)) {
    //                        for (int i = 0; i < segmentUris.size(); i++) {
    //                            URL segment = new URL(segmentUris.get(i));
    //                            InputStream in = segment.openStream();
    //                            byte[] b = new byte[1024];
    //                            int length = -1;
    //                            while( (length = in.read(b)) >= 0 ) {
    //                                fos.write(b, 0, length);
    //                            }
    //                            in.close();
    //                            int progress = (int) (i * 100.0 / segmentUris.size());
    //                            Platform.runLater(new Runnable() {
    //                                @Override
    //                                public void run() {
    //                                    recording.setStatus(STATUS.DOWNLOADING);
    //                                    recording.setProgress(progress);
    //                                }
    //                            });
    //                        }
    //
    //                    } catch (FileNotFoundException e) {
    //                        showErrorDialog("Error while downloading recording", "The target file couldn't be created", e);
    //                        LOG.error("Error while downloading recording", e);
    //                    } catch (IOException e) {
    //                        showErrorDialog("Error while downloading recording", "The recording could not be downloaded", e);
    //                        LOG.error("Error while downloading recording", e);
    //                    } finally {
    //                        Platform.runLater(new Runnable() {
    //                            @Override
    //                            public void run() {
    //                                recording.setStatus(STATUS.FINISHED);
    //                                recording.setProgress(-1);
    //                            }
    //                        });
    //                    }
    //                }
    //            };
    //            t.setDaemon(true);
    //            t.setName("Download Thread " + recording.getPath());
    //            t.start();
    //        }
    //    }

    private void showErrorDialog(final String title, final String msg, final Exception e) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                AutosizeAlert autosizeAlert = new AutosizeAlert(AlertType.ERROR);
                autosizeAlert.setTitle(title);
                autosizeAlert.setHeaderText(msg);
                autosizeAlert.setContentText("An error occured: " + e.getLocalizedMessage());
                autosizeAlert.showAndWait();
            }
        });
    }

    private void play(Recording recording) {
        final String url;
        if (Config.getInstance().getSettings().localRecording) {
            new Thread() {
                @Override
                public void run() {
                    boolean started = Player.play(recording);
                    if(started) {
                        Platform.runLater(() -> Toast.makeText(getTabPane().getScene(), "Starting Player", 2000, 500, 500));
                    }
                }
            }.start();
        } else {
            String hlsBase = "http://" + config.getSettings().httpServer + ":" + config.getSettings().httpPort + "/hls";
            url = hlsBase + "/" + recording.getPath() + "/playlist.m3u8";
            new Thread() {
                @Override
                public void run() {
                    boolean started = Player.play(url);
                    if(started) {
                        Platform.runLater(() -> Toast.makeText(getTabPane().getScene(), "Starting Player", 2000, 500, 500));
                    }
                }
            }.start();
        }

    }

    private void delete(Recording r) {
        if(r.getStatus() != STATUS.FINISHED) {
            return;
        }
        table.setCursor(Cursor.WAIT);
        String msg = "Delete " + r.getModelName() + "/" + r.getStartDate() + " for good?";
        AutosizeAlert confirm = new AutosizeAlert(AlertType.CONFIRMATION, msg, YES, NO);
        confirm.setTitle("Delete recording?");
        confirm.setHeaderText(msg);
        confirm.setContentText("");
        confirm.showAndWait();
        if (confirm.getResult() == ButtonType.YES) {
            Thread deleteThread = new Thread() {
                @Override
                public void run() {
                    try {
                        recorder.delete(r);
                        Platform.runLater(() -> observableRecordings.remove(r));
                    } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | IllegalStateException e1) {
                        LOG.error("Error while deleting recording", e1);
                        showErrorDialog("Error while deleting recording", "Recording not deleted", e1);
                    } finally {
                        table.setCursor(Cursor.DEFAULT);
                    }
                }
            };
            deleteThread.start();
        } else {
            table.setCursor(Cursor.DEFAULT);
        }
    }

    public void saveState() {
        if(!table.getSortOrder().isEmpty()) {
            TableColumn<JavaFxRecording, ?> col = table.getSortOrder().get(0);
            Config.getInstance().getSettings().recordingsSortColumn = col.getText();
            Config.getInstance().getSettings().recordingsSortType = col.getSortType().toString();
        }
        double[] columnWidths = new double[table.getColumns().size()];
        for (int i = 0; i < columnWidths.length; i++) {
            columnWidths[i] = table.getColumns().get(i).getWidth();
        }
        Config.getInstance().getSettings().recordingsColumnWidths = columnWidths;
    };

    private void restoreState() {
        String sortCol = Config.getInstance().getSettings().recordingsSortColumn;
        if(StringUtil.isNotBlank(sortCol)) {
            for (TableColumn<JavaFxRecording, ?> col : table.getColumns()) {
                if(Objects.equals(sortCol, col.getText())) {
                    col.setSortType(SortType.valueOf(Config.getInstance().getSettings().recordingsSortType));
                    table.getSortOrder().clear();
                    table.getSortOrder().add(col);
                    break;
                }
            }
        }

        double[] columnWidths = Config.getInstance().getSettings().recordingsColumnWidths;
        if(columnWidths != null && columnWidths.length == table.getColumns().size()) {
            for (int i = 0; i < columnWidths.length; i++) {
                table.getColumns().get(i).setPrefWidth(columnWidths[i]);
            }
        }
    }
}
