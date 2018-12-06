package ctbrec.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogReaction extends EventReaction {

    private static final transient Logger LOG = LoggerFactory.getLogger(LogReaction.class);

    public LogReaction() {
        super(evt -> {
            LOG.debug("LogReaction: {}", evt);
        }, TypePredicate.of(Event.Type.RECORDING_STATUS_CHANGED));
    }
}
