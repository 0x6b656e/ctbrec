package ctbrec.event;

import java.util.function.Predicate;

import ctbrec.event.EventHandlerConfiguration.PredicateConfiguration;

public abstract class EventPredicate implements Predicate<Event> {

    public abstract void configure(PredicateConfiguration pc);
}
