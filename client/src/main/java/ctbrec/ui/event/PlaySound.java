package ctbrec.ui.event;

import java.io.File;
import java.net.URL;

import ctbrec.event.Action;
import ctbrec.event.Event;
import ctbrec.event.EventHandlerConfiguration.ActionConfiguration;
import javafx.scene.media.AudioClip;

public class PlaySound extends Action {

    private URL url;

    public PlaySound() {}

    public PlaySound(URL url) {
        this.url = url;
        name = "play sound";
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
