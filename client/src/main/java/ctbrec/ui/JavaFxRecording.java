package ctbrec.ui;

import java.time.Instant;

import ctbrec.Config;
import ctbrec.Recording;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class JavaFxRecording extends Recording {

    private transient StringProperty statusProperty = new SimpleStringProperty();
    private transient StringProperty progressProperty = new SimpleStringProperty();
    private transient LongProperty sizeProperty = new SimpleLongProperty();

    private Recording delegate;
    private long lastValue = 0;

    public JavaFxRecording(Recording recording) {
        this.delegate = recording;
    }

    @Override
    public String getModelName() {
        return delegate.getModelName();
    }

    @Override
    public void setModelName(String modelName) {
        delegate.setModelName(modelName);
    }

    @Override
    public Instant getStartDate() {
        return delegate.getStartDate();
    }

    @Override
    public void setStartDate(Instant startDate) {
        delegate.setStartDate(startDate);
    }

    @Override
    public STATUS getStatus() {
        return delegate.getStatus();
    }

    public StringProperty getStatusProperty() {
        return statusProperty;
    }

    @Override
    public void setStatus(STATUS status) {
        delegate.setStatus(status);
        switch(status) {
        case RECORDING:
            statusProperty.set("recording");
            break;
        case GENERATING_PLAYLIST:
            statusProperty.set("generating playlist");
            break;
        case FINISHED:
            statusProperty.set("finished");
            break;
        case DOWNLOADING:
            statusProperty.set("downloading");
            break;
        case POST_PROCESSING:
            statusProperty.set("post-processing");
            break;
        case STOPPED:
            statusProperty.set("stopped");
            break;
        case UNKNOWN:
            statusProperty.set("unknown");
            break;
        }
    }

    @Override
    public int getProgress() {
        return delegate.getProgress();
    }

    @Override
    public void setProgress(int progress) {
        delegate.setProgress(progress);
        if(progress >= 0) {
            progressProperty.set(progress+"%");
        } else {
            progressProperty.set("");
        }
    }

    @Override
    public void setSizeInByte(long sizeInByte) {
        delegate.setSizeInByte(sizeInByte);
        sizeProperty.set(sizeInByte);
    }

    public StringProperty getProgressProperty() {
        return progressProperty;
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    public void update(Recording updated) {
        if(!Config.getInstance().getSettings().localRecording) {
            if(getStatus() == STATUS.DOWNLOADING && updated.getStatus() != STATUS.DOWNLOADING) {
                // ignore, because the the status coming from the server is FINISHED and we are
                // overriding it with DOWNLOADING
                return;
            }
        }
        setStatus(updated.getStatus());
        setProgress(updated.getProgress());
        setSizeInByte(updated.getSizeInByte());
    }

    @Override
    public String getPath() {
        return delegate.getPath();
    }

    @Override
    public void setPath(String path) {
        delegate.setPath(path);
    }

    @Override
    public boolean hasPlaylist() {
        return delegate.hasPlaylist();
    }

    @Override
    public void setHasPlaylist(boolean hasPlaylist) {
        delegate.setHasPlaylist(hasPlaylist);
    }

    @Override
    public long getSizeInByte() {
        return delegate.getSizeInByte();
    }

    public LongProperty getSizeProperty() {
        return sizeProperty;
    }

    public boolean valueChanged() {
        boolean changed = getSizeInByte() != lastValue;
        lastValue = getSizeInByte();
        return changed;
    }
}
