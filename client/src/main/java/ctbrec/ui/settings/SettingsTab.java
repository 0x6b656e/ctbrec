package ctbrec.ui.settings;

import static ctbrec.Settings.DirectoryStructure.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.Hmac;
import ctbrec.Settings.DirectoryStructure;
import ctbrec.StringUtil;
import ctbrec.recorder.Recorder;
import ctbrec.sites.ConfigUI;
import ctbrec.sites.Site;
import ctbrec.ui.SiteUiFactory;
import ctbrec.ui.TabSelectionListener;
import ctbrec.ui.controls.DirectorySelectionBox;
import ctbrec.ui.controls.ProgramSelectionBox;
import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;;

public class SettingsTab extends Tab implements TabSelectionListener {

    private static final transient Logger LOG = LoggerFactory.getLogger(SettingsTab.class);
    private static final int ONE_GiB_IN_BYTES = 1024 * 1024 * 1024;

    public static final int CHECKBOX_MARGIN = 6;
    private DirectorySelectionBox recordingsDirectory;
    private ProgramSelectionBox mediaPlayer;
    private ProgramSelectionBox postProcessing;
    private TextField server;
    private TextField port;
    private TextField onlineCheckIntervalInSecs;
    private TextField leaveSpaceOnDevice;
    private TextField minimumLengthInSecs;
    private CheckBox loadResolution;
    private CheckBox secureCommunication = new CheckBox();
    private CheckBox chooseStreamQuality = new CheckBox();
    private CheckBox multiplePlayers = new CheckBox();
    private CheckBox updateThumbnails = new CheckBox();
    private CheckBox previewInThumbnails = new CheckBox();
    private CheckBox showPlayerStarting = new CheckBox();
    private RadioButton recordLocal;
    private RadioButton recordRemote;
    private ToggleGroup recordLocation;
    private ProxySettingsPane proxySettingsPane;
    private ComboBox<Integer> maxResolution;
    private ComboBox<SplitAfterOption> splitAfter;
    private ComboBox<DirectoryStructure> directoryStructure;
    private ComboBox<String> startTab;
    private List<Site> sites;
    private Label restartLabel;
    private Accordion siteConfigAccordion = new Accordion();
    private Recorder recorder;

    public SettingsTab(List<Site> sites, Recorder recorder) {
        this.sites = sites;
        this.recorder = recorder;
        setText("Settings");
        createGui();
        setClosable(false);
        setRecordingMode(recordLocal.isSelected());
    }

    private void createGui() {
        // set up main layout, 2 columns with VBoxes 50/50
        GridPane mainLayout = createGridLayout();
        mainLayout.setHgap(15);
        mainLayout.setVgap(15);
        mainLayout.setPadding(new Insets(15));
        ColumnConstraints cc = new ColumnConstraints();
        cc.setPercentWidth(50);
        mainLayout.getColumnConstraints().setAll(cc, cc);
        ScrollPane scrollPane = new ScrollPane(mainLayout);
        setContent(scrollPane);
        GridPane.setFillHeight(scrollPane, true);
        GridPane.setFillWidth(scrollPane, true);
        GridPane.setHgrow(scrollPane, Priority.ALWAYS);
        GridPane.setVgrow(scrollPane, Priority.ALWAYS);
        VBox leftSide = new VBox(15);
        leftSide.setFillWidth(true);
        VBox rightSide = new VBox(15);
        rightSide.setFillWidth(true);
        GridPane.setHgrow(leftSide, Priority.ALWAYS);
        GridPane.setHgrow(rightSide, Priority.ALWAYS);
        GridPane.setFillWidth(leftSide, true);
        GridPane.setFillWidth(rightSide, true);
        mainLayout.add(leftSide, 0, 1);
        mainLayout.add(rightSide, 1, 1);
        mainLayout.prefWidthProperty().bind(scrollPane.widthProperty());

        // restart info label
        restartLabel = new Label("A restart is required to apply the changes you made!");
        restartLabel.setVisible(false);
        restartLabel.setFont(Font.font(24));
        restartLabel.setTextFill(Color.RED);
        mainLayout.add(restartLabel, 0, 0);
        GridPane.setColumnSpan(restartLabel, 2);
        GridPane.setHalignment(restartLabel, HPos.CENTER);

        // left side
        leftSide.getChildren().add(createGeneralPanel());
        leftSide.getChildren().add(createRecorderPanel());
        leftSide.getChildren().add(createRecordLocationPanel());

        //right side
        rightSide.getChildren().add(siteConfigAccordion);
        ActionSettingsPanel actions = new ActionSettingsPanel(this, recorder);
        rightSide.getChildren().add(actions);
        proxySettingsPane = new ProxySettingsPane(this);
        rightSide.getChildren().add(proxySettingsPane);
        for (int i = 0; i < sites.size(); i++) {
            Site site = sites.get(i);
            ConfigUI siteConfig = SiteUiFactory.getUi(site).getConfigUI();
            if(siteConfig != null) {
                TitledPane pane = new TitledPane(site.getName(), siteConfig.createConfigPanel());
                siteConfigAccordion.getPanes().add(pane);
            }
        }
    }

