package ctbrec.event;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import ctbrec.Model;
import ctbrec.event.EventHandlerConfiguration.PredicateConfiguration;

public class ModelPredicate extends EventPredicate {

    private Predicate<Event> internal;

    public ModelPredicate() {}

    public ModelPredicate(Model model) {
        internal = createFor(model);
    }

    public ModelPredicate(List<Model> models) {
        configure(models);
    }

    private void configure(List<Model> models) {
        if(models.isEmpty()) {
            throw new IllegalArgumentException("List has to contain at least one model");
        }

        Predicate<Event> predicate = createFor(models.get(0));
        for (int i = 1; i < models.size(); i++) {
            predicate = predicate.or(createFor(models.get(i)));
        }
        internal = predicate;
    }

    @Override
    public boolean test(Event evt) {
        return internal.test(evt);
    }

    private Predicate<Event> createFor(Model model) {
        return evt -> {
            if(evt instanceof AbstractModelEvent) {
                AbstractModelEvent modelEvent = (AbstractModelEvent) evt;
                Model other = modelEvent.getModel();
                return Objects.equals(model, other);
            } else {
                return false;
            }
        };
    }

    @Override
    public void configure(PredicateConfiguration pc) {
        configure(pc.getModels());
    }
}
