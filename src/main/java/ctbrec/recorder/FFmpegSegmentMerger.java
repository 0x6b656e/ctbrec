package ctbrec.recorder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iheartradio.m3u8.Encoding;
import com.iheartradio.m3u8.Format;
import com.iheartradio.m3u8.ParseException;
import com.iheartradio.m3u8.PlaylistException;
import com.iheartradio.m3u8.PlaylistParser;
import com.iheartradio.m3u8.data.MediaPlaylist;
import com.iheartradio.m3u8.data.Playlist;
import com.iheartradio.m3u8.data.TrackData;

import ctbrec.DevNull;

public class FFmpegSegmentMerger implements SegmentMerger {
    private static final transient Logger LOG = LoggerFactory.getLogger(FFmpegSegmentMerger.class);

    private int lastPercentage;

    @Override
    public void merge(File recDir, File targetFile) throws IOException, ParseException, PlaylistException {
        if (targetFile.exists()) {
            return;
        }

        File playlistFile = new File(recDir, "playlist.m3u8");
        if (!playlistFile.exists()) {
            LOG.warn("Couldn't merge segments. Playlist {} does not exist", playlistFile);
            return;
        }

        Runtime rt = Runtime.getRuntime();
        Process ffmpeg = rt.exec("ffmpeg -y -i - -c:v copy -c:a copy -f mp4 " + targetFile.getCanonicalPath());

        // create threads, which read stdout and stderr of the player process. these are needed,
        // because otherwise the internal buffer for these streams fill up and block the process
        Thread std = new Thread(new StreamRedirectThread(ffmpeg.getInputStream(), new DevNull()));
        std.setName("FFmpeg stdout pipe");
        std.setDaemon(true);
        std.start();
        Thread err = new Thread(new StreamRedirectThread(ffmpeg.getErrorStream(), new DevNull()));
        err.setName("FFmpeg stderr pipe");
        err.setDaemon(true);
        err.start();

        try (FileInputStream fin = new FileInputStream(playlistFile); OutputStream ffmpegStdin = ffmpeg.getOutputStream()) {
            PlaylistParser parser = new PlaylistParser(fin, Format.EXT_M3U, Encoding.UTF_8);
            Playlist playlist = parser.parse();
            MediaPlaylist mediaPlaylist = playlist.getMediaPlaylist();
            List<TrackData> tracks = mediaPlaylist.getTracks();
            for (int i = 0; i < tracks.size(); i++) {
                TrackData trackData = tracks.get(i);
                File segment = new File(recDir, trackData.getUri());
                if (segment.exists()) {
                    try (FileInputStream segmentStream = new FileInputStream(segment)) {
                        int length = -1;
                        byte[] b = new byte[1024 * 1024];
                        while ((length = segmentStream.read(b)) >= 0) {
                            ffmpegStdin.write(b, 0, length);
                        }
                        lastPercentage = (int) (i * 100.0 / tracks.size());
                        Thread.sleep(10);
                    } catch (Exception e) {
                        LOG.error("Couldn't append segment {} to merged file {}", segment.getName(), targetFile.getName(), e);
                    }
                }
            }
        }

        try {
            int exitCode = ffmpeg.waitFor();
            if(exitCode != 0) {
                throw new IOException("FFmpeg exited with code " + exitCode);
            } else {
                LOG.debug("FFmpeg finished.");
            }
        } catch (InterruptedException e) {
            LOG.error("Interrupted while waiting for FFmpeg to finish");
        }
    }

    @Override
    public int getProgress() {
        return lastPercentage;
    }
}
