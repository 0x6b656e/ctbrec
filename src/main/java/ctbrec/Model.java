package ctbrec;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.iheartradio.m3u8.ParseException;
import com.iheartradio.m3u8.PlaylistException;

import ctbrec.recorder.download.StreamSource;
import ctbrec.sites.Site;

public interface Model {
    public String getUrl();
    public void setUrl(String url);
    public String getName();
    public void setName(String name);
    public String getPreview();
    public void setPreview(String preview);
    public List<String> getTags();
    public void setTags(List<String> tags);
    public String getDescription();
    public void setDescription(String description);
    public int getStreamUrlIndex();
    public void setStreamUrlIndex(int streamUrlIndex);
    public boolean isOnline() throws IOException, ExecutionException, InterruptedException;
    public boolean isOnline(boolean ignoreCache) throws IOException, ExecutionException, InterruptedException;
    public String getOnlineState(boolean failFast) throws IOException, ExecutionException;
    public List<StreamSource> getStreamSources() throws IOException, ExecutionException, ParseException, PlaylistException;
    public String getSegmentPlaylistUrl() throws IOException, ExecutionException, ParseException, PlaylistException;
    public void invalidateCacheEntries();
    public void receiveTip(int tokens) throws IOException;
    public int[] getStreamResolution(boolean failFast) throws ExecutionException;
    public boolean follow() throws IOException;
    public boolean unfollow() throws IOException;
    public void setSite(Site site);
}