package ctbrec.ui;

import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class PauseIndicator extends HBox {

    public PauseIndicator(Color c, int size) {
        spacingProperty().setValue(size*1/5);
        Rectangle left = new Rectangle(size*2/5, size);
        left.setFill(c);
        Rectangle right = new Rectangle(size*2/5, size);
        right.setFill(c);
        getChildren().add(left);
        getChildren().add(right);
    }
}
