package ctbrec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import ctbrec.sites.Site;

public abstract class AbstractModel implements Model {

    private String url;
    private String name;
    private String preview;
    private String description;
    private List<String> tags = new ArrayList<>();
    private int streamUrlIndex = -1;
    private boolean suspended = false;
    protected Site site;

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
    public void readSiteSpecificData(JsonReader reader) throws IOException {
        // noop default implementation, can be overriden by concrete models
    }

    @Override
    public void writeSiteSpecificData(JsonWriter writer) throws IOException {
        // noop default implementation, can be overriden by concrete models
    }

    @Override
    public boolean isSuspended() {
        return suspended;
    }

    @Override
    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
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

    @Override
    public void setSite(Site site) {
        this.site = site;
    }

    @Override
    public Site getSite() {
        return site;
    }
}
