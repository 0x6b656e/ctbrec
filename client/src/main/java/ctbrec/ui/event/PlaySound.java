package ctbrec.ui.event;

import java.io.File;
import java.net.URL;

import ctbrec.event.Action;
import ctbrec.event.Event;
import ctbrec.event.EventHandlerConfiguration.ActionConfiguration;
import javafx.scene.media.AudioClip;

public class PlaySound extends Action {

    private URL url;

    public PlaySound() {
        name = "play sound";
    }

    public PlaySound(URL url) {
        this();
        this.url = url;
    }

    @Override
    public void accept(Event evt) {
        AudioClip clip = new AudioClip(url.toString());
        clip.play();
    }

    @Override
    public void configure(ActionConfiguration config) throws Exception {
        File file = new File((String) config.getConfiguration().get("file"));
        url = file.toURI().toURL();
    }
}
