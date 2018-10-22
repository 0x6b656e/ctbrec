package ctbrec.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import ctbrec.Config;
import ctbrec.Version;
import ctbrec.recorder.LocalRecorder;
import ctbrec.recorder.Recorder;
import ctbrec.recorder.RemoteRecorder;
import ctbrec.sites.Site;
import ctbrec.sites.chaturbate.Chaturbate;
import ctbrec.sites.mfc.MyFreeCams;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import okhttp3.Request;
import okhttp3.Response;

public class CamrecApplication extends Application {

    static final transient Logger LOG = LoggerFactory.getLogger(CamrecApplication.class);

    private Config config;
    private Recorder recorder;
    static HostServices hostServices;
    private SettingsTab settingsTab;
    private TabPane tabPane = new TabPane();
    private TabPane rootPane = new TabPane();
    static EventBus bus;
    private HBox tokenPanel;
    private Site site;
    private List<Site> sites = new ArrayList<>();

    @Override
    public void start(Stage primaryStage) throws Exception {
        site = new MyFreeCams();
        sites.add(site);
        Chaturbate ctb = new Chaturbate();
        sites.add(ctb);
        loadConfig();
        bus = new AsyncEventBus(Executors.newSingleThreadExecutor());
        hostServices = getHostServices();
        createRecorder();
        for (Site site : sites) {
            site.setRecorder(recorder);
            site.init();
        }
        if (!Objects.equals(System.getenv("CTBREC_DEV"), "1")) {
            site.login();
        }
        createGui(primaryStage);
        checkForUpdates();
    }

    private void createGui(Stage primaryStage) throws IOException {
        LOG.debug("Creating GUI");
        primaryStage.setTitle("CTB Recorder " + getVersion());
        InputStream icon = getClass().getResourceAsStream("/icon.png");
        primaryStage.getIcons().add(new Image(icon));
        int windowWidth = Config.getInstance().getSettings().windowWidth;
        int windowHeight = Config.getInstance().getSettings().windowHeight;

        rootPane = new TabPane();
        Scene scene = new Scene(rootPane, windowWidth, windowHeight);
        primaryStage.setScene(scene);
        for (Site site : sites) {
            SiteTabPane siteTabPane = new SiteTabPane(site, scene);
            Tab siteTab = new Tab(site.getName());
            siteTab.setClosable(false);
            siteTab.setContent(siteTabPane);
            rootPane.getTabs().add(siteTab);
        }

        RecordedModelsTab modelsTab = new RecordedModelsTab("Recording", recorder, site);
        rootPane.getTabs().add(modelsTab);
        RecordingsTab recordingsTab = new RecordingsTab("Recordings", recorder, config, site);
        rootPane.getTabs().add(recordingsTab);
        settingsTab = new SettingsTab();
        rootPane.getTabs().add(settingsTab);
        rootPane.getTabs().add(new DonateTabFx());

        primaryStage.getScene().getStylesheets().add("/ctbrec/ui/ThumbCell.css");
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
                    settingsTab.saveConfig();
                    recorder.shutdown();
                    site.shutdown();
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

        String username = Config.getInstance().getSettings().username;
        if (site.supportsTips() && username != null && !username.trim().isEmpty()) {
            double fontSize = tabPane.getTabMaxHeight() / 2 - 1;
            Button buyTokens = new Button("Buy Tokens");
            buyTokens.setFont(Font.font(fontSize));
            buyTokens.setOnAction((e) -> DesktopIntergation.open(site.getBuyTokensLink()));
            buyTokens.setMaxHeight(tabPane.getTabMaxHeight());
            TokenLabel tokenBalance = new TokenLabel(site);
            tokenPanel = new HBox(5, tokenBalance, buyTokens);
            tokenPanel.setAlignment(Pos.BASELINE_RIGHT);
            tokenPanel.setMaxHeight(tabPane.getTabMaxHeight());
            tokenPanel.setMaxWidth(200);
            tokenBalance.setFont(Font.font(fontSize));
            HBox.setMargin(tokenBalance, new Insets(0, 5, 0, 0));
            HBox.setMargin(buyTokens, new Insets(0, 5, 0, 0));
            for (Node node : tabPane.getChildrenUnmodifiable()) {
                if (node.getStyleClass().contains("tab-header-area")) {
                    Parent header = (Parent) node;
                    for (Node nd : header.getChildrenUnmodifiable()) {
                        if (nd.getStyleClass().contains("tab-header-background")) {
                            StackPane pane = (StackPane) nd;
                            StackPane.setAlignment(tokenPanel, Pos.CENTER_RIGHT);
                            pane.getChildren().add(tokenPanel);
                        }
                    }

                }
            }
            tokenBalance.loadBalance();
        }
    }

    private void createRecorder() {
        if (config.getSettings().localRecording) {
            recorder = new LocalRecorder(config);
        } else {
            recorder = new RemoteRecorder(config, site.getHttpClient());
        }
    }

    private void loadConfig() {
        try {
            Config.init(sites);
        } catch (Exception e) {
            LOG.error("Couldn't load settings", e);
            Alert alert = new AutosizeAlert(Alert.AlertType.ERROR);
            alert.setTitle("Whoopsie");
            alert.setContentText("Couldn't load settings.");
            alert.showAndWait();
            System.exit(1);
        }
        config = Config.getInstance();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void checkForUpdates() {
        Thread updateCheck = new Thread(() -> {
            try {
                String url = "https://api.github.com/repos/0xboobface/ctbrec/releases";
                Request request = new Request.Builder().url(url).build();
                Response response = site.getHttpClient().execute(request);
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
