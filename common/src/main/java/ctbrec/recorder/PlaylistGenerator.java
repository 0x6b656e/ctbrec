package ctbrec.recorder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iheartradio.m3u8.Encoding;
import com.iheartradio.m3u8.Format;
import com.iheartradio.m3u8.ParseException;
import com.iheartradio.m3u8.ParsingMode;
import com.iheartradio.m3u8.PlaylistException;
import com.iheartradio.m3u8.PlaylistParser;
import com.iheartradio.m3u8.PlaylistWriter;
import com.iheartradio.m3u8.data.MediaPlaylist;
import com.iheartradio.m3u8.data.Playlist;
import com.iheartradio.m3u8.data.PlaylistType;
import com.iheartradio.m3u8.data.TrackData;
import com.iheartradio.m3u8.data.TrackInfo;

import ctbrec.MpegUtil;


public class PlaylistGenerator {
    private static final transient Logger LOG = LoggerFactory.getLogger(PlaylistGenerator.class);

    private int lastPercentage;
    private List<ProgressListener> listeners = new ArrayList<>();

    public File generate(File directory) throws IOException, ParseException, PlaylistException {
        LOG.debug("Starting playlist generation for {}", directory);
        // get a list of all ts files and sort them by sequence
        File[] files = directory.listFiles((f) -> f.getName().endsWith(".ts"));
        if(files == null || files.length == 0) {
            LOG.debug("{} is empty. Not going to generate a playlist", directory);
            return null;
        }

        Arrays.sort(files, (f1, f2) -> {
            String n1 = f1.getName();
            String n2 = f2.getName();
            return n1.compareTo(n2);
        });

        // create a track containing all files
        List<TrackData> track = new ArrayList<>();
        int total = files.length;
        int done = 0;
        for (File file : files) {
            try {
                track.add(new TrackData.Builder()
                        .withUri(file.getName())
                        .withTrackInfo(new TrackInfo((float) MpegUtil.getFileDuration(file), file.getName()))
                        .build());
            } catch(Exception e) {
                LOG.warn("Couldn't determine duration for {}. Skipping this file.", file.getName());
                file.renameTo(new File(directory, file.getName()+".corrupt"));
            }
            done++;
            double percentage = (double)done / (double) total;
            updateProgressListeners(percentage);
        }

        // create a media playlist
        float targetDuration = getAvgDuration(track);
        MediaPlaylist playlist = new MediaPlaylist.Builder()
                .withPlaylistType(PlaylistType.VOD)
                .withMediaSequenceNumber(0)
                .withTargetDuration((int) targetDuration)
                .withTracks(track).build();

        // create a master playlist containing the media playlist
        Playlist master = new Playlist.Builder()
                .withCompatibilityVersion(4)
                .withExtended(true)
                .withMediaPlaylist(playlist)
                .build();

        // write the playlist to a file
        File output = new File(directory, "playlist.m3u8");
        try(FileOutputStream fos = new FileOutputStream(output)) {
            PlaylistWriter writer = new PlaylistWriter.Builder()
                    .withFormat(Format.EXT_M3U)
                    .withEncoding(Encoding.UTF_8)
                    .withOutputStream(fos)
                    .build();
            writer.write(master);
            LOG.debug("Finished playlist generation for {}", directory);
        }
        return output;
    }

    private void updateProgressListeners(double percentage) {
        int p = (int) (percentage*100);
        if(p > lastPercentage) {
            for (ProgressListener progressListener : listeners) {
                progressListener.update(p);
            }
            lastPercentage = p;
        }
    }

    private float getAvgDuration(List<TrackData> track) {
        float targetDuration = 0;
        for (TrackData trackData : track) {
            targetDuration += trackData.getTrackInfo().duration;
        }
        targetDuration /= track.size();
        return targetDuration;
    }

    public void addProgressListener(ProgressListener l) {
        listeners.add(l);
    }

    public int getProgress() {
        return lastPercentage;
    }

    public void validate(File recDir) throws IOException, ParseException, PlaylistException {
        File playlist = new File(recDir, "playlist.m3u8");
        if(playlist.exists()) {
            PlaylistParser playlistParser = new PlaylistParser(new FileInputStream(playlist), Format.EXT_M3U, Encoding.UTF_8, ParsingMode.LENIENT);
            Playlist m3u = playlistParser.parse();
            MediaPlaylist mediaPlaylist = m3u.getMediaPlaylist();
            int playlistSize = mediaPlaylist.getTracks().size();
            File[] segments = recDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".ts");
                }
            });
            if(segments.length == 0) {
                throw new InvalidPlaylistException("No segments found. Playlist is empty");
            } else if(segments.length != playlistSize) {
                throw new InvalidPlaylistException("Playlist size and amount of segments differ");
            } else {
                LOG.debug("Generated playlist looks good");
            }
        } else {
            throw new FileNotFoundException(playlist.getAbsolutePath() + " does not exist");
        }
    }

    public static class InvalidPlaylistException extends RuntimeException {
        public InvalidPlaylistException(String msg) {
            super(msg);
        }
    }
}
