package ctbrec.ui;


import static ctbrec.Model.State.*;
import static ctbrec.event.Event.Type.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.StringUtil;
import ctbrec.Version;
import ctbrec.event.Event;
import ctbrec.event.EventBusHolder;
import ctbrec.event.ModelStateChangedEvent;
import ctbrec.io.HttpClient;
import ctbrec.recorder.LocalRecorder;
import ctbrec.recorder.OnlineMonitor;
import ctbrec.recorder.Recorder;
import ctbrec.recorder.RemoteRecorder;
import ctbrec.sites.Site;
import ctbrec.sites.bonga.BongaCams;
import ctbrec.sites.cam4.Cam4;
import ctbrec.sites.camsoda.Camsoda;
import ctbrec.sites.chaturbate.Chaturbate;
import ctbrec.sites.mfc.MyFreeCams;
import ctbrec.ui.settings.SettingsTab;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import okhttp3.Request;
import okhttp3.Response;

public class CamrecApplication extends Application {

    static final transient Logger LOG = LoggerFactory.getLogger(CamrecApplication.class);

    private Stage primaryStage;
    private Config config;
    private Recorder recorder;
    private OnlineMonitor onlineMonitor;
    static HostServices hostServices;
    private SettingsTab settingsTab;
    private TabPane rootPane = new TabPane();
    private List<Site> sites = new ArrayList<>();
    public static HttpClient httpClient;
    public static String title;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        logEnvironment();
        registerAlertSystem();
        sites.add(new BongaCams());
        sites.add(new Cam4());
        sites.add(new Camsoda());
        sites.add(new Chaturbate());
        sites.add(new MyFreeCams());
        loadConfig();
        createHttpClient();
        hostServices = getHostServices();
        createRecorder();
        onlineMonitor = new OnlineMonitor(recorder);
        onlineMonitor.start();
        for (Site site : sites) {
            if(site.isEnabled()) {
                try {
                    site.setRecorder(recorder);
                    site.init();
                } catch(Exception e) {
                    LOG.error("Error while initializing site {}", site.getName(), e);
                }
            }
        }
        createGui(primaryStage);
        checkForUpdates();

    }

    private void logEnvironment() {
        LOG.debug("OS:\t{} {}", System.getProperty("os.name"), System.getProperty("os.version"));
        LOG.debug("Java:\t{} {} {}", System.getProperty("java.vendor"), System.getProperty("java.vm.name"), System.getProperty("java.version"));
        LOG.debug("JavaFX:\t{} ({})", System.getProperty("javafx.version"), System.getProperty("javafx.runtime.version"));
    }

    private void createGui(Stage primaryStage) throws IOException {
        LOG.debug("Creating GUI");
        CamrecApplication.title = "CTB Recorder " + getVersion();
        primaryStage.setTitle(title);
        InputStream icon = getClass().getResourceAsStream("/icon.png");
        primaryStage.getIcons().add(new Image(icon));
        int windowWidth = Config.getInstance().getSettings().windowWidth;
        int windowHeight = Config.getInstance().getSettings().windowHeight;

        rootPane = new TabPane();
        Scene scene = new Scene(rootPane, windowWidth, windowHeight);
        primaryStage.setScene(scene);
        for (Iterator<Site> iterator = sites.iterator(); iterator.hasNext();) {
            Site site = iterator.next();
            if(site.isEnabled()) {
                SiteTab siteTab = new SiteTab(site, scene);
                rootPane.getTabs().add(siteTab);
            }
        }

        RecordedModelsTab modelsTab = new RecordedModelsTab("Recording", recorder, sites);
        rootPane.getTabs().add(modelsTab);
        RecordingsTab recordingsTab = new RecordingsTab("Recordings", recorder, config, sites);
        rootPane.getTabs().add(recordingsTab);
        settingsTab = new SettingsTab(sites);
        rootPane.getTabs().add(settingsTab);
        rootPane.getTabs().add(new DonateTabFx());

        switchToStartTab();
        writeColorSchemeStyleSheet(primaryStage);
        Color base = Color.web(Config.getInstance().getSettings().colorBase);
        if(!base.equals(Color.WHITE)) {
            loadStyleSheet(primaryStage, "color.css");
        }
        loadStyleSheet(primaryStage, "style.css");
        primaryStage.getScene().getStylesheets().add("/ctbrec/ui/settings/ColorSettingsPane.css");
        primaryStage.getScene().getStylesheets().add("/ctbrec/ui/ThumbCell.css");
        primaryStage.getScene().getStylesheets().add("/ctbrec/ui/controls/SearchBox.css");
        primaryStage.getScene().getStylesheets().add("/ctbrec/ui/controls/Popover.css");
        primaryStage.getScene().widthProperty().addListener((observable, oldVal, newVal) -> Config.getInstance().getSettings().windowWidth = newVal.intValue());
        primaryStage.getScene().heightProperty()
        .addListener((observable, oldVal, newVal) -> Config.getInstance().getSettings().windowHeight = newVal.intValue());
        primaryStage.setMaximized(Config.getInstance().getSettings().windowMaximized);
        primaryStage.maximizedProperty()
        .addListener((observable, oldVal, newVal) -> Config.getInstance().getSettings().windowMaximized = newVal.booleanValue());
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
                    modelsTab.saveState();
                    recordingsTab.saveState();
                    settingsTab.saveConfig();
                    onlineMonitor.shutdown();
                    recorder.shutdown();
                    for (Site site : sites) {
                        if(site.isEnabled()) {
                            site.shutdown();
                        }
                    }
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

        // register changelistener to activate / deactivate tabs, when the user switches between them
        rootPane.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
            @Override
            public void changed(ObservableValue<? extends Tab> ov, Tab from, Tab to) {
                if (from != null && from instanceof TabSelectionListener) {
                    ((TabSelectionListener) from).deselected();
                }
                if (to != null && to instanceof TabSelectionListener) {
                    ((TabSelectionListener) to).selected();
                }
            }
        });
    }

    private void registerAlertSystem() {
        //            try {
        //                // don't register before 1 minute has passed, because directly after
        //                // the start of ctbrec, an event for every online model would be fired,
        //                // which is annoying as f
        //                Thread.sleep(TimeUnit.MINUTES.toMillis(1));
        //            } catch (InterruptedException e) {
        //                e.printStackTrace();
        //            }
        EventBusHolder.BUS.register(new Object() {
            @Subscribe
            public void modelEvent(Event e) {
                try {
                    if (e.getType() == MODEL_STATUS_CHANGED) {
                        ModelStateChangedEvent evt = (ModelStateChangedEvent) e;
                        Model model = evt.getModel();
                        if (evt.getNewState() == ONLINE && primaryStage != null && primaryStage.getTitle() != null) {
                            String header = "Model Online";
                            String msg = model.getDisplayName() + " is now online";
                            LOG.debug(msg);
                            //OS.notification(primaryStage.getTitle(), header, msg);
                        }
                    }
                } catch (Exception e1) {
                    LOG.error("Couldn't show notification", e1);
                }
            }
        });

        //        EventBusHolder.BUS.register(new Object() {
        //            URL url = CamrecApplication.class.getResource("/Oxygen-Im-Highlight-Msg.mp3");
        //            PlaySound playSound = new PlaySound(url);
        //            EventHandler reaction = new EventHandler(playSound);
        //            //            LogReaction reaction = new LogReaction();
        //            @Subscribe
        //            public void modelEvent(Event e) {
        //                reaction.reactToEvent(e);
        //            }
        //        });


        LOG.debug("Alert System registered");
    }

    private void writeColorSchemeStyleSheet(Stage primaryStage) {
        File colorCss = new File(Config.getInstance().getConfigDir(), "color.css");
        try(FileOutputStream fos = new FileOutputStream(colorCss)) {
            String content = ".root {\n" +
                    "    -fx-base: "+Config.getInstance().getSettings().colorBase+";\n" +
                    "    -fx-accent: "+Config.getInstance().getSettings().colorAccent+";\n" +
                    "    -fx-default-button: -fx-accent;\n" +
                    "    -fx-focus-color: -fx-accent;\n" +
                    "    -fx-control-inner-background-alt: derive(-fx-base, 95%);\n" +
                    "}";
            fos.write(content.getBytes("utf-8"));
        } catch(Exception e) {
            LOG.error("Couldn't write stylesheet for user defined color theme");
        }
    }

    private void loadStyleSheet(Stage primaryStage, String filename) {
        File css = new File(Config.getInstance().getConfigDir(), filename);
        if(css.exists() && css.isFile()) {
            primaryStage.getScene().getStylesheets().add(css.toURI().toString());
        }
    }

    private void switchToStartTab() {
        String startTab = Config.getInstance().getSettings().startTab;
        if(StringUtil.isNotBlank(startTab)) {
            for (Tab tab : rootPane.getTabs()) {
                if(Objects.equals(startTab, tab.getText())) {
                    rootPane.getSelectionModel().select(tab);
                    break;
                }
            }
        }
        if(rootPane.getSelectionModel().getSelectedItem() instanceof TabSelectionListener) {
            ((TabSelectionListener)rootPane.getSelectionModel().getSelectedItem()).selected();
        }
    }

    private void createRecorder() {
        if (config.getSettings().localRecording) {
            recorder = new LocalRecorder(config);
        } else {
            recorder = new RemoteRecorder(config, httpClient, sites);
        }
    }

    private void loadConfig() {
        try {
            Config.init(sites);
        } catch (Exception e) {
            LOG.error("Couldn't load settings", e);
            Alert alert = new AutosizeAlert(Alert.AlertType.ERROR);
            alert.setTitle("Whoopsie");
            alert.setContentText("Couldn't load settings. Falling back to defaults. A backup of your settings has been created.");
            alert.showAndWait();
        }
        config = Config.getInstance();
    }

    private void createHttpClient() {
        httpClient = new HttpClient("camrec") {
            @Override
            public boolean login() throws IOException {
                return false;
            }
        };
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void checkForUpdates() {
        Thread updateCheck = new Thread(() -> {
            try {
                String url = "https://api.github.com/repos/0xboobface/ctbrec/releases";
                Request request = new Request.Builder().url(url).build();
                Response response = httpClient.execute(request);
                if (response.isSuccessful()) {
                    Moshi moshi = new Moshi.Builder().build();
                    Type type = Types.newParameterizedType(List.class, Release.class);
                    JsonAdapter<List<Release>> adapter = moshi.adapter(type);
                    List<Release> releases = adapter.fromJson(response.body().source());
                    Release latest = releases.get(0);
                    Version latestVersion = latest.getVersion();
                    Version ctbrecVersion = getVersion();
                    if (latestVersion.compareTo(ctbrecVersion) > 0) {
                        LOG.debug("Update available {} < {}", ctbrecVersion, latestVersion);
                        Platform.runLater(() -> rootPane.getTabs().add(new UpdateTab(latest)));
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
        if (Objects.equals(System.getenv("CTBREC_DEV"), "1")) {
            return Version.of("0.0.0-DEV");
        } else {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("version")) {
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
