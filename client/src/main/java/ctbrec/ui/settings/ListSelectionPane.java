package ctbrec.ui.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ListSelectionPane<T extends Comparable<T>> extends GridPane {

    private ListView<T> availableListView = new ListView<>();
    private ListView<T> selectedListView = new ListView<>();
    private Button addModel = new Button(">");
    private Button removeModel = new Button("<");
    private CheckBox selectAll = new CheckBox("all");

    public ListSelectionPane(List<T> available, List<T> selected) {
        super();
        setHgap(5);
        setVgap(5);

        createGui();
        fillLists(available, selected);
    }

    private void fillLists(List<T> available, List<T> selected) {
        ObservableList<T> obsAvail = FXCollections.observableArrayList(available);
        ObservableList<T> obsSel = FXCollections.observableArrayList(selected);
        for (Iterator<T> iterator = obsAvail.iterator(); iterator.hasNext();) {
            T t = iterator.next();
            if(obsSel.contains(t)) {
                iterator.remove();
            }
        }
        Collections.sort(obsAvail);
        Collections.sort(obsSel);
        availableListView.setItems(obsAvail);
        selectedListView.setItems(obsSel);
        availableListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        selectedListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private void createGui() {
        Label labelAvailable = new Label("Available");
        Label labelSelected = new Label("Selected");

        add(labelAvailable, 0, 0);
        add(availableListView, 0, 1);

        VBox buttonBox = new VBox(5);
        buttonBox.getChildren().add(addModel);
        buttonBox.getChildren().add(removeModel);
        buttonBox.setAlignment(Pos.CENTER);
        add(buttonBox, 1, 1);

        add(labelSelected, 2, 0);
        add(selectedListView, 2, 1);

        add(selectAll, 0, 2);

        GridPane.setHgrow(availableListView, Priority.ALWAYS);
        GridPane.setHgrow(selectedListView, Priority.ALWAYS);
        GridPane.setFillWidth(availableListView, true);
        GridPane.setFillWidth(selectedListView, true);

        addModel.setOnAction(evt -> addSelectedItems());
        removeModel.setOnAction(evt -> removeSelectedItems());

        availableListView.disableProperty().bind(selectAll.selectedProperty());
        selectedListView.disableProperty().bind(selectAll.selectedProperty());
        addModel.disableProperty().bind(selectAll.selectedProperty());
        removeModel.disableProperty().bind(selectAll.selectedProperty());
    }

    private void addSelectedItems() {
        List<T> selected = new ArrayList<>(availableListView.getSelectionModel().getSelectedItems());
        for (T t : selected) {
            if(!selectedListView.getItems().contains(t)) {
                selectedListView.getItems().add(t);
                availableListView.getItems().remove(t);
            }
        }
        Collections.sort(selectedListView.getItems());
    }

    private void removeSelectedItems() {
        List<T> selected = new ArrayList<>(selectedListView.getSelectionModel().getSelectedItems());
        for (T t : selected) {
            if(!availableListView.getItems().contains(t)) {
                availableListView.getItems().add(t);
                selectedListView.getItems().remove(t);
            }
        }
        Collections.sort(availableListView.getItems());
    }

    public List<T> getSelectedItems() {
        if(selectAll.isSelected()) {
            List<T> all = new ArrayList<>(availableListView.getItems());
            all.addAll(selectedListView.getItems());
            return all;
        } else {
            return selectedListView.getItems();
        }
    }

    public boolean isAllSelected() {
        return selectAll.isSelected();
    }
}
