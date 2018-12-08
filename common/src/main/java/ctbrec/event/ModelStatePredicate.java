package ctbrec.event;

import java.util.function.Predicate;

import ctbrec.Model;

public class ModelStatePredicate implements Predicate<Event> {

    private Model.State state;

    private ModelStatePredicate(Model.State state) {
        this.state = state;
    }

    @Override
    public boolean test(Event evt) {
        if(evt instanceof AbstractModelEvent) {
            ModelStateChangedEvent modelEvent = (ModelStateChangedEvent) evt;
            Model.State newState = modelEvent.getNewState();
            return newState == state;
        } else {
            return false;
        }
    }

    public static ModelStatePredicate of(Model.State state) {
        return new ModelStatePredicate(state);
    }
}
