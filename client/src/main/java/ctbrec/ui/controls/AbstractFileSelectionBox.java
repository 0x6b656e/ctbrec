package ctbrec.ui.controls;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.ui.AutosizeAlert;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

public abstract class AbstractFileSelectionBox extends HBox {
    private static final transient Logger LOG = LoggerFactory.getLogger(AbstractFileSelectionBox.class);

    private ObjectProperty<File> fileProperty = new ObjectPropertyBase<File>() {
        @Override
        public Object getBean() {
            return null;
        }

        @Override
        public String getName() {
            return "file";
        }
    };
    protected TextField fileInput;
    private Tooltip validationError = new Tooltip();

    public AbstractFileSelectionBox() {
        super(5);
        fileInput = new TextField();
        fileInput.textProperty().addListener(textListener());
        fileInput.focusedProperty().addListener((obs, o, n) -> {
            if(!n) {
                validationError.hide();
            }
        });
        Node browse = createBrowseButton();
        getChildren().addAll(fileInput, browse);
        fileInput.disableProperty().bind(disableProperty());
        browse.disableProperty().bind(disableProperty());
        HBox.setHgrow(fileInput, Priority.ALWAYS);
    }

    public AbstractFileSelectionBox(String initialValue) {
        this();
        fileInput.setText(initialValue);
    }

    private ChangeListener<? super String> textListener() {
        return (obs, o, n) -> {
            String input = fileInput.getText();
            File program = new File(input);
            setFile(program);
        };
    }

    protected void setFile(File file) {
        String msg = validate(file);
        if (msg != null) {
            fileInput.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.DASHED, new CornerRadii(2), new BorderWidths(2))));
            validationError.setText(msg);
            fileInput.setTooltip(validationError);
            Point2D p = fileInput.localToScreen(fileInput.getTranslateY(), fileInput.getTranslateY());
            if(!validationError.isShowing() && getScene() != null) {
                validationError.show(getScene().getWindow(), p.getX(), p.getY() + fileInput.getHeight() + 4);
            }
        } else {
            fileInput.setBorder(Border.EMPTY);
            fileInput.setTooltip(null);
            fileProperty.set(file);
            validationError.hide();
        }
    }

    protected String validate(File file) {
        if (file == null || !file.exists()) {
            return "File does not exist";
        } else {
            return null;
        }
    }

    private Button createBrowseButton() {
        Button button = new Button("Select");
        button.setOnAction((e) -> {
            choose();
        });
        return button;
    }

    protected void choose() {
        FileChooser chooser = new FileChooser();
        File program = chooser.showOpenDialog(null);
        if(program != null) {
            try {
                fileInput.setText(program.getCanonicalPath());
            } catch (IOException e1) {
                LOG.error("Couldn't determine path", e1);
                Alert alert = new AutosizeAlert(Alert.AlertType.ERROR);
                alert.setTitle("Whoopsie");
                alert.setContentText("Couldn't determine path");
                alert.showAndWait();
            }
            setFile(program);
        }
    }

    public ObjectProperty<File> fileProperty() {
        return fileProperty;
    }
}
