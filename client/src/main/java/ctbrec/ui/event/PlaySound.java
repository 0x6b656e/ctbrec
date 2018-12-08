package ctbrec.ui.event;

import java.net.URL;

import ctbrec.event.Action;
import ctbrec.event.Event;
import javafx.scene.media.AudioClip;

public class PlaySound extends Action {

    private URL url;

    public PlaySound(URL url) {
        this.url = url;
        name = "play sound";
    }

    @Override
    public void accept(Event evt) {
        AudioClip clip = new AudioClip(url.toString());
        clip.play();
    }
}
