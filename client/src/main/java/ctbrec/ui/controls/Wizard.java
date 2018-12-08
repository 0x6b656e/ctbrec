package ctbrec.ui.controls;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Wizard extends BorderPane {

    private Pane[] pages;
    private StackPane stack;
    private Stage stage;
    private int page = 0;
    private Button next;
    private Button prev;
    private Button finish;

    public Wizard(Stage stage, Pane... pages) {
        this.stage = stage;
        this.pages = pages;

        if (pages.length == 0) {
            throw new IllegalArgumentException("Provide at least one page");
        }

        createUi();
    }

    private void createUi() {
        stack = new StackPane();
        setCenter(stack);
        
        next = new Button("Next");
        next.setOnAction(evt -> nextPage());
        prev = new Button("Prev");
        prev.setOnAction(evt -> prevPage());
        Button cancel = new Button("Cancel");
        cancel.setOnAction(evt -> stage.close());
        finish = new Button("Finish");
        HBox buttons = new HBox(5, prev, next, cancel, finish);
        buttons.setAlignment(Pos.BASELINE_RIGHT);
        setBottom(buttons);
        BorderPane.setMargin(buttons, new Insets(5));

        if (pages.length != 0) {
            stack.getChildren().add(pages[0]);
        }
        setButtonStates();
    }

    private void prevPage() {
        page = Math.max(0, --page);
        stack.getChildren().clear();
        stack.getChildren().add(pages[page]);
        setButtonStates();
    }

    private void nextPage() {
        page = Math.min(pages.length - 1, ++page);
        stack.getChildren().clear();
        stack.getChildren().add(pages[page]);
        setButtonStates();
    }

    private void setButtonStates() {
        prev.setDisable(page == 0);
        next.setDisable(page == pages.length - 1);
        finish.setDisable(page != pages.length - 1);
    }
}
