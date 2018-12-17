package ctbrec.event;

import ctbrec.Model;

public class ModelIsOnlineEvent extends AbstractModelEvent {

    public ModelIsOnlineEvent(Model model) {
        super.model = model;
    }

    @Override
    public Type getType() {
        return Event.Type.MODEL_ONLINE;
    }

    @Override
    public String getName() {
        return "Model is online";
    }

    @Override
    public String getDescription() {
        return "Repeatedly fired when a model is online";
    }

    @Override
    public String[] getExecutionParams() {
        return new String[] {
                getType().toString(),
                model.getDisplayName(),
                model.getUrl(),
                model.getSite().getName()
        };
    }

}
