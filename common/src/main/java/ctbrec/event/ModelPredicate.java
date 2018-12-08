package ctbrec.event;

import java.util.Objects;
import java.util.function.Predicate;

import ctbrec.Model;

public class ModelPredicate implements Predicate<Event> {

    private Model model;

    private ModelPredicate(Model model) {
        this.model = model;
    }

    @Override
    public boolean test(Event evt) {
        if(evt instanceof AbstractModelEvent) {
            AbstractModelEvent modelEvent = (AbstractModelEvent) evt;
            Model other = modelEvent.getModel();
            return Objects.equals(model, other);
        } else {
            return false;
        }
    }

    public static ModelPredicate of(Model model) {
        return new ModelPredicate(model);
    }
}
