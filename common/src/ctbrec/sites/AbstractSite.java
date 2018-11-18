package ctbrec.sites;

import ctbrec.recorder.Recorder;

public abstract class AbstractSite implements Site {

    private boolean enabled;
    private Recorder recorder;

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setRecorder(Recorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public Recorder getRecorder() {
        return recorder;
    }
}
