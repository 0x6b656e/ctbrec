package ctbrec.ui;

import static javafx.scene.control.ButtonType.NO;
import static javafx.scene.control.ButtonType.YES;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iheartradio.m3u8.ParseException;
import com.iheartradio.m3u8.PlaylistException;

import ctbrec.Config;
import ctbrec.HttpClient;
import ctbrec.Model;
import ctbrec.Recording;
import ctbrec.Recording.STATUS;
import ctbrec.recorder.Recorder;
import ctbrec.recorder.download.MergedHlsDownload;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;
import javafx.util.Duration;

public class RecordingsTab extends Tab implements TabSelectionListener {
    private static final transient Logger LOG = LoggerFactory.getLogger(RecordingsTab.class);

    private ScheduledService<List<JavaFxRecording>> updateService;
    private Config config;
    private Recorder recorder;

    FlowPane grid = new FlowPane();
    ScrollPane scrollPane = new ScrollPane();
    TableView<JavaFxRecording> table = new TableView<JavaFxRecording>();
    ObservableList<JavaFxRecording> observableRecordings = FXCollections.observableArrayList();
    ContextMenu popup;

    public RecordingsTab(String title, Recorder recorder, Config config) {
        super(title);
        this.recorder = recorder;
        this.config = config;
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
        TableColumn<JavaFxRecording, String> date = new TableColumn<>("Date");
        date.setCellValueFactory(new PropertyValueFactory<JavaFxRecording, String>("startDate"));
        date.setPrefWidth(200);
        TableColumn<JavaFxRecording, String> status = new TableColumn<>("Status");
        status.setCellValueFactory((cdf) -> cdf.getValue().getStatusProperty());
        status.setPrefWidth(300);
        TableColumn<JavaFxRecording, String> progress = new TableColumn<>("Progress");
        progress.setCellValueFactory((cdf) -> cdf.getValue().getProgressProperty());
        progress.setPrefWidth(100);
        TableColumn<JavaFxRecording, String> size = new TableColumn<>("Size");
        size.setCellValueFactory((cdf) -> cdf.getValue().getSizeProperty());
        size.setPrefWidth(100);

        table.getColumns().addAll(name, date, status, progress, size);
        table.setItems(observableRecordings);
        table.addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> {
            Recording recording = table.getSelectionModel().getSelectedItem();
            popup = createContextMenu(recording);
            if(!popup.getItems().isEmpty()) {
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
                JavaFxRecording recording = table.getSelectionModel().getSelectedItem();
                if(recording != null) {
                    delete(recording);
                }
            }
        });
        scrollPane.setContent(table);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(5));
        root.setCenter(scrollPane);
        setContent(root);
    }

    void initializeUpdateService() {
        updateService = createUpdateService();
        updateService.setPeriod(new Duration(TimeUnit.SECONDS.toMillis(2)));
        updateService.setOnSucceeded((event) -> {
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

    private ScheduledService<List<JavaFxRecording>> createUpdateService() {
        ScheduledService<List<JavaFxRecording>>  updateService = new ScheduledService<List<JavaFxRecording>>() {
            @Override
            protected Task<List<JavaFxRecording>> createTask() {
                return new Task<List<JavaFxRecording>>() {
                    @Override
                    public List<JavaFxRecording> call() throws IOException, InvalidKeyException, NoSuchAlgorithmException, IllegalStateException {
                        List<JavaFxRecording> recordings = new ArrayList<>();
                        for (Recording rec : recorder.getRecordings()) {
                            recordings.add(new JavaFxRecording(rec));
                        }
                        return recordings;
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

        MenuItem stopRecording = new MenuItem("Stop recording");
        stopRecording.setOnAction((e) -> {
            Model m = new Model();
            m.setName(recording.getModelName());
            m.setUrl(CtbrecApplication.BASE_URI + '/' + recording.getModelName() + '/');
            try {
                recorder.stopRecording(m);
            } catch (Exception e1) {
                showErrorDialog("Stop recording", "Couldn't stop recording of model " + m.getName(), e1);
            }
        });
        if(recording.getStatus() == STATUS.RECORDING) {
            contextMenu.getItems().add(stopRecording);
        }

        MenuItem deleteRecording = new MenuItem("Delete");
        deleteRecording.setOnAction((e) -> {
            delete(recording);
        });
        if(recording.getStatus() == STATUS.FINISHED) {
            contextMenu.getItems().add(deleteRecording);
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
                        MergedHlsDownload download = new MergedHlsDownload(HttpClient.getInstance());
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
                    Player.play(recording);
                }
            }.start();
        } else {
            String hlsBase = "http://" + config.getSettings().httpServer + ":" + config.getSettings().httpPort + "/hls";
            url = hlsBase + "/" + recording.getPath() + "/playlist.m3u8";
            new Thread() {
                @Override
                public void run() {
                    Player.play(url);
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
}
