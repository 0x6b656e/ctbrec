package ctbrec;

import java.util.concurrent.Executors;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

public class EventBusHolder {
    public static final EventBus BUS = new AsyncEventBus(Executors.newSingleThreadExecutor());
}
