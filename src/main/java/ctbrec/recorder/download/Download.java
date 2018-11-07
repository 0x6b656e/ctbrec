package ctbrec.recorder.download;

import java.io.File;
import java.io.IOException;
import java.time.Instant;

import ctbrec.Config;
import ctbrec.Model;

public interface Download {
    public void start(Model model, Config config) throws IOException;
    public void stop();
    public boolean isAlive();
    public File getDirectory();
    public Model getModel();
    public Instant getStartTime();
}
