package ctbrec.event;

import ctbrec.Model;
import ctbrec.Model.State;

public class ModelStateChangedEvent extends AbstractModelEvent {

    private State oldState;
    private State newState;

    public ModelStateChangedEvent(Model model, Model.State oldState, Model.State newState) {
        super.model = model;
        this.oldState = oldState;
        this.newState = newState;
    }

    @Override
    public Type getType() {
        return Event.Type.MODEL_STATUS_CHANGED;
    }

    @Override
    public String getName() {
        return "Model state changed";
    }

    @Override
    public String getDescription() {
        return "Fired when a model state changed. E.g. from OFFLINE to ONLINE";
    }

    @Override
    public String[] getExecutionParams() {
        return new String[] {
                getType().name(),
                model.getDisplayName(),
                model.getUrl(),
                model.getSite().getName(),
                oldState.name(),
                newState.name()
        };
    }

    public State getOldState() {
        return oldState;
    }

    public void setOldState(State oldState) {
        this.oldState = oldState;
    }

    public State getNewState() {
        return newState;
    }

    public void setNewState(State newState) {
        this.newState = newState;
    }
}
