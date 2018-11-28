import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

public class HlsTest extends Application {
    // media = new Media("http://localhost:3202/hls/sun_shine_baby/2018-11-28_20-43/playlist.m3u8");

    private static final String MEDIA_URL = "http://localhost:3202/hls/sun_shine_baby/2018-11-28_20-43/playlist.m3u8";

    private Media media;
    private MediaPlayer mediaPlayer;
    private MediaControl mediaControl;

    public Parent createContent() {
        media = new Media(MEDIA_URL);
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setOnError(()-> {
            mediaPlayer.getError().printStackTrace(System.err);
        });
        mediaControl = new MediaControl(mediaPlayer);
        mediaControl.setMinSize(480, 280);
        mediaControl.setPrefSize(480, 280);
        mediaControl.setMaxSize(480, 280);
        return mediaControl;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setScene(new Scene(createContent()));
        primaryStage.show();
    }

    @Override
    public void stop() {
        mediaPlayer.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
