package ctbrec.ui;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

import ctbrec.event.EventBusHolder;
import ctbrec.sites.Site;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

public class TokenLabel extends Label {

    private static final transient Logger LOG = LoggerFactory.getLogger(TokenLabel.class);
    private int tokens = -1;
    private Site site;

    public TokenLabel(Site site) {
        this.site = site;
        setText("Tokens: loading…");
        EventBusHolder.BUS.register(new Object() {
            @Subscribe
            public void tokensUpdates(Map<String, Object> e) {
                if (Objects.equals("tokens", e.get("event"))) {
                    tokens = (int) e.get("amount");
                    updateText();
                } else if (Objects.equals("tokens.sent", e.get("event"))) {
                    int _tokens = (int) e.get("amount");
                    tokens -= _tokens;
                    updateText();
                }
            }
        });
    }

    public void decrease(int tokens) {
        this.tokens -= tokens;
        updateText();
    }

    public void update(int tokens) {
        this.tokens = tokens;
        updateText();
    }

    private void updateText() {
        Platform.runLater(() -> setText("Tokens: " + tokens));
    }

    public void loadBalance() {
        Task<Integer> task = new Task<Integer>() {
            @Override
            protected Integer call() throws Exception {
                if (!Objects.equals(System.getenv("CTBREC_DEV"), "1")) {
                    SiteUiFactory.getUi(site).login();
                    return site.getTokenBalance();
                } else {
                    return 1_000_000;
                }
            }

            @Override
            protected void done() {
                try {
                    int tokens = get();
                    update(tokens);
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("Couldn't retrieve account balance", e);
                    Platform.runLater(() -> {
                        setText("Tokens: error");
                        setTooltip(new Tooltip(e.getMessage()));
                    });
                }
            }
        };
        new Thread(task).start();
    }
}
