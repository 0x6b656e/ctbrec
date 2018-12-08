package ctbrec.event;

import java.util.function.Predicate;

import ctbrec.event.Event.Type;

public class EventTypePredicate implements Predicate<Event> {

    private Type type;

    private EventTypePredicate(Type type) {
        this.type = type;
    }

    @Override
    public boolean test(Event evt) {
        return evt.getType() == type;
    }

    public static EventTypePredicate of(Type type) {
        return new EventTypePredicate(type);
    }
}
