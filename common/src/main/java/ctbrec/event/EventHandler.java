package ctbrec.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

import ctbrec.event.Event.Type;
import ctbrec.event.EventHandlerConfiguration.ActionConfiguration;
import ctbrec.event.EventHandlerConfiguration.PredicateConfiguration;

public class EventHandler {
    private static final transient Logger LOG = LoggerFactory.getLogger(EventHandler.class);

    private List<EventPredicate> predicates = new ArrayList<>();
    private List<Action> actions;
    private Type event;
    private String id;

    public EventHandler(EventHandlerConfiguration config) {
        id = config.getId();
        event = config.getEvent();
        actions = createActions(config);
        predicates = createPredicates(config);
        predicates.add(new EventTypePredicate(event));
    }

    public String getId() {
        return id;
    }

    @SafeVarargs
    public EventHandler(Action action, EventPredicate... predicates) {
        this(Collections.singletonList(action), predicates);
    }

    @SafeVarargs
    public EventHandler(List<Action> actions, EventPredicate... predicates) {
        this.actions = actions;
        for (EventPredicate predicate : predicates) {
            this.predicates.add(predicate);
        }
    }

    @Subscribe
    public void reactToEvent(Event evt) {
        try {
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
        } catch(Exception e) {
            LOG.error("Error while processing event", e);
        }
    }

    private List<EventPredicate> createPredicates(EventHandlerConfiguration config) {
        List<EventPredicate> predicates = new ArrayList<>(config.getPredicates().size());
        for (PredicateConfiguration pc : config.getPredicates()) {

            try {
                @SuppressWarnings("unchecked")
                Class<EventPredicate> cls = (Class<EventPredicate>) Class.forName(pc.getType());
                if(cls == null) {
                    LOG.warn("Ignoring unknown action {}", cls);
                    continue;
                }
                EventPredicate predicate = cls.newInstance();
                predicate.configure(pc);
                predicates.add(predicate);
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                LOG.warn("Error while creating action {} {}", pc.getType(), pc.getConfiguration(), e);
            }
        }
        return predicates;
    }

    private List<Action> createActions(EventHandlerConfiguration config) {
        List<Action> actions = new ArrayList<>(config.getActions().size());
        for (ActionConfiguration ac : config.getActions()) {
            try {
                @SuppressWarnings("unchecked")
                Class<Action> cls = (Class<Action>) Class.forName(ac.getType());
                if(cls == null) {
                    LOG.warn("Ignoring unknown action {}", cls);
                    continue;
                }
                Action action = cls.newInstance();
                action.configure(ac);
                actions.add(action);
            } catch (Exception e) {
                LOG.warn("Error while creating action {} {}", ac.getType(), ac.getConfiguration(), e);
            }
        }
        return actions;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EventHandler other = (EventHandler) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }


}
