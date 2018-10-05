package ctbrec.ui;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.HttpClient;
import ctbrec.Model;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import okhttp3.Request;
import okhttp3.Response;

public class TipDialog extends TextInputDialog {

    private static final transient Logger LOG = LoggerFactory.getLogger(TipDialog.class);

    public TipDialog(Model model) {
        setTitle("Send Tip");
        loadTokenBalance();
        setHeaderText("Loading token balance…");
        setContentText("Amount of tokens to tip:");
        setResizable(true);
    }

    private void loadTokenBalance() {
        Task<Integer> task = new Task<Integer>() {
            @Override
            protected Integer call() throws Exception {
                String username = Config.getInstance().getSettings().username;
                if (username == null || username.trim().isEmpty()) {
                    throw new IOException("Not logged in");
                }

                String url = "https://chaturbate.com/p/" + username + "/";
                HttpClient client = HttpClient.getInstance();
                Request req = new Request.Builder().url(url).build();
                Response resp = client.execute(req, true);
                if (resp.isSuccessful()) {
                    String profilePage = resp.body().string();
                    String tokenText = HtmlParser.getText(profilePage, "span.tokencount");
                    int tokens = Integer.parseInt(tokenText);
                    return tokens;
                } else {
                    throw new IOException("HTTP response: " + resp.code() + " - " + resp.message());
                }
            }

            @Override
            protected void done() {
                try {
                    int tokens = get();
                    Platform.runLater(() -> setHeaderText("Current token balance: " + tokens));
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("Couldn't retrieve account balance", e);
                    showErrorDialog(e);
                }
            }
        };
        new Thread(task).start();
    }

    private void showErrorDialog(Throwable throwable) {
        Platform.runLater(() -> {
            Alert alert = new AutosizeAlert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Couldn't retrieve token balance");
            alert.setContentText("Error while loading your token balance: " + throwable.getLocalizedMessage());
            alert.showAndWait();
            TipDialog.this.close();
        });
    }

}
