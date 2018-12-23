package ctbrec.event;

import ctbrec.event.Event.Type;
import ctbrec.event.EventHandlerConfiguration.PredicateConfiguration;

public class EventTypePredicate extends EventPredicate {

    private Type type;

    public EventTypePredicate() {
    }

    public EventTypePredicate(Type type) {
        this.type = type;
    }

    @Override
    public boolean test(Event evt) {
        return evt.getType() == type;
    }

    @Override
    public void configure(PredicateConfiguration pc) {
        type = Type.valueOf((String) pc.getConfiguration().get("type"));
    }
}
