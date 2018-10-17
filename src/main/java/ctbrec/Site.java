package ctbrec;

import ctbrec.recorder.Recorder;
import ctbrec.ui.TabProvider;

public interface Site {
    public String getName();
    public String getBaseUrl();
    public String getAffiliateLink();
    public void setRecorder(Recorder recorder);
    public TabProvider getTabProvider();
    public Model createModel(String name);
}
