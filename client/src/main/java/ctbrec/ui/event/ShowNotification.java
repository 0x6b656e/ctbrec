package ctbrec.ui.event;

import ctbrec.Model;
import ctbrec.OS;
import ctbrec.event.Action;
import ctbrec.event.Event;
import ctbrec.event.EventHandlerConfiguration.ActionConfiguration;
import ctbrec.event.ModelStateChangedEvent;
import ctbrec.event.RecordingStateChangedEvent;
import ctbrec.ui.CamrecApplication;

public class ShowNotification extends Action {

    public ShowNotification() {
        name = "show notification";
    }

    @Override
    public void accept(Event evt) {
        String header = evt.getType().toString();
        String msg;
        switch(evt.getType()) {
        case MODEL_STATUS_CHANGED:
            ModelStateChangedEvent modelEvent = (ModelStateChangedEvent) evt;
            Model m = modelEvent.getModel();
            msg = m.getDisplayName() + " is now " + modelEvent.getNewState().toString();
            break;
        case RECORDING_STATUS_CHANGED:
            RecordingStateChangedEvent recEvent = (RecordingStateChangedEvent) evt;
            m = recEvent.getModel();
            msg = "Recording for model " + m.getDisplayName() + " is now in state " + recEvent.getState().toString();
            break;
        default:
            msg = evt.getDescription();
        }
        OS.notification(CamrecApplication.title, header, msg);
    }

    @Override
    public void configure(ActionConfiguration config) throws Exception {
    }
}
