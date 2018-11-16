package ctbrec.ui.autofilltextbox;


import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class AutoFillTextField extends TextField {

    private ObservableList<String> suggestions;
    private EventHandler<ActionEvent> handler;

    public AutoFillTextField(ObservableList<String> suggestions) {
        this.suggestions = suggestions;
        addEventHandler(KeyEvent.KEY_RELEASED, (evt) -> {
            if (evt.getCode().isLetterKey() || evt.getCode().isDigitKey()) {
                autocomplete(false);
            } else if (evt.getCode() == KeyCode.ENTER) {
                if (getSelection().getLength() > 0) {
                    selectRange(0, 0);
                    insertText(lengthProperty().get(), ":");
                    positionCaret(lengthProperty().get());
                    evt.consume();
                } else {
                    handler.handle(new ActionEvent(this, null));
                }
            } else if (evt.getCode() == KeyCode.SPACE && evt.isControlDown()) {
                autocomplete(true);
            }
        });
    }

    private void autocomplete(boolean fulltextSearch) {
        String oldtext = getOldText();
        if(oldtext.isEmpty()) {
            return;
        }
        for (String sug : suggestions) {
            boolean startsWith = sug.toLowerCase().startsWith(oldtext.toLowerCase());
            boolean textMatch = fulltextSearch && sug.toLowerCase().contains(oldtext.toLowerCase());
            if(startsWith || textMatch) {
                setText(sug);
                int pos = oldtext.length();
                positionCaret(pos);
                selectRange(pos, sug.length());
                break;
            }
        }
    }

    private String getOldText() {
        if(getSelection().getLength() > 0) {
            return getText().substring(0, getSelection().getStart());
        } else {
            return getText();
        }
    }

    public void onActionHandler(EventHandler<ActionEvent> handler) {
        this.handler = handler;
    }
}
