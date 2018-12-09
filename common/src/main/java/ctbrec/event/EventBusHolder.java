package ctbrec.event;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

public class EventBusHolder {
    private static final transient Logger LOG = LoggerFactory.getLogger(EventBusHolder.class);
    private static Map<String, EventHandler> handlers = new HashMap<>();

    public static final EventBus BUS = new AsyncEventBus(Executors.newSingleThreadExecutor());

    public static void register(EventHandler handler) {
        if(handlers.containsKey(handler.getId())) {
            LOG.warn("EventHandler with ID {} is already registered", handler.getId());
        } else {
            BUS.register(handler);
            handlers.put(handler.getId(), handler);
            LOG.debug("EventHandler with ID {} has been added", handler.getId());
        }
    }

    public static void unregister(String id) {
        EventHandler handler = handlers.get(id);
        if(handler != null) {
            BUS.unregister(handler);
            handlers.remove(id);
            LOG.debug("EventHandler with ID {} has been removed", id);
        }
    }
}
