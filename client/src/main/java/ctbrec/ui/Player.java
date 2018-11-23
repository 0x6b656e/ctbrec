package ctbrec.ui;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.Hmac;
import ctbrec.Model;
import ctbrec.OS;
import ctbrec.Recording;
import ctbrec.io.DevNull;
import ctbrec.io.StreamRedirectThread;
import ctbrec.recorder.download.StreamSource;
import javafx.application.Platform;
import javafx.scene.control.Alert;

public class Player {
    private static final transient Logger LOG = LoggerFactory.getLogger(Player.class);
    private static PlayerThread playerThread;

    public static boolean play(String url) {
        boolean singlePlayer = Config.getInstance().getSettings().singlePlayer;
        try {
            if (singlePlayer && playerThread != null && playerThread.isRunning()) {
                playerThread.stopThread();
            }

            playerThread = new PlayerThread(url);
            return true;
        } catch (Exception e1) {
            LOG.error("Couldn't start player", e1);
            return false;
        }
    }

    public static boolean play(Recording rec) {
        boolean singlePlayer = Config.getInstance().getSettings().singlePlayer;
        try {
            if (singlePlayer && playerThread != null && playerThread.isRunning()) {
                playerThread.stopThread();
            }

            playerThread = new PlayerThread(rec);
            return true;
        } catch (Exception e1) {
            LOG.error("Couldn't start player", e1);
            return false;
        }
    }

    public static boolean play(Model model) {
        try {
            if(model.isOnline(true)) {
                boolean singlePlayer = Config.getInstance().getSettings().singlePlayer;
                if (singlePlayer && playerThread != null && playerThread.isRunning()) {
                    playerThread.stopThread();
                }
                List<StreamSource> sources = model.getStreamSources();
                Collections.sort(sources);
                StreamSource best = sources.get(sources.size()-1);
                LOG.debug("Playing {}", best.getMediaPlaylistUrl());
                return Player.play(best.getMediaPlaylistUrl());
            } else {
                Platform.runLater(() -> {
                    Alert alert = new AutosizeAlert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Room not public");
                    alert.setHeaderText("Room is currently not public");
                    alert.showAndWait();
                });
                return false;
            }
        } catch (Exception e1) {
            LOG.error("Couldn't get stream information for model {}", model, e1);
            Platform.runLater(() -> {
                Alert alert = new AutosizeAlert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Couldn't determine stream URL");
                alert.setContentText(e1.getLocalizedMessage());
                alert.showAndWait();
            });
            return false;
        }
    }


    public static void stop() {
        if (playerThread != null) {
            playerThread.stopThread();
        }
    }


    private static class PlayerThread extends Thread {
        private boolean running = false;
        private Process playerProcess;
        private String url;
        private Recording rec;

        PlayerThread(String url) {
            this.url = url;
            setName(getClass().getName());
            start();
        }

        PlayerThread(Recording rec) {
            this.rec = rec;
            setName(getClass().getName());
            start();
        }

        @Override
        public void run() {
            running = true;
            Runtime rt = Runtime.getRuntime();
            try {
                if (Config.getInstance().getSettings().localRecording && rec != null) {
                    File file = new File(Config.getInstance().getSettings().recordingsDir, rec.getPath());
                    playerProcess = rt.exec(Config.getInstance().getSettings().mediaPlayer + " " + file, OS.getEnvironment(), file.getParentFile());
                } else {
                    if(Config.getInstance().getSettings().requireAuthentication) {
                        URL u = new URL(url);
                        String path = u.getPath();
                        byte[] key = Config.getInstance().getSettings().key;
                        String hmac = Hmac.calculate(path, key);
                        url = url + "?hmac=" + hmac;
                    }
                    LOG.debug("Playing {}", url);
                    playerProcess = rt.exec(Config.getInstance().getSettings().mediaPlayer + " " + url);
                }

                // create threads, which read stdout and stderr of the player process. these are needed,
                // because otherwise the internal buffer for these streams fill up and block the process
                Thread std = new Thread(new StreamRedirectThread(playerProcess.getInputStream(), new DevNull()));
                std.setName("Player stdout pipe");
                std.setDaemon(true);
                std.start();
                Thread err = new Thread(new StreamRedirectThread(playerProcess.getErrorStream(), new DevNull()));
                err.setName("Player stderr pipe");
                err.setDaemon(true);
                err.start();

                playerProcess.waitFor();
                LOG.debug("Media player finished.");
            } catch (Exception e) {
                LOG.error("Error in player thread", e);
            }
            running = false;
        }

        public boolean isRunning() {
            return running;
        }

        public void stopThread() {
            if (playerProcess != null) {
                playerProcess.destroy();
            }
        }
    }
}
