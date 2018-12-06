package ctbrec.event;

public abstract class Event {

    public static enum Type {
        /**
         * This event is fired every time the OnlineMonitor sees a model online
         * It is also fired, if the model was online before. You can see it as a "still online ping".
         */
        MODEL_ONLINE,

        /**
         * This event is fired whenever the model's online state (Model.STATUS) changes.
         */
        MODEL_STATUS_CHANGED,


        /**
         * This event is fired whenever the state of a recording changes.
         */
        RECORDING_STATUS_CHANGED
    }

    public abstract Type getType();
    public abstract String getName();
    public abstract String getDescription();
    public abstract String[] getExecutionParams();
}
