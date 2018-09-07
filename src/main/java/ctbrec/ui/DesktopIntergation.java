package ctbrec.ui;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;

public class DesktopIntergation {

    private static final transient Logger LOG = LoggerFactory.getLogger(DesktopIntergation.class);

    public static void open(String uri) {
        try {
            CtbrecApplication.hostServices.showDocument(uri);
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
        Alert shutdownInfo = new AutosizeAlert(Alert.AlertType.ERROR);
        shutdownInfo.setTitle("Open URL");
        shutdownInfo.setContentText("Couldn't open URL");
        BorderPane pane = new BorderPane();
        pane.setTop(new Label());
        TextField urlField = new TextField(uri);
        urlField.setPadding(new Insets(10));
        urlField.setEditable(false);
        pane.setCenter(urlField);
        shutdownInfo.getDialogPane().setExpandableContent(pane);
        shutdownInfo.getDialogPane().setExpanded(true);
        shutdownInfo.show();
    }
}
