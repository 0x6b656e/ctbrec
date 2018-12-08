package ctbrec.event;

import java.util.function.Consumer;

public abstract class Action implements Consumer<Event> {

    protected String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
