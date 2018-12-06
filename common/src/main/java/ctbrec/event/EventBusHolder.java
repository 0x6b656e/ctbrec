package ctbrec;

import java.util.concurrent.Executors;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

public class EventBusHolder {

    public static final String EVENT = "event";
    public static final String OLD = "old";
    public static final String STATUS = "status";
    public static final String MODEL = "model";

    public static enum EVENT_TYPE {
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

    public static final EventBus BUS = new AsyncEventBus(Executors.newSingleThreadExecutor());
}
