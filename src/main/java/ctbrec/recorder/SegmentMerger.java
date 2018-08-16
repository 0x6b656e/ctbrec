package ctbrec.recorder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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


public class SegmentMerger {
    private static final transient Logger LOG = LoggerFactory.getLogger(SegmentMerger.class);

    private int lastPercentage;

    public void merge(File recDir, File targetFile) throws IOException, ParseException, PlaylistException {
        if (targetFile.exists()) {
            return;
        }

        File playlistFile = new File(recDir, "playlist.m3u8");
        if(!playlistFile.exists()) {
            LOG.warn("Couldn't merge segments. Playlist {} does not exist", playlistFile);
            return;
        }

        try (FileInputStream fin = new FileInputStream(playlistFile); FileOutputStream fos = new FileOutputStream(targetFile)) {
            PlaylistParser parser = new PlaylistParser(fin, Format.EXT_M3U, Encoding.UTF_8);
            Playlist playlist = parser.parse();
            MediaPlaylist mediaPlaylist = playlist.getMediaPlaylist();
            List<TrackData> tracks = mediaPlaylist.getTracks();
            for (int i = 0; i < tracks.size(); i++) {
                TrackData trackData = tracks.get(i);
                File segment = new File(recDir, trackData.getUri());
                if(segment.exists()) {
                    try (FileInputStream segmentStream = new FileInputStream(segment)) {
                        int length = -1;
                        byte[] b = new byte[1024 * 1024];
                        while ((length = segmentStream.read(b)) >= 0) {
                            fos.write(b, 0, length);
                        }
                        lastPercentage = (int) (i * 100.0 / tracks.size());
                    } catch(Exception e) {
                        LOG.error("Couldn't append segment {} to merged file {}", segment.getName(), targetFile.getName(), e);
                    }
                }
            }
        }
    }

    public int getProgress() {
        return lastPercentage;
    }
}
