package ctbrec.event;

import java.util.function.Predicate;

import ctbrec.event.Event.Type;

public class TypePredicate implements Predicate<Event> {

    private Type type;

    private TypePredicate(Type type) {
        this.type = type;
    }

    @Override
    public boolean test(Event evt) {
        return evt.getType() == type;
    }

    public static TypePredicate of(Type type) {
        return new TypePredicate(type);
    }
}
