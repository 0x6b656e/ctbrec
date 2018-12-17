package ctbrec.sites;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import ctbrec.Model;
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

    @Override
    public boolean supportsSearch() {
        return false;
    }

    @Override
    public List<Model> search(String q) throws IOException, InterruptedException {
        return Collections.emptyList();
    }

    @Override
    public boolean searchRequiresLogin() {
        return false;
    }

    @Override
    public Model createModelFromUrl(String url) {
        return null;
    }
}
