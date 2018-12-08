package ctbrec.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class EventHandler {

    private List<Predicate<Event>> predicates = new ArrayList<>();
    private List<Consumer<Event>> actions;

    @SafeVarargs
    public EventHandler(Consumer<Event> action, Predicate<Event>... predicates) {
        this(Collections.singletonList(action), predicates);
    }

    @SafeVarargs
    public EventHandler(List<Consumer<Event>> actions, Predicate<Event>... predicates) {
        this.actions = actions;
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
            for (Consumer<Event> action : actions) {
                action.accept(evt);
            }
        }
    }
}
