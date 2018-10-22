package ctbrec.sites;

import java.io.IOException;

import ctbrec.Model;
import ctbrec.io.HttpClient;
import ctbrec.recorder.Recorder;
import ctbrec.ui.TabProvider;

public interface Site {
    public String getName();
    public String getBaseUrl();
    public String getAffiliateLink();
    public void setRecorder(Recorder recorder);
    public TabProvider getTabProvider();
    public Model createModel(String name);
    public Integer getTokenBalance() throws IOException;
    public String getBuyTokensLink();
    public void login() throws IOException;
    public HttpClient getHttpClient();
    public void init() throws IOException;
    public void shutdown();
    public boolean supportsTips();
    public boolean supportsFollow();
    public boolean isSiteForModel(Model m);
}
