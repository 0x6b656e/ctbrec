package ctbrec.ui;

import java.util.Map;
import java.util.Objects;

import com.google.common.eventbus.Subscribe;

import javafx.application.Platform;
import javafx.scene.control.Label;

public class TokenLabel extends Label {

    private int tokens = -1;

    public TokenLabel() {
        setText("Tokens: loading…");
        CamrecApplication.bus.register(new Object() {
            @Subscribe
            public void tokensUpdates(Map<String, Object> e) {
                if(Objects.equals("tokens", e.get("event"))) {
                    tokens = (int) e.get("amount");
                    updateText();
                } else if(Objects.equals("tokens.sent", e.get("event"))) {
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
}
