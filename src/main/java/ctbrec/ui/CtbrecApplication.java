package ctbrec.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import ctbrec.Config;
import ctbrec.HttpClient;
import ctbrec.Version;
import ctbrec.recorder.LocalRecorder;
import ctbrec.recorder.Recorder;
import ctbrec.recorder.RemoteRecorder;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import okhttp3.Request;
import okhttp3.Response;

public class CtbrecApplication extends Application {

    static final transient Logger LOG = LoggerFactory.getLogger(CtbrecApplication.class);
    public static final String BASE_URI = "https://chaturbate.com";

    private Config config;
    private Recorder recorder;
    private HttpClient client;
    static HostServices hostServices;
    private SettingsTab settingsTab;
    private TabPane tabPane = new TabPane();

    @Override
    public void start(Stage primaryStage) throws Exception {
        loadConfig();
        hostServices = getHostServices();
        client = HttpClient.getInstance();
        createRecorder();
        doInitialLogin();
        createGui(primaryStage);
        checkForUpdates();
    }

    private void createGui(Stage primaryStage) throws IOException {
        LOG.debug("Creating GUI");
        primaryStage.setTitle("CTB Recorder " + getVersion());
        InputStream icon = getClass().getResourceAsStream("/icon.png");
        primaryStage.getIcons().add(new Image(icon));

        tabPane = new TabPane();
        tabPane.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
            @Override
            public void changed(ObservableValue<? extends Tab> ov, Tab from, Tab to) {
                if(from != null && from instanceof TabSelectionListener) {
                    ((TabSelectionListener) from).deselected();
                }
                if(to != null && to instanceof TabSelectionListener) {
                    ((TabSelectionListener) to).selected();
                }
            }
        });
        tabPane.setTabClosingPolicy(TabClosingPolicy.SELECTED_TAB);
        tabPane.getTabs().add(createTab("Featured", BASE_URI + "/"));
        tabPane.getTabs().add(createTab("Female", BASE_URI + "/female-cams/"));
        tabPane.getTabs().add(createTab("Male", BASE_URI + "/male-cams/"));
        tabPane.getTabs().add(createTab("Couples", BASE_URI + "/couple-cams/"));
        tabPane.getTabs().add(createTab("Trans", BASE_URI + "/trans-cams/"));
        FollowedTab tab = new FollowedTab("Followed", BASE_URI + "/followed-cams/");
        tab.setRecorder(recorder);
        tabPane.getTabs().add(tab);
        RecordedModelsTab modelsTab = new RecordedModelsTab("Recording", recorder);
        tabPane.getTabs().add(modelsTab);
        RecordingsTab recordingsTab = new RecordingsTab("Recordings", recorder, config);
        tabPane.getTabs().add(recordingsTab);
        settingsTab = new SettingsTab();
        tabPane.getTabs().add(settingsTab);
        tabPane.getTabs().add(new DonateTabFx());

        int windowWidth = Config.getInstance().getSettings().windowWidth;
        int windowHeight = Config.getInstance().getSettings().windowHeight;
        primaryStage.setScene(new Scene(tabPane, windowWidth, windowHeight));
        primaryStage.getScene().getStylesheets().add("/ctbrec/ui/ThumbCell.css");
        primaryStage.getScene().widthProperty().addListener((observable, oldVal, newVal) -> Config.getInstance().getSettings().windowWidth = newVal.intValue());
        primaryStage.getScene().heightProperty().addListener((observable, oldVal, newVal) -> Config.getInstance().getSettings().windowHeight = newVal.intValue());
        primaryStage.setMaximized(Config.getInstance().getSettings().windowMaximized);
        primaryStage.maximizedProperty().addListener((observable, oldVal, newVal) -> Config.getInstance().getSettings().windowMaximized = newVal.booleanValue());
        primaryStage.setX(Config.getInstance().getSettings().windowX);
        primaryStage.setY(Config.getInstance().getSettings().windowY);
        primaryStage.xProperty().addListener((observable, oldVal, newVal) -> Config.getInstance().getSettings().windowX = newVal.intValue());
        primaryStage.yProperty().addListener((observable, oldVal, newVal) -> Config.getInstance().getSettings().windowY = newVal.intValue());
        primaryStage.show();
        primaryStage.setOnCloseRequest((e) -> {
            e.consume();
            Alert shutdownInfo = new AutosizeAlert(Alert.AlertType.INFORMATION);
            shutdownInfo.setTitle("Shutdown");
            shutdownInfo.setContentText("Shutting down. Please wait a few seconds...");
            shutdownInfo.show();

            new Thread() {
                @Override
                public void run() {
                    settingsTab.saveConfig();
                    recorder.shutdown();
                    client.shutdown();
                    try {
                        Config.getInstance().save();
                        LOG.info("Shutdown complete. Goodbye!");
                        Platform.exit();
                        // This is needed, because OkHttp?! seems to block the shutdown with its writer threads. They are not daemon threads :(
                        System.exit(0);
                    } catch (IOException e1) {
                        Platform.runLater(() -> {
                            Alert alert = new AutosizeAlert(Alert.AlertType.ERROR);
                            alert.setTitle("Error saving settings");
                            alert.setContentText("Couldn't save settings: " + e1.getLocalizedMessage());
                            alert.showAndWait();
                            System.exit(1);
                        });
                    }
                }
            }.start();
        });
    }

    private void doInitialLogin() {
        if(config.getSettings().username != null && !config.getSettings().username.isEmpty()) {
            new Thread() {
                @Override
                public void run() {
                    if(!Objects.equals(System.getenv("CTBREC_DEV"), "1")) {
                        try {
                            client.login();
                        } catch (IOException e1) {
                            LOG.warn("Initial login failed" , e1);
                        }
                    }
                };
            }.start();
        }
    }

    private void createRecorder() {
        if(config.getSettings().localRecording) {
            recorder = new LocalRecorder(config);
        } else {
            recorder = new RemoteRecorder(config, client);
        }
    }

    private void loadConfig() {
        try {
            Config.init();
        } catch (Exception e) {
            Alert alert = new AutosizeAlert(Alert.AlertType.ERROR);
            alert.setTitle("Whoopsie");
            alert.setContentText("Couldn't load settings.");
            alert.showAndWait();
            System.exit(1);
        }
        config = Config.getInstance();
    }

    Tab createTab(String title, String url) {
        ThumbOverviewTab tab = new ThumbOverviewTab(title, url, false);
        tab.setRecorder(recorder);
        return tab;
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void checkForUpdates() {
        Thread updateCheck = new Thread(() -> {
            try {
                String url = "https://api.github.com/repos/0xboobface/ctbrec/releases";
                Request request = new Request.Builder().url(url).build();
                Response response = client.execute(request);
                if(response.isSuccessful()) {
                    Moshi moshi = new Moshi.Builder().build();
                    Type type = Types.newParameterizedType(List.class, Release.class);
                    JsonAdapter<List<Release>> adapter = moshi.adapter(type);
                    List<Release> releases = adapter.fromJson(response.body().source());
                    Release latest = releases.get(0);
                    Version latestVersion = latest.getVersion();
                    Version ctbrecVersion = getVersion();
                    if(latestVersion.compareTo(ctbrecVersion) > 0) {
                        LOG.debug("Update available {} < {}", ctbrecVersion, latestVersion);
                        Platform.runLater(() -> tabPane.getTabs().add(new UpdateTab(latest)));
                    } else {
                        LOG.debug("ctbrec is up-to-date {}", ctbrecVersion);
                    }
                }
                response.close();
            } catch (IOException e) {
                LOG.warn("Update check failed {}", e.getMessage());
            }

        });
        updateCheck.setName("Update Check");
        updateCheck.setDaemon(true);
        updateCheck.start();
    }

    private Version getVersion() throws IOException {
        if(Objects.equals(System.getenv("CTBREC_DEV"), "1")) {
            return Version.of("0.0.0-DEV");
        } else {
            try(InputStream is = getClass().getClassLoader().getResourceAsStream("version")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String versionString = reader.readLine();
                Version version = Version.of(versionString);
                return version;
            }
        }
    }

    static class Release {
        private String name;
        private String tag_name;
        private String html_url;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTagName() {
            return tag_name;
        }

        public void setTagName(String tagName) {
            this.tag_name = tagName;
        }

        public String getHtmlUrl() {
            return html_url;
        }

        public void setHtmlUrl(String htmlUrl) {
            this.html_url = htmlUrl;
        }

        public Version getVersion() {
            return Version.of(tag_name);
        }
    }
}
