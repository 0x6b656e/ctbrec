package ctbrec;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

public class Recording {
    private String modelName;
    private Instant startDate;
    private String path;
    private boolean hasPlaylist;
    private State status = State.UNKNOWN;
    private int progress = -1;
    private long sizeInByte;

    public static enum State {
        RECORDING,
        GENERATING_PLAYLIST,
        STOPPED,
        FINISHED,
        DOWNLOADING,
        POST_PROCESSING,
        UNKNOWN
    }

    public Recording() {}

    public Recording(String path) throws ParseException {
        this.path = path;
        this.modelName = path.substring(0, path.indexOf("/"));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
        Date date = sdf.parse(path.substring(path.indexOf('/')+1));
        startDate = Instant.ofEpochMilli(date.getTime());
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public State getStatus() {
        return status;
    }

    public void setStatus(State status) {
        this.status = status;
    }

    public int getProgress() {
        return this.progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean hasPlaylist() {
        return hasPlaylist;
    }

    public void setHasPlaylist(boolean hasPlaylist) {
        this.hasPlaylist = hasPlaylist;
    }

    public long getSizeInByte() {
        return sizeInByte;
    }

    public void setSizeInByte(long sizeInByte) {
        this.sizeInByte = sizeInByte;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((modelName == null) ? 0 : modelName.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result + ((startDate == null) ? 0 : startDate.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        Recording other = (Recording) obj;
        if (modelName == null) {
            if (other.getModelName() != null)
                return false;
        } else if (!modelName.equals(other.getModelName()))
            return false;
        if (path == null) {
            if (other.getPath() != null)
                return false;
        } else if (!path.equals(other.getPath()))
            return false;
        if (startDate == null) {
            if (other.getStartDate() != null)
                return false;
        } else if (!startDate.equals(other.getStartDate()))
            return false;
        return true;
    }
}
