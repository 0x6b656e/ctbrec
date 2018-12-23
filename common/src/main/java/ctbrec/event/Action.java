package ctbrec.event;

import java.util.function.Consumer;

import ctbrec.event.EventHandlerConfiguration.ActionConfiguration;

public abstract class Action implements Consumer<Event> {

    protected String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract void configure(ActionConfiguration config) throws Exception;

    @Override
    public String toString() {
        return name;
    }
}
