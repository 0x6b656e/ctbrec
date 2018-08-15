package ctbrec.ui;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.Hmac;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;;

public class SettingsTab extends Tab {

    private static final transient Logger LOG = LoggerFactory.getLogger(SettingsTab.class);

    private static final int CHECKBOX_MARGIN = 6;
    private TextField recordingsDirectory;
    private Button recordingsDirectoryButton;
    private TextField mergeDirectory;
    private TextField mediaPlayer;
    private TextField username;
    private TextField server;
    private TextField port;
    private CheckBox loadResolution;
    private CheckBox secureCommunication = new CheckBox();
    private CheckBox automerge = new CheckBox();
    private CheckBox automergeKeepSegments = new CheckBox();
    private CheckBox chooseStreamQuality = new CheckBox();
    private CheckBox autoRecordFollowed = new CheckBox();
    private CheckBox multiplePlayers = new CheckBox();
    private PasswordField password;
    private RadioButton recordLocal;
    private RadioButton recordRemote;
    private ToggleGroup recordLocation;

    private TitledPane ctb;
    private TitledPane mergePane;

    public SettingsTab() {
        setText("Settings");
        createGui();
        setClosable(false);
    }

    private void createGui() {
        GridPane mainLayout = createGridLayout();
        mainLayout.setHgap(15);
        mainLayout.setVgap(15);
        mainLayout.setPadding(new Insets(15));
        setContent(mainLayout);

        GridPane layout = createGridLayout();
        layout.add(new Label("Recordings Directory"), 0, 0);
        recordingsDirectory = new TextField(Config.getInstance().getSettings().recordingsDir);
        recordingsDirectory.focusedProperty().addListener(createRecordingsDirectoryFocusListener());
        recordingsDirectory.setPrefWidth(400);
        GridPane.setFillWidth(recordingsDirectory, true);
        GridPane.setHgrow(recordingsDirectory, Priority.ALWAYS);
        GridPane.setColumnSpan(recordingsDirectory, 2);
        layout.add(recordingsDirectory, 1, 0);
        recordingsDirectoryButton = createRecordingsBrowseButton();
        layout.add(recordingsDirectoryButton, 3, 0);

        layout.add(new Label("Player"), 0, 1);
        mediaPlayer = new TextField(Config.getInstance().getSettings().mediaPlayer);
        mediaPlayer.focusedProperty().addListener(createMpvFocusListener());
        GridPane.setFillWidth(mediaPlayer, true);
        GridPane.setHgrow(mediaPlayer, Priority.ALWAYS);
        GridPane.setColumnSpan(mediaPlayer, 2);
        layout.add(mediaPlayer, 1, 1);
        layout.add(createMpvBrowseButton(), 3, 1);

        Label l = new Label("Allow multiple players");
        layout.add(l, 0, 2);
        multiplePlayers.setSelected(!Config.getInstance().getSettings().singlePlayer);
        multiplePlayers.setOnAction((e) -> Config.getInstance().getSettings().singlePlayer = !multiplePlayers.isSelected());
        GridPane.setMargin(recordingsDirectory, new Insets(0, 0, 0, CHECKBOX_MARGIN));
        GridPane.setMargin(mediaPlayer, new Insets(0, 0, 0, CHECKBOX_MARGIN));
        GridPane.setMargin(l, new Insets(3, 0, 0, 0));
        GridPane.setMargin(multiplePlayers, new Insets(3, 0, 0, CHECKBOX_MARGIN));
        layout.add(multiplePlayers, 1, 2);

        TitledPane locations = new TitledPane("Locations", layout);
        locations.setCollapsible(false);
        mainLayout.add(locations, 0, 0);

        layout = createGridLayout();
        layout.add(new Label("Chaturbate User"), 0, 0);
        username = new TextField(Config.getInstance().getSettings().username);
        username.focusedProperty().addListener((e) -> Config.getInstance().getSettings().username = username.getText());
        GridPane.setFillWidth(username, true);
        GridPane.setHgrow(username, Priority.ALWAYS);
        GridPane.setColumnSpan(username, 2);
        layout.add(username, 1, 0);

        layout.add(new Label("Chaturbate Password"), 0, 1);
        password = new PasswordField();
        password.setText(Config.getInstance().getSettings().password);
        password.focusedProperty().addListener((e) -> {
            if(!password.getText().isEmpty()) {
                Config.getInstance().getSettings().password = password.getText();
            }
        });
        GridPane.setFillWidth(password, true);
        GridPane.setHgrow(password, Priority.ALWAYS);
        GridPane.setColumnSpan(password, 2);
        layout.add(password, 1, 1);

        l = new Label("Record all followed models");
        layout.add(l, 0, 2);
        autoRecordFollowed = new CheckBox();
        autoRecordFollowed.setSelected(Config.getInstance().getSettings().recordFollowed);
        autoRecordFollowed.setOnAction((e) -> {
            Config.getInstance().getSettings().recordFollowed = autoRecordFollowed.isSelected();
            showRestartRequired();
        });
        layout.add(autoRecordFollowed, 1, 2);
        Label warning = new Label("Don't do this, if you follow many models. You have been warned ;) !");
        warning.setTextFill(Color.RED);
        layout.add(warning, 2, 2);
        GridPane.setMargin(l, new Insets(3, 0, 0, 0));
        GridPane.setMargin(warning, new Insets(3, 0, 0, 0));
        GridPane.setMargin(autoRecordFollowed, new Insets(3, 0, 0, CHECKBOX_MARGIN));
        GridPane.setMargin(username, new Insets(0, 0, 0, CHECKBOX_MARGIN));
        GridPane.setMargin(password, new Insets(0, 0, 0, CHECKBOX_MARGIN));

        ctb = new TitledPane("Chaturbate", layout);
        ctb.setCollapsible(false);
        mainLayout.add(ctb, 0, 1);

        layout = createGridLayout();
        l = new Label("Display stream resolution in overview");
        layout.add(l, 0, 0);
        loadResolution = new CheckBox();
        loadResolution.setSelected(Config.getInstance().getSettings().determineResolution);
        loadResolution.setOnAction((e) -> {
            Config.getInstance().getSettings().determineResolution = loadResolution.isSelected();
            if(!loadResolution.isSelected()) {
                ThumbOverviewTab.queue.clear();
            }
        });
        //GridPane.setMargin(l, new Insets(CHECKBOX_MARGIN, 0, 0, 0));
        GridPane.setMargin(loadResolution, new Insets(0, 0, 0, CHECKBOX_MARGIN));
        layout.add(loadResolution, 1, 0);

        l = new Label("Manually select stream quality");
        layout.add(l, 0, 1);
        chooseStreamQuality.setSelected(Config.getInstance().getSettings().chooseStreamQuality);
        chooseStreamQuality.setOnAction((e) -> Config.getInstance().getSettings().chooseStreamQuality = chooseStreamQuality.isSelected());
        GridPane.setMargin(l, new Insets(CHECKBOX_MARGIN, 0, 0, 0));
        GridPane.setMargin(chooseStreamQuality, new Insets(CHECKBOX_MARGIN, 0, 0, CHECKBOX_MARGIN));
        layout.add(chooseStreamQuality, 1, 1);
        TitledPane quality = new TitledPane("Stream Quality", layout);
        quality.setCollapsible(false);
        mainLayout.add(quality, 0, 2);

        GridPane mergeLayout = createGridLayout();
        l = new Label("Auto-merge recordings");
        mergeLayout.add(l, 0, 0);
        automerge.setSelected(Config.getInstance().getSettings().automerge);
        automerge.setOnAction((e) -> Config.getInstance().getSettings().automerge = automerge.isSelected());
        GridPane.setMargin(automerge, new Insets(0, 0, 0, CHECKBOX_MARGIN));
        mergeLayout.add(automerge, 1, 0);

        l = new Label("Keep segments");
        mergeLayout.add(l, 0, 1);
        automergeKeepSegments.setSelected(Config.getInstance().getSettings().automergeKeepSegments);
        automergeKeepSegments.setOnAction((e) -> Config.getInstance().getSettings().automergeKeepSegments = automergeKeepSegments.isSelected());
        GridPane.setMargin(l, new Insets(CHECKBOX_MARGIN, 0, CHECKBOX_MARGIN, 0));
        GridPane.setMargin(automergeKeepSegments, new Insets(CHECKBOX_MARGIN, 0, CHECKBOX_MARGIN, CHECKBOX_MARGIN));
        mergeLayout.add(automergeKeepSegments, 1, 1);

        l = new Label("Move merged files to");
        mergeLayout.add(l, 0, 2);
        mergeDirectory = new TextField(Config.getInstance().getSettings().mergeDir);
        mergeDirectory.setOnAction((e) -> Config.getInstance().getSettings().mergeDir = mergeDirectory.getText());
        mergeDirectory.focusedProperty().addListener(createMergeDirectoryFocusListener());
        GridPane.setFillWidth(mergeDirectory, true);
        GridPane.setHgrow(mergeDirectory, Priority.ALWAYS);
        GridPane.setMargin(mergeDirectory, new Insets(0, 0, 0, CHECKBOX_MARGIN));
        mergeLayout.add(mergeDirectory, 1, 2);
        mergeLayout.add(createMergeDirButton(), 3, 2);

        mergePane = new TitledPane("Auto-merge", mergeLayout);
        mergePane.setCollapsible(false);
        mainLayout.add(mergePane, 0, 3);

        layout = createGridLayout();
        l = new Label("Record Location");
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
        });
        GridPane.setMargin(l, new Insets(0, 0, CHECKBOX_MARGIN, 0));
        GridPane.setMargin(recordLocal, new Insets(0, 0, CHECKBOX_MARGIN, 0));
        GridPane.setMargin(recordRemote, new Insets(0, 0, CHECKBOX_MARGIN, 0));

        layout.add(new Label("Server"), 0, 1);
        server = new TextField(Config.getInstance().getSettings().httpServer);
        server.focusedProperty().addListener((e) -> {
            if(!server.getText().isEmpty()) {
                Config.getInstance().getSettings().httpServer = server.getText();
            }
        });
        GridPane.setFillWidth(server, true);
        GridPane.setHgrow(server, Priority.ALWAYS);
        GridPane.setColumnSpan(server, 2);
        layout.add(server, 1, 1);

        layout.add(new Label("Port"), 0, 2);
        port = new TextField(Integer.toString(Config.getInstance().getSettings().httpPort));
        port.focusedProperty().addListener((e) -> {
            if(!port.getText().isEmpty()) {
                try {
                    Config.getInstance().getSettings().httpPort = Integer.parseInt(port.getText());
                    port.setBorder(Border.EMPTY);
                    port.setTooltip(null);
                } catch (NumberFormatException e1) {
                    port.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.DASHED, new CornerRadii(2), new BorderWidths(2))));
                    port.setTooltip(new Tooltip("Port has to be a number in the range 1 - 65536"));
                }
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
        GridPane.setMargin(l, new Insets(CHECKBOX_MARGIN, CHECKBOX_MARGIN, 0, 0));
        GridPane.setMargin(secureCommunication, new Insets(CHECKBOX_MARGIN, 0, 0, 0));
        layout.add(secureCommunication, 1, 3);

        TitledPane recordLocation = new TitledPane("Record Location", layout);
        recordLocation.setCollapsible(false);
        mainLayout.add(recordLocation, 0, 4);

        setRecordingMode(recordLocal.isSelected());
    }

    private void showRestartRequired() {
        Alert restart = new AutosizeAlert(AlertType.INFORMATION);
        restart.setTitle("Restart required");
        restart.setHeaderText("Restart required");
        restart.setContentText("Changes get applied after a restart of the application");
        restart.show();
    }

    private GridPane createGridLayout() {
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
        automerge.setDisable(!local);
        automergeKeepSegments.setDisable(!local);
        mergePane.setDisable(!local);
        ctb.setDisable(!local);
        recordingsDirectory.setDisable(!local);
        recordingsDirectoryButton.setDisable(!local);
    }

    private ChangeListener<? super Boolean> createRecordingsDirectoryFocusListener() {
        return new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) {
                if (newPropertyValue) {
                    recordingsDirectory.setBorder(Border.EMPTY);
                    recordingsDirectory.setTooltip(null);
                } else {
                    String input = recordingsDirectory.getText();
                    File newDir = new File(input);
                    setRecordingsDir(newDir);
                }
            }
        };
    }

    private ChangeListener<? super Boolean> createMergeDirectoryFocusListener() {
        return new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) {
                if (newPropertyValue) {
                    mergeDirectory.setBorder(Border.EMPTY);
                    mergeDirectory.setTooltip(null);
                } else {
                    String input = mergeDirectory.getText();
                    if(input.isEmpty()) {
                        Config.getInstance().getSettings().mergeDir = "";
                    } else {
                        File newDir = new File(input);
                        setMergeDir(newDir);
                    }
                }
            }
        };
    }

    private ChangeListener<? super Boolean> createMpvFocusListener() {
        return new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) {
                if (newPropertyValue) {
                    mediaPlayer.setBorder(Border.EMPTY);
                    mediaPlayer.setTooltip(null);
                } else {
                    String input = mediaPlayer.getText();
                    File program = new File(input);
                    setMpv(program);
                }
            }
        };
    }

    private void setMpv(File program) {
        String msg = validateProgram(program);
        if (msg != null) {
            mediaPlayer.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.DASHED, new CornerRadii(2), new BorderWidths(2))));
            mediaPlayer.setTooltip(new Tooltip(msg));
        } else {
            Config.getInstance().getSettings().mediaPlayer = mediaPlayer.getText();
        }
    }

    private String validateProgram(File program) {
        if (program == null || !program.exists()) {
            return "File does not exist";
        } else if (!program.isFile() || !program.canExecute()) {
            return "This is not an executable application";
        }
        return null;
    }

    private Button createRecordingsBrowseButton() {
        Button button = new Button("Select");
        button.setOnAction((e) -> {
            DirectoryChooser chooser = new DirectoryChooser();
            File currentDir = new File(Config.getInstance().getSettings().recordingsDir);
            if (currentDir.exists() && currentDir.isDirectory()) {
                chooser.setInitialDirectory(currentDir);
            }
            File selectedDir = chooser.showDialog(null);
            if(selectedDir != null) {
                setRecordingsDir(selectedDir);
            }
        });
        return button;
    }

    private Node createMergeDirButton() {
        Button button = new Button("Select");
        button.setOnAction((e) -> {
            DirectoryChooser chooser = new DirectoryChooser();
            File currentDir = new File(Config.getInstance().getSettings().mergeDir);
            if (currentDir.exists() && currentDir.isDirectory()) {
                chooser.setInitialDirectory(currentDir);
            }
            File selectedDir = chooser.showDialog(null);
            if(selectedDir != null) {
                setMergeDir(selectedDir);
            }
        });
        return button;
    }

    private Node createMpvBrowseButton() {
        Button button = new Button("Select");
        button.setOnAction((e) -> {
            FileChooser chooser = new FileChooser();
            File program = chooser.showOpenDialog(null);
            if(program != null) {
                try {
                    mediaPlayer.setText(program.getCanonicalPath());
                } catch (IOException e1) {
                    LOG.error("Couldn't determine path", e1);
                    Alert alert = new AutosizeAlert(Alert.AlertType.ERROR);
                    alert.setTitle("Whoopsie");
                    alert.setContentText("Couldn't determine path");
                    alert.showAndWait();
                }
                setMpv(program);
            }
        });
        return button;
    }

    private void setRecordingsDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            try {
                String path = dir.getCanonicalPath();
                Config.getInstance().getSettings().recordingsDir = path;
                recordingsDirectory.setText(path);
            } catch (IOException e1) {
                LOG.error("Couldn't determine directory path", e1);
                Alert alert = new AutosizeAlert(Alert.AlertType.ERROR);
                alert.setTitle("Whoopsie");
                alert.setContentText("Couldn't determine directory path");
                alert.showAndWait();
            }
        } else {
            recordingsDirectory.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.DASHED, new CornerRadii(2), new BorderWidths(2))));
            if (!dir.isDirectory()) {
                recordingsDirectory.setTooltip(new Tooltip("This is not a directory"));
            }
            if (!dir.exists()) {
                recordingsDirectory.setTooltip(new Tooltip("Directory does not exist"));
            }

        }
    }

    private void setMergeDir(File dir) {
        if (dir != null) {
            if (dir.isDirectory()) {
                try {
                    String path = dir.getCanonicalPath();
                    Config.getInstance().getSettings().mergeDir = path;
                    mergeDirectory.setText(path);
                } catch (IOException e1) {
                    LOG.error("Couldn't determine directory path", e1);
                    Alert alert = new AutosizeAlert(Alert.AlertType.ERROR);
                    alert.setTitle("Whoopsie");
                    alert.setContentText("Couldn't determine directory path");
                    alert.showAndWait();
                }
            } else {
                mergeDirectory.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.DASHED, new CornerRadii(2), new BorderWidths(2))));
                if (!dir.isDirectory()) {
                    mergeDirectory.setTooltip(new Tooltip("This is not a directory"));
                }
                if (!dir.exists()) {
                    mergeDirectory.setTooltip(new Tooltip("Directory does not exist"));
                }

            }
        }
    }
}
