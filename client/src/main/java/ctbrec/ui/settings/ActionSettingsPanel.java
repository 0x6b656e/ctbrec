package ctbrec.ui.settings;

import java.io.InputStream;

import ctbrec.event.EventHandlerConfiguration;
import ctbrec.ui.controls.Wizard;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class ActionSettingsPanel extends TitledPane {

    private TableView actionTable;

    public ActionSettingsPanel(SettingsTab settingsTab) {
        setText("Events & Actions");
        setExpanded(true);
        setCollapsible(false);
        createGui();
    }

    private void createGui() {
        BorderPane mainLayout = new BorderPane();
        setContent(mainLayout);

        actionTable = createActionTable();
        actionTable.setPrefSize(300, 200);
        ScrollPane scrollPane = new ScrollPane(actionTable);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);
        scrollPane.setBorder(Border.EMPTY);
        mainLayout.setCenter(scrollPane);
        BorderPane.setMargin(scrollPane, new Insets(5));

        Button add = new Button("Add");
        add.setOnAction(this::add);
        Button delete = new Button("Delete");
        delete.setOnAction(this::delete);
        delete.setDisable(true);
        HBox buttons = new HBox(10, add, delete);
        mainLayout.setBottom(buttons);
        BorderPane.setMargin(buttons, new Insets(5));
    }

    private void add(ActionEvent evt) {
        EventHandlerConfiguration config = new EventHandlerConfiguration();
        Pane namePane = createNamePane(config);
        GridPane pane2 = SettingsTab.createGridLayout();
        pane2.add(new Label("Pane 2"), 0, 0);
        GridPane pane3 = SettingsTab.createGridLayout();
        pane3.add(new Label("Pane 3"), 0, 0);
        Stage dialog = new Stage();
        dialog.setTitle("New Action");
        InputStream icon = getClass().getResourceAsStream("/icon.png");
        dialog.getIcons().add(new Image(icon));
        Wizard root = new Wizard(dialog, namePane, pane2, pane3);
        Scene scene = new Scene(root, 640, 480);
        scene.getStylesheets().addAll(getScene().getStylesheets());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void delete(ActionEvent evt) {

    }

    private Pane createNamePane(EventHandlerConfiguration config) {
        GridPane layout = SettingsTab.createGridLayout();
        int row = 0;
        layout.add(new Label("Name"), 0, row);
        TextField name = new TextField();
        layout.add(name, 1, row);
        return layout;
    }


    private TableView createActionTable() {
        TableView view = new TableView();
        return view;
    }
}
