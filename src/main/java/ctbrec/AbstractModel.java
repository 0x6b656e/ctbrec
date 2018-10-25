package ctbrec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.iheartradio.m3u8.ParseException;
import com.iheartradio.m3u8.PlaylistException;

import ctbrec.recorder.download.StreamSource;

public abstract class AbstractModel implements Model {

    private String url;
    private String name;
    private String preview;
    private String description;
    private List<String> tags = new ArrayList<>();
    private int streamUrlIndex = -1;

    @Override
    public boolean isOnline() throws IOException, ExecutionException, InterruptedException {
        return isOnline(false);
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getPreview() {
        return preview;
    }

    @Override
    public void setPreview(String preview) {
        this.preview = preview;
    }

    @Override
    public List<String> getTags() {
        return tags;
    }

    @Override
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public int getStreamUrlIndex() {
        return streamUrlIndex;
    }

    @Override
    public void setStreamUrlIndex(int streamUrlIndex) {
        this.streamUrlIndex = streamUrlIndex;
    }

    @Override
    public String getSegmentPlaylistUrl() throws IOException, ExecutionException, ParseException, PlaylistException {
        List<StreamSource> streamSources = getStreamSources();
        String url = null;
        if(getStreamUrlIndex() >= 0 && getStreamUrlIndex() < streamSources.size()) {
            url = streamSources.get(getStreamUrlIndex()).getMediaPlaylistUrl();
        } else {
            Collections.sort(streamSources);
            url = streamSources.get(streamSources.size()-1).getMediaPlaylistUrl();
        }
        return url;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        result = prime * result + ((getUrl() == null) ? 0 : getUrl().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Model))
            return false;
        Model other = (Model) obj;
        if (getName() == null) {
            if (other.getName() != null)
                return false;
        } else if (!getName().equals(other.getName()))
            return false;
        if (getUrl() == null) {
            if (other.getUrl() != null)
                return false;
        } else if (!getUrl().equals(other.getUrl()))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return getName();
    }

}
