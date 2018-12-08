package ctbrec.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventHandlerConfiguration {

    private String name;
    private Event.Type event;
    private List<PredicateConfiguration> predicates = new ArrayList<>();
    private List<ActionConfiguration> actions = new ArrayList<>();

    public Event.Type getEvent() {
        return event;
    }

    public void setEvent(Event.Type event) {
        this.event = event;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<PredicateConfiguration> getPredicates() {
        return predicates;
    }

    public void setPredicates(List<PredicateConfiguration> predicates) {
        this.predicates = predicates;
    }

    public List<ActionConfiguration> getActions() {
        return actions;
    }

    public void setActions(List<ActionConfiguration> actions) {
        this.actions = actions;
    }

    public class PredicateConfiguration {
        private String type;
        private Map<String, Object> configuration = new HashMap<>();

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Map<String, Object> getConfiguration() {
            return configuration;
        }

        public void setConfiguration(Map<String, Object> configuration) {
            this.configuration = configuration;
        }

    }

    public class ActionConfiguration {
        private String type;
        private Map<String, Object> configuration = new HashMap<>();

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Map<String, Object> getConfiguration() {
            return configuration;
        }

        public void setConfiguration(Map<String, Object> configuration) {
            this.configuration = configuration;
        }
    }
}
