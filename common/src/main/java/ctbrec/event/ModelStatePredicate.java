package ctbrec.event;

import ctbrec.Model;
import ctbrec.Model.State;
import ctbrec.event.EventHandlerConfiguration.PredicateConfiguration;

public class ModelStatePredicate extends EventPredicate {

    private Model.State state;

    public ModelStatePredicate() {}

    public ModelStatePredicate(Model.State state) {
        this.state = state;
    }

    @Override
    public boolean test(Event evt) {
        if(evt instanceof ModelStateChangedEvent) {
            ModelStateChangedEvent modelEvent = (ModelStateChangedEvent) evt;
            Model.State newState = modelEvent.getNewState();
            return newState == state;
        } else {
            return false;
        }
    }

    @Override
    public void configure(PredicateConfiguration pc) {
        state = State.valueOf((String) pc.getConfiguration().get("state"));
    }
}
