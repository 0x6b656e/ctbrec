package ctbrec;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.iheartradio.m3u8.ParseException;
import com.iheartradio.m3u8.PlaylistException;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import ctbrec.recorder.download.StreamSource;
import ctbrec.sites.Site;

public interface Model {

    public static enum STATUS {
        ONLINE("online"),
        OFFLINE("offline"),
        AWAY("away"),
        PRIVATE("private"),
        GROUP("group"),
        UNKNOWN("unknown");

        String display;
        STATUS(String display) {
            this.display = display;
        }

        @Override
        public String toString() {
            return display;
        }
    }

    public String getUrl();

    public void setUrl(String url);

    public String getDisplayName();

    public void setDisplayName(String name);

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

    public STATUS getOnlineState(boolean failFast) throws IOException, ExecutionException;

    public List<StreamSource> getStreamSources() throws IOException, ExecutionException, ParseException, PlaylistException;

    public void invalidateCacheEntries();

    public void receiveTip(int tokens) throws IOException;

    /**
     * Determines the stream resolution for this model
     *
     * @param failFast
     *            If set to true, the method returns emmediately, even if the resolution is unknown. If
     *            the resolution is unknown, the array contains 0,0
     *
     * @return a tupel of width and height represented by an int[2]
     * @throws ExecutionException
     */
    public int[] getStreamResolution(boolean failFast) throws ExecutionException;

    public boolean follow() throws IOException;

    public boolean unfollow() throws IOException;

    public void setSite(Site site);

    public Site getSite();

    public void writeSiteSpecificData(JsonWriter writer) throws IOException;

    public void readSiteSpecificData(JsonReader reader) throws IOException;

    public boolean isSuspended();

    public void setSuspended(boolean suspended);



}