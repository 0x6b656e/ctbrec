package ctbrec.sites;

import java.io.IOException;
import java.util.List;

import ctbrec.Model;
import ctbrec.io.HttpClient;
import ctbrec.recorder.Recorder;

public interface Site {
    public String getName();
    public String getBaseUrl();
    public String getAffiliateLink();
    public void setRecorder(Recorder recorder);
    public Recorder getRecorder();
    public Model createModel(String name);
    public Integer getTokenBalance() throws IOException;
    public String getBuyTokensLink();
    public boolean login() throws IOException;
    public HttpClient getHttpClient();
    public void init() throws IOException;
    public void shutdown();
    public boolean supportsTips();
    public boolean supportsFollow();
    public boolean supportsSearch();
    public boolean isSiteForModel(Model m);
    public boolean credentialsAvailable();
    public void setEnabled(boolean enabled);
    public boolean isEnabled();
    public List<Model> search(String q) throws IOException, InterruptedException;
    public boolean searchRequiresLogin();
    public Model createModelFromUrl(String url);
}
