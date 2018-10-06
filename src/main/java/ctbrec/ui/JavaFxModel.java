package ctbrec.ui;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import ctbrec.Model;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * Just a wrapper for Model, which augments it with JavaFX value binding properties, so that UI widgets get updated proeprly
 */
public class JavaFxModel extends Model {
    private transient BooleanProperty onlineProperty = new SimpleBooleanProperty();

    private Model delegate;

    public JavaFxModel(Model delegate) {
        this.delegate = delegate;
        try {
            onlineProperty.set(Objects.equals("public", delegate.getOnlineState(true)));
        } catch (IOException | ExecutionException e) {}
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
}
