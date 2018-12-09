package ctbrec.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ctbrec.Model;

public class EventHandlerConfiguration {

    private String id;
    private String name;
    private Event.Type event;
    private List<PredicateConfiguration> predicates = new ArrayList<>();
    private List<ActionConfiguration> actions = new ArrayList<>();

    public EventHandlerConfiguration() {
        id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public static class PredicateConfiguration {
        private String name;
        private String type;
        private List<Model> models;
        private Map<String, Object> configuration = new HashMap<>();

        public void setName(String name) {
            this.name = name;
        }

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

        public List<Model> getModels() {
            return models;
        }

        public void setModels(List<Model> models) {
            this.models = models;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static class ActionConfiguration {
        private String name;
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

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Override
    public String toString() {
        return name + ", when:" + predicates + " do:" + actions + "]";
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
        EventHandlerConfiguration other = (EventHandlerConfiguration) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
}
