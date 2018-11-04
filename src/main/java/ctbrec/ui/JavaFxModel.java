package ctbrec.ui;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.iheartradio.m3u8.ParseException;
import com.iheartradio.m3u8.PlaylistException;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import ctbrec.AbstractModel;
import ctbrec.Model;
import ctbrec.recorder.download.StreamSource;
import ctbrec.sites.Site;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * Just a wrapper for Model, which augments it with JavaFX value binding properties, so that UI widgets get updated proeprly
 */
public class JavaFxModel extends AbstractModel {
    private transient BooleanProperty onlineProperty = new SimpleBooleanProperty();
    private Model delegate;

    public JavaFxModel(Model delegate) {
        this.delegate = delegate;
        try {
            onlineProperty.set(delegate.isOnline());
        } catch (IOException | ExecutionException | InterruptedException e) {}
    }

    @Override
    public String getUrl() {
        return delegate.getUrl();
    }

    @Override
    public void setUrl(String url) {
        delegate.setUrl(url);
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public void setName(String name) {
        delegate.setName(name);
    }

    @Override
    public String getPreview() {
        return delegate.getPreview();
    }

    @Override
    public void setPreview(String preview) {
        delegate.setPreview(preview);
    }

    @Override
    public List<String> getTags() {
        return delegate.getTags();
    }

    @Override
    public void setTags(List<String> tags) {
        delegate.setTags(tags);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    public BooleanProperty getOnlineProperty() {
        return onlineProperty;
    }

    Model getDelegate() {
        return delegate;
    }

    @Override
    public boolean isOnline() throws IOException, ExecutionException, InterruptedException {
        return delegate.isOnline();
    }

    @Override
    public boolean isOnline(boolean ignoreCache) throws IOException, ExecutionException, InterruptedException {
        return delegate.isOnline(ignoreCache);
    }

    @Override
    public String getOnlineState(boolean failFast) throws IOException, ExecutionException {
        return delegate.getOnlineState(failFast);
    }

    @Override
    public List<StreamSource> getStreamSources() throws IOException, ExecutionException, ParseException, PlaylistException {
        return delegate.getStreamSources();
    }

    @Override
    public void invalidateCacheEntries() {
        delegate.invalidateCacheEntries();
    }

    @Override
    public void receiveTip(int tokens) throws IOException {
        delegate.receiveTip(tokens);
    }

    @Override
    public int[] getStreamResolution(boolean b) throws ExecutionException {
        return delegate.getStreamResolution(b);
    }

    @Override
    public boolean follow() throws IOException {
        return delegate.follow();
    }

    @Override
    public boolean unfollow() throws IOException {
        return delegate.unfollow();
    }

    @Override
    public void setSite(Site site) {
        delegate.setSite(site);
    }

    @Override
    public Site getSite() {
        return delegate.getSite();
    }

    @Override
    public void readSiteSpecificData(JsonReader reader) throws IOException {
        delegate.readSiteSpecificData(reader);
    }

    @Override
    public void writeSiteSpecificData(JsonWriter writer) throws IOException {
        delegate.writeSiteSpecificData(writer);
    }
}
