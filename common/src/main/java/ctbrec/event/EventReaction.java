package ctbrec.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class EventReaction {

    private List<Predicate<Event>> predicates = new ArrayList<>();
    private Consumer<Event> action;

    @SafeVarargs
    public EventReaction(Consumer<Event> action, Predicate<Event>... predicates) {
        this.action = action;
        for (Predicate<Event> predicate : predicates) {
            this.predicates.add(predicate);
        }
    }

    public void reactToEvent(Event evt) {
        boolean matches = true;
        for (Predicate<Event> predicate : predicates) {
            if(!predicate.test(evt)) {
                matches = false;
            }
        }
        if(matches) {
            action.accept(evt);
        }
    }
}
