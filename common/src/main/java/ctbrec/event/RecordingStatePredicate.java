package ctbrec.event;

import ctbrec.Recording;
import ctbrec.event.EventHandlerConfiguration.PredicateConfiguration;

public class RecordingStatePredicate extends EventPredicate {

    private Recording.State state;

    public RecordingStatePredicate() {}

    public RecordingStatePredicate(Recording.State state) {
        this.state = state;
    }

    @Override
    public boolean test(Event evt) {
        if(evt instanceof RecordingStateChangedEvent) {
            RecordingStateChangedEvent event = (RecordingStateChangedEvent) evt;
            Recording.State newState = event.getState();
            return newState == state;
        } else {
            return false;
        }
    }

    @Override
    public void configure(PredicateConfiguration pc) {
        state = Recording.State.valueOf((String) pc.getConfiguration().get("state"));
    }
}
