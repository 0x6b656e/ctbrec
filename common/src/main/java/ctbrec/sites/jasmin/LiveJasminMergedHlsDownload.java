package ctbrec.sites.jasmin;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iheartradio.m3u8.ParseException;
import com.iheartradio.m3u8.PlaylistException;

import ctbrec.io.HttpClient;
import ctbrec.recorder.download.MergedHlsDownload;

public class LiveJasminMergedHlsDownload extends MergedHlsDownload {

    private static final transient Logger LOG = LoggerFactory.getLogger(LiveJasminMergedHlsDownload.class);
    private long lastMasterPlaylistUpdate = 0;
    private String segmentUrl;

    public LiveJasminMergedHlsDownload(HttpClient client) {
        super(client);
    }

    @Override
    protected SegmentPlaylist getNextSegments(String segments) throws IOException, ParseException, PlaylistException {
        if(this.segmentUrl == null) {
            this.segmentUrl = segments;
        }
        SegmentPlaylist playlist = super.getNextSegments(segmentUrl);
        long now = System.currentTimeMillis();
        if( (now - lastMasterPlaylistUpdate) > TimeUnit.SECONDS.toMillis(60)) {
            super.downloadThreadPool.submit(this::updatePlaylistUrl);
            lastMasterPlaylistUpdate = now;
        }
        return playlist;
    }

    private void updatePlaylistUrl() {
        try {
            LOG.debug("Updating segment playlist URL for {}", getModel());
            segmentUrl = getSegmentPlaylistUrl(getModel());
        } catch (IOException | ExecutionException | ParseException | PlaylistException e) {
            LOG.error("Couldn't update segment playlist url. This might cause a premature download termination", e);
        }
    }
}
