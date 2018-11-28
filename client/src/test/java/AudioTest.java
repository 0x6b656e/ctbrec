import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.media.AudioClip;
import javafx.stage.Stage;

public class AudioTest extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        AudioClip clip = new AudioClip("file:///tmp/Oxygen-Im-Highlight-Msg.mp3");
        clip.cycleCountProperty().set(3);
        clip.play();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
