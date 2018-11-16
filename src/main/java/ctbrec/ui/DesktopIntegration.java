package ctbrec.ui;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;

public class DesktopIntegration {

    private static final transient Logger LOG = LoggerFactory.getLogger(DesktopIntegration.class);

    public static void open(String uri) {
        try {
            CamrecApplication.hostServices.showDocument(uri);
            return;
        } catch (Exception e) {
            LOG.debug("Couldn't open URL with host services {}", uri);
        }

        // opening with HostServices failed, now try Desktop
        try {
            Desktop.getDesktop().browse(new URI(uri));
            return;
        } catch (Exception e) {
            LOG.debug("Couldn't open URL with Desktop {}", uri);
        }

        // try external helpers
        String[] externalHelpers = {"kde-open5", "kde-open", "gnome-open", "xdg-open"};
        Runtime rt = Runtime.getRuntime();
        for (String helper : externalHelpers) {
            try {
                rt.exec(helper + " " + uri);
                return;
            } catch (IOException e) {
                LOG.debug("Couldn't open URL with {} {}", helper, uri);
            }
        }

        // all attempts failed, show a dialog with URL at least
        Alert info = new AutosizeAlert(Alert.AlertType.ERROR);
        info.setTitle("Open URL");
        info.setContentText("Couldn't open URL");
        BorderPane pane = new BorderPane();
        pane.setTop(new Label());
        TextField urlField = new TextField(uri);
        urlField.setPadding(new Insets(10));
        urlField.setEditable(false);
        pane.setCenter(urlField);
        info.getDialogPane().setExpandableContent(pane);
        info.getDialogPane().setExpanded(true);
        info.show();
    }

    public static void open(File f) {
        try {
            Desktop.getDesktop().open(f);
            return;
        } catch (Exception e) {
            LOG.debug("Couldn't open file with Desktop {}", f);
        }

        // try external helpers
        String[] externalHelpers = {"kde-open5", "kde-open", "gnome-open", "xdg-open"};
        Runtime rt = Runtime.getRuntime();
        for (String helper : externalHelpers) {
            try {
                rt.exec(helper + " " + f.getAbsolutePath());
                return;
            } catch (IOException e) {
                LOG.debug("Couldn't open file with {} {}", helper, f);
            }
        }

        // all attempts failed, show a dialog with path at least
        Alert info = new AutosizeAlert(Alert.AlertType.ERROR);
        info.setTitle("Open file");
        info.setContentText("Couldn't open file");
        BorderPane pane = new BorderPane();
        pane.setTop(new Label());
        TextField urlField = new TextField(f.toString());
        urlField.setPadding(new Insets(10));
        urlField.setEditable(false);
        pane.setCenter(urlField);
        info.getDialogPane().setExpandableContent(pane);
        info.getDialogPane().setExpanded(true);
        info.show();
    }
}
