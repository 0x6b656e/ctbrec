package ctbrec.event;

import java.util.concurrent.Executors;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

public class EventBusHolder {

    public static final String EVENT = "event";
    public static final String OLD = "old";
    public static final String STATUS = "status";
    public static final String MODEL = "model";

    public static final EventBus BUS = new AsyncEventBus(Executors.newSingleThreadExecutor());
}
