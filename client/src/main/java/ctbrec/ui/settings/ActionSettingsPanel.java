package ctbrec.ui.settings;

import java.io.File;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.OS;
import ctbrec.Recording;
import ctbrec.StringUtil;
import ctbrec.event.Event;
import ctbrec.event.EventBusHolder;
import ctbrec.event.EventHandler;
import ctbrec.event.EventHandlerConfiguration;
import ctbrec.event.EventHandlerConfiguration.ActionConfiguration;
import ctbrec.event.EventHandlerConfiguration.PredicateConfiguration;
import ctbrec.event.ExecuteProgram;
import ctbrec.event.ModelPredicate;
import ctbrec.event.ModelStatePredicate;
import ctbrec.event.RecordingStatePredicate;
import ctbrec.recorder.Recorder;
import ctbrec.ui.CamrecApplication;
import ctbrec.ui.controls.FileSelectionBox;
import ctbrec.ui.controls.ProgramSelectionBox;
import ctbrec.ui.controls.Wizard;
import ctbrec.ui.event.PlaySound;
import ctbrec.ui.event.ShowNotification;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class ActionSettingsPanel extends TitledPane {
    private static final transient Logger LOG = LoggerFactory.getLogger(ActionSettingsPanel.class);
    private ListView<EventHandlerConfiguration> actionTable;

    private TextField name = new TextField();
    private ComboBox<Event.Type> event = new ComboBox<>();
    private ComboBox<Model.State> modelState = new ComboBox<>();
    private ComboBox<Recording.State> recordingState = new ComboBox<>();

    private CheckBox playSound = new CheckBox("Play sound");
    private FileSelectionBox sound = new FileSelectionBox();
    private CheckBox showNotification = new CheckBox("Notify me");
    private Button testNotification = new Button("Test");
    private CheckBox executeProgram = new CheckBox("Execute program");
    private ProgramSelectionBox program = new ProgramSelectionBox();
    private ListSelectionPane<Model> modelSelectionPane;

    private Recorder recorder;

    public ActionSettingsPanel(SettingsTab settingsTab, Recorder recorder) {
        this.recorder = recorder;
        setText("Events & Actions");
        setExpanded(true);
        setCollapsible(false);
        createGui();
        loadEventHandlers();
    }

    private void loadEventHandlers() {
        actionTable.getItems().addAll(Config.getInstance().getSettings().eventHandlers);
    }

    private void createGui() {
        BorderPane mainLayout = new BorderPane();
        setContent(mainLayout);

        actionTable = createActionTable();
        ScrollPane scrollPane = new ScrollPane(actionTable);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: -fx-background");
        mainLayout.setCenter(scrollPane);

        Button add = new Button("Add");
        add.setOnAction(this::add);
        Button delete = new Button("Delete");
        delete.setOnAction(this::delete);
        delete.setDisable(true);
        HBox buttons = new HBox(5, add, delete);
        mainLayout.setBottom(buttons);
        BorderPane.setMargin(buttons, new Insets(5, 0, 0, 0));

        actionTable.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<EventHandlerConfiguration>() {
            @Override
            public void onChanged(Change<? extends EventHandlerConfiguration> change) {
                delete.setDisable(change.getList().isEmpty());
            }
        });
    }

    private void add(ActionEvent evt) {
        Pane actionPane = createActionPane();
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(getScene().getWindow());
        dialog.setTitle("New Action");
        InputStream icon = getClass().getResourceAsStream("/icon.png");
        dialog.getIcons().add(new Image(icon));
        Wizard root = new Wizard(dialog, this::validateSettings, actionPane);
        Scene scene = new Scene(root, 800, 540);
        scene.getStylesheets().addAll(getScene().getStylesheets());
        dialog.setScene(scene);
        centerOnParent(dialog);
        dialog.showAndWait();
        if(!root.isCancelled()) {
            createEventHandler();
        }
    }

    private void createEventHandler() {
        EventHandlerConfiguration config = new EventHandlerConfiguration();
        config.setName(name.getText());
        config.setEvent(event.getValue());
        if(event.getValue() == Event.Type.MODEL_STATUS_CHANGED) {
            PredicateConfiguration pc = new PredicateConfiguration();
            pc.setType(ModelStatePredicate.class.getName());
            pc.getConfiguration().put("state", modelState.getValue().name());
            pc.setName("state = " + modelState.getValue().toString());
            config.getPredicates().add(pc);
        } else if(event.getValue() == Event.Type.RECORDING_STATUS_CHANGED) {
            PredicateConfiguration pc = new PredicateConfiguration();
            pc.setType(RecordingStatePredicate.class.getName());
            pc.getConfiguration().put("state", recordingState.getValue().name());
            pc.setName("state = " + recordingState.getValue().toString());
            config.getPredicates().add(pc);
        }
        if(!modelSelectionPane.isAllSelected()) {
            PredicateConfiguration pc = new PredicateConfiguration();
            pc.setType(ModelPredicate.class.getName());
            pc.setModels(modelSelectionPane.getSelectedItems());
            pc.setName("model is one of:" + modelSelectionPane.getSelectedItems());
            config.getPredicates().add(pc);
        }
        if(showNotification.isSelected()) {
            ActionConfiguration ac = new ActionConfiguration();
            ac.setType(ShowNotification.class.getName());
            ac.setName("show notification");
            config.getActions().add(ac);
        }
        if(playSound.isSelected()) {
            ActionConfiguration ac = new ActionConfiguration();
            ac.setType(PlaySound.class.getName());
            File file = new File(sound.fileProperty().get());
            ac.getConfiguration().put("file", file.getAbsolutePath());
            ac.setName("play " + file.getName());
            config.getActions().add(ac);
        }
        if(executeProgram.isSelected()) {
            ActionConfiguration ac = new ActionConfiguration();
            ac.setType(ExecuteProgram.class.getName());
            File file = new File(program.fileProperty().get());
            ac.getConfiguration().put("file", file.getAbsolutePath());
            ac.setName("execute " + file.getName());
            config.getActions().add(ac);
        }

        EventHandler handler = new EventHandler(config);
        EventBusHolder.register(handler);
        Config.getInstance().getSettings().eventHandlers.add(config);
        actionTable.getItems().add(config);
        LOG.debug("Registered event handler for {} {}", config.getEvent(), config.getName());
    }

    private void validateSettings() {
        if(StringUtil.isBlank(name.getText())) {
            throw new IllegalStateException("Name cannot be empty");
        }
        if(event.getValue() == Event.Type.MODEL_STATUS_CHANGED && modelState.getValue() == null) {
            throw new IllegalStateException("Select a state");
        }
        if(event.getValue() == Event.Type.RECORDING_STATUS_CHANGED && recordingState.getValue() == null) {
            throw new IllegalStateException("Select a state");
        }
        if(modelSelectionPane.getSelectedItems().isEmpty() && !modelSelectionPane.isAllSelected()) {
            throw new IllegalStateException("Select one or more models or tick off \"all\"");
        }
        if(!(showNotification.isSelected() || playSound.isSelected() || executeProgram.isSelected())) {
            throw new IllegalStateException("No action selected");
        }
    }

    private void delete(ActionEvent evt) {
        List<EventHandlerConfiguration> selected = new ArrayList<>(actionTable.getSelectionModel().getSelectedItems());
        for (EventHandlerConfiguration config : selected) {
            EventBusHolder.unregister(config.getId());
            Config.getInstance().getSettings().eventHandlers.remove(config);
            actionTable.getItems().remove(config);
        }
    }

    private Pane createActionPane() {
        GridPane layout = SettingsTab.createGridLayout();
        recordingState.prefWidthProperty().bind(event.widthProperty());
        modelState.prefWidthProperty().bind(event.widthProperty());
        name.prefWidthProperty().bind(event.widthProperty());

        int row = 0;
        layout.add(new Label("Name"), 0, row);
        layout.add(name, 1, row++);

        layout.add(new Label("Event"), 0, row);
        event.getItems().add(Event.Type.MODEL_STATUS_CHANGED);
        event.getItems().add(Event.Type.RECORDING_STATUS_CHANGED);
        event.setOnAction(evt -> {
            modelState.setVisible(event.getSelectionModel().getSelectedItem() == Event.Type.MODEL_STATUS_CHANGED);
        });
        event.getSelectionModel().select(Event.Type.MODEL_STATUS_CHANGED);
        layout.add(event, 1, row++);

        layout.add(new Label("State"), 0, row);
        modelState.getItems().clear();
        modelState.getItems().addAll(Model.State.values());
        layout.add(modelState, 1, row);
        recordingState.getItems().clear();
        recordingState.getItems().addAll(Recording.State.values());
        layout.add(recordingState, 1, row++);
        recordingState.visibleProperty().bind(modelState.visibleProperty().not());

        layout.add(createSeparator(), 0, row++);

        Label l = new Label("Models");
        layout.add(l, 0, row);
        modelSelectionPane = new ListSelectionPane<Model>(recorder.getModelsRecording(), Collections.emptyList());
        layout.add(modelSelectionPane, 1, row++);
        GridPane.setValignment(l, VPos.TOP);
        GridPane.setHgrow(modelSelectionPane, Priority.ALWAYS);
        GridPane.setFillWidth(modelSelectionPane, true);

        layout.add(createSeparator(), 0, row++);

        layout.add(showNotification, 0, row);
        layout.add(testNotification, 1, row++);
        testNotification.setOnAction(evt -> {
            DateTimeFormatter format = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);
            ZonedDateTime time = ZonedDateTime.now();
            OS.notification(CamrecApplication.title, "Test Notification", "Oi, what's up! " + format.format(time));
        });
        testNotification.disableProperty().bind(showNotification.selectedProperty().not());

        layout.add(playSound, 0, row);
        layout.add(sound, 1, row++);
        sound.disableProperty().bind(playSound.selectedProperty().not());

        layout.add(executeProgram, 0, row);
        layout.add(program, 1, row);
        program.disableProperty().bind(executeProgram.selectedProperty().not());

        GridPane.setFillWidth(name, true);
        GridPane.setHgrow(name, Priority.ALWAYS);
        GridPane.setFillWidth(sound, true);

        return layout;
    }

    private ListView<EventHandlerConfiguration> createActionTable() {
        ListView<EventHandlerConfiguration> view = new ListView<>();
        view.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        view.setPrefSize(300, 200);
        return view;
    }

    private Node createSeparator() {
        Separator divider = new Separator(Orientation.HORIZONTAL);
        GridPane.setHgrow(divider, Priority.ALWAYS);
        GridPane.setFillWidth(divider, true);
        GridPane.setColumnSpan(divider, 2);
        int tb = 20;
        int lr = 0;
        GridPane.setMargin(divider, new Insets(tb, lr, tb, lr));
        return divider;
    }

    private void centerOnParent(Stage dialog) {
        dialog.setWidth(dialog.getScene().getWidth());
        dialog.setHeight(dialog.getScene().getHeight());
        double w = dialog.getWidth();
        double h = dialog.getHeight();
        Window p = dialog.getOwner();
        double px = p.getX();
        double py = p.getY();
        double pw = p.getWidth();
        double ph = p.getHeight();
        dialog.setX(px + (pw - w) / 2);
        dialog.setY(py + (ph - h) / 2);
    }
}
