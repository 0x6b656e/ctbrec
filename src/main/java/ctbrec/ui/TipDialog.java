package ctbrec.ui;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.io.HttpClient;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
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
        getEditor().setDisable(true);
    }

    private void loadTokenBalance() {
        Task<Integer> task = new Task<Integer>() {
            @Override
            protected Integer call() throws Exception {
                if (!Objects.equals(System.getenv("CTBREC_DEV"), "1")) {
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
                        Map<String, Object> event = new HashMap<>();
                        event.put("event", "tokens");
                        event.put("amount", tokens);
                        CamrecApplication.bus.post(event);
                        return tokens;
                    } else {
                        throw new IOException("HTTP response: " + resp.code() + " - " + resp.message());
                    }
                } else {
                    return 1_000_000;
                }
            }

            @Override
            protected void done() {
                try {
                    int tokens = get();
                    Platform.runLater(() -> {
                        if (tokens <= 0) {
                            String msg = "Do you want to buy tokens now?\n\nIf you agree, Chaturbate will open in a browser. "
                                    + "The used address is an affiliate link, which supports me, but doesn't cost you anything more.";
                            Alert buyTokens = new AutosizeAlert(Alert.AlertType.CONFIRMATION, msg, ButtonType.NO, ButtonType.YES);
                            buyTokens.setTitle("No tokens");
                            buyTokens.setHeaderText("You don't have any tokens");
                            buyTokens.showAndWait();
                            TipDialog.this.close();
                            if(buyTokens.getResult() == ButtonType.YES) {
                                DesktopIntergation.open(CamrecApplication.AFFILIATE_LINK);
                            }
                        } else {
                            getEditor().setDisable(false);
                            setHeaderText("Current token balance: " + tokens);
                        }
                    });
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
