package ctbrec.ui.event;

import ctbrec.OS;
import ctbrec.event.Action;
import ctbrec.event.Event;
import ctbrec.ui.CamrecApplication;

public class ModelStateNotification extends Action {

    private String header;
    private String msg;

    public ModelStateNotification(String header, String msg) {
        this.header = header;
        this.msg = msg;
        name = "show notification";
    }

    @Override
    public void accept(Event evt) {
        OS.notification(CamrecApplication.title, header, msg);
    }
}
