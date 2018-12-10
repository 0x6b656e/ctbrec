package ctbrec.event;

import java.io.File;
import java.time.Instant;

import ctbrec.Model;
import ctbrec.Recording.State;

public class RecordingStateChangedEvent extends AbstractModelEvent {

    private File path;
    private State newState;
    private Instant startTime;

    public RecordingStateChangedEvent(File recording, State newState, Model model, Instant startTime) {
        super.model = model;
        this.path = recording;
        this.newState = newState;
        this.startTime = startTime;
    }

    @Override
    public Type getType() {
        return Event.Type.RECORDING_STATUS_CHANGED;
    }

    @Override
    public String getName() {
        return "Recording state changed";
    }

    @Override
    public String getDescription() {
        return "Fired when a recording state changed. E.g. from RECORDING to STOPPED";
    }

    @Override
    public String[] getExecutionParams() {
        return new String[] {
                getType().name(),
                path.getAbsolutePath(),
                newState.name(),
                model.getDisplayName(),
                model.getSite().getName(),
                model.getUrl(),
                Long.toString(startTime.getEpochSecond())
        };
    }

    public State getState() {
        return newState;
    }

    @Override
    public String toString() {
        return "RecordingStateChanged[" + newState.name() + "," + model.getDisplayName() + "," + path + "]";
    }

}