    private Node createRecordLocationPanel() {
        GridPane layout = createGridLayout();
        Label l = new Label("Record Location");
        layout.add(l, 0, 0);
        recordLocation = new ToggleGroup();
        recordLocal = new RadioButton("Local");
        recordRemote = new RadioButton("Remote");
        recordLocal.setToggleGroup(recordLocation);
        recordRemote.setToggleGroup(recordLocation);
        recordLocal.setSelected(Config.getInstance().getSettings().localRecording);
        recordRemote.setSelected(!recordLocal.isSelected());
        layout.add(recordLocal, 1, 0);
        layout.add(recordRemote, 2, 0);
        recordLocation.selectedToggleProperty().addListener((e) -> {
            Config.getInstance().getSettings().localRecording = recordLocal.isSelected();
            setRecordingMode(recordLocal.isSelected());
            showRestartRequired();
            saveConfig();
        });
        GridPane.setMargin(l, new Insets(0, 0, CHECKBOX_MARGIN, 0));
        GridPane.setMargin(recordLocal, new Insets(0, 0, CHECKBOX_MARGIN, 0));
        GridPane.setMargin(recordRemote, new Insets(0, 0, CHECKBOX_MARGIN, 0));

        layout.add(new Label("Server"), 0, 1);
        server = new TextField(Config.getInstance().getSettings().httpServer);
        server.textProperty().addListener((ob, o, n) -> {
            if(!server.getText().isEmpty()) {
                Config.getInstance().getSettings().httpServer = server.getText();
                saveConfig();
            }
        });
        GridPane.setFillWidth(server, true);
        GridPane.setHgrow(server, Priority.ALWAYS);
        GridPane.setColumnSpan(server, 2);
        layout.add(server, 1, 1);

        layout.add(new Label("Port"), 0, 2);
        port = new TextField(Integer.toString(Config.getInstance().getSettings().httpPort));
        port.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                port.setText(newValue.replaceAll("[^\\d]", ""));
            }
            if(!port.getText().isEmpty()) {
                Config.getInstance().getSettings().httpPort = Integer.parseInt(port.getText());
                saveConfig();
            }
        });
        GridPane.setFillWidth(port, true);
        GridPane.setHgrow(port, Priority.ALWAYS);
        GridPane.setColumnSpan(port, 2);
        layout.add(port, 1, 2);

        l = new Label("Require authentication");
        layout.add(l, 0, 3);
        secureCommunication.setSelected(Config.getInstance().getSettings().requireAuthentication);
        secureCommunication.setOnAction((e) -> {
            Config.getInstance().getSettings().requireAuthentication = secureCommunication.isSelected();
            if(secureCommunication.isSelected()) {
                byte[] key = Config.getInstance().getSettings().key;
                if(key == null) {
                    key = Hmac.generateKey();
                    Config.getInstance().getSettings().key = key;
                    saveConfig();
                }
                TextInputDialog keyDialog = new TextInputDialog();
                keyDialog.setResizable(true);
                keyDialog.setTitle("Server Authentication");
                keyDialog.setHeaderText("A key has been generated");
                keyDialog.setContentText("Add this setting to your server's config.json:\n");
                keyDialog.getEditor().setText("\"key\": " + Arrays.toString(key));
                keyDialog.getEditor().setEditable(false);
                keyDialog.setWidth(800);
                keyDialog.setHeight(200);
                keyDialog.show();
            }
        });
        GridPane.setMargin(l, new Insets(4, CHECKBOX_MARGIN, 0, 0));
        GridPane.setMargin(secureCommunication, new Insets(4, 0, 0, 0));
        layout.add(secureCommunication, 1, 3);

        TitledPane recordLocation = new TitledPane("Record Location", layout);
        recordLocation.setCollapsible(false);
        return recordLocation;
    }

    private Node createRecorderPanel() {
        int row = 0;
        GridPane layout = createGridLayout();

        layout.add(new Label("Recordings Directory"), 0, row);
        recordingsDirectory = new DirectorySelectionBox(Config.getInstance().getSettings().recordingsDir);
        recordingsDirectory.prefWidth(400);
        recordingsDirectory.fileProperty().addListener((obs, o, n) -> {
            String path = n.getAbsolutePath();
            if(!Objects.equals(path, Config.getInstance().getSettings().recordingsDir)) {
                Config.getInstance().getSettings().recordingsDir = path;
                saveConfig();
            }
        });
        GridPane.setFillWidth(recordingsDirectory, true);
        GridPane.setHgrow(recordingsDirectory, Priority.ALWAYS);
        GridPane.setMargin(recordingsDirectory, new Insets(0, 0, 0, CHECKBOX_MARGIN));
        layout.add(recordingsDirectory, 1, row++);

        layout.add(new Label("Directory Structure"), 0, row);
        List<DirectoryStructure> options = new ArrayList<>();
        options.add(FLAT);
        options.add(ONE_PER_MODEL);
        options.add(ONE_PER_RECORDING);
        directoryStructure = new ComboBox<>(FXCollections.observableList(options));
        directoryStructure.setValue(Config.getInstance().getSettings().recordingsDirStructure);
        directoryStructure.setOnAction((evt) -> {
            Config.getInstance().getSettings().recordingsDirStructure = directoryStructure.getValue();
            saveConfig();
        });
        GridPane.setMargin(directoryStructure, new Insets(0, 0, 0, CHECKBOX_MARGIN));
        layout.add(directoryStructure, 1, row++);
        recordingsDirectory.prefWidthProperty().bind(directoryStructure.widthProperty());

        Label l = new Label("Maximum resolution (0 = unlimited)");
        layout.add(l, 0, row);
        List<Integer> resolutionOptions = new ArrayList<>();
        resolutionOptions.add(1080);
        resolutionOptions.add(720);
        resolutionOptions.add(600);
        resolutionOptions.add(480);
        resolutionOptions.add(0);
        maxResolution = new ComboBox<>(FXCollections.observableList(resolutionOptions));
        setMaxResolutionValue();
        maxResolution.setOnAction((e) -> {
            Config.getInstance().getSettings().maximumResolution = maxResolution.getSelectionModel().getSelectedItem();
            saveConfig();
        });
        maxResolution.prefWidthProperty().bind(directoryStructure.widthProperty());
        layout.add(maxResolution, 1, row++);
        GridPane.setMargin(l, new Insets(0, 0, 0, 0));
        GridPane.setMargin(maxResolution, new Insets(0, 0, 0, CHECKBOX_MARGIN));

        l = new Label("Split recordings after (minutes)");
        layout.add(l, 0, row);
        List<SplitAfterOption> splitOptions = new ArrayList<>();
        splitOptions.add(new SplitAfterOption("disabled", 0));
        if(Config.isDevMode()) {
            splitOptions.add(new SplitAfterOption( "1 min",  1 * 60));
            splitOptions.add(new SplitAfterOption( "3 min",  3 * 60));
            splitOptions.add(new SplitAfterOption( "5 min",  5 * 60));
        }
        splitOptions.add(new SplitAfterOption("10 min", 10 * 60));
        splitOptions.add(new SplitAfterOption("15 min", 15 * 60));
        splitOptions.add(new SplitAfterOption("20 min", 20 * 60));
        splitOptions.add(new SplitAfterOption("30 min", 30 * 60));
        splitOptions.add(new SplitAfterOption("60 min", 60 * 60));
        splitAfter = new ComboBox<>(FXCollections.observableList(splitOptions));
        layout.add(splitAfter, 1, row++);
        setSplitAfterValue();
        splitAfter.setOnAction((e) -> {
            Config.getInstance().getSettings().splitRecordings = splitAfter.getSelectionModel().getSelectedItem().getValue();
            saveConfig();
        });
        splitAfter.prefWidthProperty().bind(directoryStructure.widthProperty());
        GridPane.setMargin(l, new Insets(0, 0, 0, 0));
        GridPane.setMargin(splitAfter, new Insets(0, 0, 0, CHECKBOX_MARGIN));

        layout.add(new Label("Post-Processing"), 0, row);
        // TODO allow empty strings to remove post-processing scripts
        postProcessing = new ProgramSelectionBox(Config.getInstance().getSettings().postProcessing);
        postProcessing.fileProperty().addListener((obs, o, n) -> {
            String path = n.getAbsolutePath();
            if(!Objects.equals(path, Config.getInstance().getSettings().postProcessing)) {
                Config.getInstance().getSettings().postProcessing = path;
                saveConfig();
            }
        });
        GridPane.setFillWidth(postProcessing, true);
        GridPane.setHgrow(postProcessing, Priority.ALWAYS);
        GridPane.setMargin(postProcessing, new Insets(0, 0, 0, CHECKBOX_MARGIN));
        layout.add(postProcessing, 1, row++);

        Tooltip tt = new Tooltip("Check every x seconds, if a model came online");
        l = new Label("Check online state every (seconds)");
        l.setTooltip(tt);
        layout.add(l, 0, row);
        onlineCheckIntervalInSecs = new TextField(Integer.toString(Config.getInstance().getSettings().onlineCheckIntervalInSecs));
        onlineCheckIntervalInSecs.setTooltip(tt);
        onlineCheckIntervalInSecs.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                onlineCheckIntervalInSecs.setText(newValue.replaceAll("[^\\d]", ""));
            }
            if(!onlineCheckIntervalInSecs.getText().isEmpty()) {
                Config.getInstance().getSettings().onlineCheckIntervalInSecs = Integer.parseInt(onlineCheckIntervalInSecs.getText());
                saveConfig();
            }
        });
        GridPane.setMargin(onlineCheckIntervalInSecs, new Insets(0, 0, 0, CHECKBOX_MARGIN));
        layout.add(onlineCheckIntervalInSecs, 1, row++);

        tt = new Tooltip("Stop recording, if the free space on the device gets below this threshold");
        l = new Label("Leave space on device (GiB)");
        l.setTooltip(tt);
        layout.add(l, 0, row);
        long minimumSpaceLeftInBytes = Config.getInstance().getSettings().minimumSpaceLeftInBytes;
        int minimumSpaceLeftInGiB = (int) (minimumSpaceLeftInBytes / ONE_GiB_IN_BYTES);
        leaveSpaceOnDevice = new TextField(Integer.toString(minimumSpaceLeftInGiB));
        leaveSpaceOnDevice.setTooltip(tt);
        leaveSpaceOnDevice.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                leaveSpaceOnDevice.setText(newValue.replaceAll("[^\\d]", ""));
            }
            if(!leaveSpaceOnDevice.getText().isEmpty()) {
                long spaceLeftInGiB = Long.parseLong(leaveSpaceOnDevice.getText());
                Config.getInstance().getSettings().minimumSpaceLeftInBytes = spaceLeftInGiB * ONE_GiB_IN_BYTES;
                saveConfig();
            }
        });
        GridPane.setMargin(leaveSpaceOnDevice, new Insets(0, 0, 0, CHECKBOX_MARGIN));
        layout.add(leaveSpaceOnDevice, 1, row++);

        tt = new Tooltip("Delete recordings, which are shorter than x seconds. 0 to disable.");
        l = new Label("Delete recordings shorter than (secs)");
        l.setTooltip(tt);
        layout.add(l, 0, row);
        int minimumLengthInSeconds = Config.getInstance().getSettings().minimumLengthInSeconds;
        minimumLengthInSecs = new TextField(Integer.toString(minimumLengthInSeconds));
        minimumLengthInSecs.setTooltip(tt);
        minimumLengthInSecs.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                minimumLengthInSecs.setText(newValue.replaceAll("[^\\d]", ""));
            }
            if(!minimumLengthInSecs.getText().isEmpty()) {
                int minimumLength = Integer.parseInt(minimumLengthInSecs.getText());
                Config.getInstance().getSettings().minimumLengthInSeconds = minimumLength;
                saveConfig();
            }
        });
        GridPane.setMargin(minimumLengthInSecs, new Insets(0, 0, 0, CHECKBOX_MARGIN));
        layout.add(minimumLengthInSecs, 1, row++);

        TitledPane locations = new TitledPane("Recorder", layout);
        locations.setCollapsible(false);
        return locations;
    }

    private Node createGeneralPanel() {
        GridPane layout = createGridLayout();
        int row = 0;

        layout.add(new Label("Player"), 0, row);
        mediaPlayer = new ProgramSelectionBox(Config.getInstance().getSettings().mediaPlayer);
        mediaPlayer.fileProperty().addListener((obs, o, n) -> {
            String path = n.getAbsolutePath();
            if (!Objects.equals(path, Config.getInstance().getSettings().mediaPlayer)) {
                Config.getInstance().getSettings().mediaPlayer = path;
                saveConfig();
            }
        });
        GridPane.setFillWidth(mediaPlayer, true);
        GridPane.setHgrow(mediaPlayer, Priority.ALWAYS);
        GridPane.setMargin(mediaPlayer, new Insets(0, 0, 0, CHECKBOX_MARGIN));
        layout.add(mediaPlayer, 1, row++);

        Label l = new Label("Allow multiple players");
        layout.add(l, 0, row);
        multiplePlayers.setSelected(!Config.getInstance().getSettings().singlePlayer);
        multiplePlayers.setOnAction((e) -> {
            Config.getInstance().getSettings().singlePlayer = !multiplePlayers.isSelected();
            saveConfig();
        });
        GridPane.setMargin(l, new Insets(3, 0, 0, 0));
        GridPane.setMargin(multiplePlayers, new Insets(CHECKBOX_MARGIN, 0, 0, CHECKBOX_MARGIN));
        layout.add(multiplePlayers, 1, row++);

        l = new Label("Show \"Player Starting\" Message");
        layout.add(l, 0, row);
        showPlayerStarting.setSelected(Config.getInstance().getSettings().showPlayerStarting);
        showPlayerStarting.setOnAction((e) -> {
            Config.getInstance().getSettings().showPlayerStarting = showPlayerStarting.isSelected();
            saveConfig();
        });
        GridPane.setMargin(l, new Insets(3, 0, 0, 0));
        GridPane.setMargin(showPlayerStarting, new Insets(CHECKBOX_MARGIN, 0, 0, CHECKBOX_MARGIN));
        layout.add(showPlayerStarting, 1, row++);

        l = new Label("Display stream resolution in overview");
        layout.add(l, 0, row);
        loadResolution = new CheckBox();
        loadResolution.setSelected(Config.getInstance().getSettings().determineResolution);
        loadResolution.setOnAction((e) -> {
            Config.getInstance().getSettings().determineResolution = loadResolution.isSelected();
            saveConfig();
        });
        GridPane.setMargin(l, new Insets(3, 0, 0, 0));
        GridPane.setMargin(loadResolution, new Insets(CHECKBOX_MARGIN, 0, 0, CHECKBOX_MARGIN));
        layout.add(loadResolution, 1, row++);

        l = new Label("Manually select stream quality");
        layout.add(l, 0, row);
        chooseStreamQuality.setSelected(Config.getInstance().getSettings().chooseStreamQuality);
        chooseStreamQuality.setOnAction((e) -> {
            Config.getInstance().getSettings().chooseStreamQuality = chooseStreamQuality.isSelected();
            saveConfig();
        });
        GridPane.setMargin(l, new Insets(3, 0, 0, 0));
        GridPane.setMargin(chooseStreamQuality, new Insets(CHECKBOX_MARGIN, 0, 0, CHECKBOX_MARGIN));
        layout.add(chooseStreamQuality, 1, row++);

        l = new Label("Update thumbnails");
        layout.add(l, 0, row);
        updateThumbnails.setSelected(Config.getInstance().getSettings().updateThumbnails);
        updateThumbnails.setOnAction((e) -> {
            Config.getInstance().getSettings().updateThumbnails = updateThumbnails.isSelected();
            saveConfig();
        });
        GridPane.setMargin(l, new Insets(3, 0, 0, 0));
        GridPane.setMargin(updateThumbnails, new Insets(CHECKBOX_MARGIN, 0, 0, CHECKBOX_MARGIN));
        layout.add(updateThumbnails, 1, row++);

        l = new Label("Preview in thumbnails");
        layout.add(l, 0, row);
        previewInThumbnails.setSelected(Config.getInstance().getSettings().previewInThumbnails);
        previewInThumbnails.setOnAction((e) -> {
            Config.getInstance().getSettings().previewInThumbnails = previewInThumbnails.isSelected();
            saveConfig();
        });
        GridPane.setMargin(l, new Insets(3, 0, 0, 0));
        GridPane.setMargin(previewInThumbnails, new Insets(CHECKBOX_MARGIN, 0, 0, CHECKBOX_MARGIN));
        layout.add(previewInThumbnails, 1, row++);

        l = new Label("Start Tab");
        layout.add(l, 0, row);
        startTab = new ComboBox<>();
        startTab.setOnAction((e) -> {
            Config.getInstance().getSettings().startTab = startTab.getSelectionModel().getSelectedItem();
            saveConfig();
        });
        layout.add(startTab, 1, row++);
        GridPane.setMargin(l, new Insets(3, 0, 0, 0));
        GridPane.setMargin(startTab, new Insets(CHECKBOX_MARGIN, 0, 0, CHECKBOX_MARGIN));

        l = new Label("Colors (Base / Accent)");
        layout.add(l, 0, row);
        ColorSettingsPane colorSettingsPane = new ColorSettingsPane(this);
        layout.add(colorSettingsPane, 1, row++);
        GridPane.setMargin(l, new Insets(0, 0, 0, 0));
        GridPane.setMargin(colorSettingsPane, new Insets(CHECKBOX_MARGIN, 0, 0, CHECKBOX_MARGIN));

        TitledPane general = new TitledPane("General", layout);
        general.setCollapsible(false);
        return general;
    }

    private void setSplitAfterValue() {
        int value = Config.getInstance().getSettings().splitRecordings;
        for (SplitAfterOption option : splitAfter.getItems()) {
            if(option.getValue() == value) {
                splitAfter.getSelectionModel().select(option);
            }
        }
    }

    private void setMaxResolutionValue() {
        int value = Config.getInstance().getSettings().maximumResolution;
        for (Integer option : maxResolution.getItems()) {
            if(option == value) {
                maxResolution.getSelectionModel().select(option);
            }
        }
    }

    void showRestartRequired() {
        restartLabel.setVisible(true);
    }

    public static GridPane createGridLayout() {
        GridPane layout = new GridPane();
        layout.setPadding(new Insets(10));
        layout.setHgap(5);
        layout.setVgap(5);
        return layout;
    }

    private void setRecordingMode(boolean local) {
        server.setDisable(local);
        port.setDisable(local);
        secureCommunication.setDisable(local);
        recordingsDirectory.setDisable(!local);
        splitAfter.setDisable(!local);
        maxResolution.setDisable(!local);
        directoryStructure.setDisable(!local);
        onlineCheckIntervalInSecs.setDisable(!local);
        leaveSpaceOnDevice.setDisable(!local);
        postProcessing.setDisable(!local);
    }

    @Override
    public void selected() {
        if(startTab.getItems().isEmpty()) {
            for(Tab tab : getTabPane().getTabs()) {
                startTab.getItems().add(tab.getText());
            }
        }
        String startTabName = Config.getInstance().getSettings().startTab;
        if(StringUtil.isNotBlank(startTabName)) {
            startTab.getSelectionModel().select(startTabName);
        }
    }

    @Override
    public void deselected() {
        saveConfig();
    }

    public void saveConfig() {
        if(proxySettingsPane != null) {
            proxySettingsPane.saveConfig();
        }
        try {
            Config.getInstance().save();
        } catch (IOException e) {
            LOG.error("Couldn't save config", e);
        }
    }

    public static class SplitAfterOption {
        private String label;
        private int value;

        public SplitAfterOption(String label, int value) {
            super();
            this.label = label;
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
