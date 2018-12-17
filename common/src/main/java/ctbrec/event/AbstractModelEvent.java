package ctbrec.event;

import ctbrec.Model;

public abstract class AbstractModelEvent extends Event {

    protected Model model;

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }
}
