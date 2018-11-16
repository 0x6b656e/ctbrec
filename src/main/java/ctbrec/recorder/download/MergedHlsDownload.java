package ctbrec.recorder.download;

import static java.nio.file.StandardOpenOption.*;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.taktik.mpegts.Streamer;
import org.taktik.mpegts.sinks.ByteChannelSink;
import org.taktik.mpegts.sinks.MTSSink;
import org.taktik.mpegts.sources.BlockingMultiMTSSource;
import org.taktik.mpegts.sources.InputStreamMTSSource;

import com.iheartradio.m3u8.ParseException;
import com.iheartradio.m3u8.PlaylistException;

import ctbrec.Config;
import ctbrec.Hmac;
import ctbrec.Model;
import ctbrec.Recording;
import ctbrec.io.HttpClient;
import ctbrec.io.HttpException;
import ctbrec.recorder.ProgressListener;
import okhttp3.Request;
import okhttp3.Response;

public class MergedHlsDownload extends AbstractHlsDownload {

    private static final transient Logger LOG = LoggerFactory.getLogger(MergedHlsDownload.class);
    private static final boolean IGNORE_CACHE = true;
    private BlockingMultiMTSSource multiSource;
    private Thread mergeThread;
    private Streamer streamer;
    private ZonedDateTime startTime;
    private Config config;
    private File targetFile;
    private DecimalFormat df = new DecimalFormat("00000");
    private int splitCounter = 0;

    public MergedHlsDownload(HttpClient client) {
        super(client);
    }

    public File getTargetFile() {
        return targetFile;
    }

    public void start(String segmentPlaylistUri, File targetFile, ProgressListener progressListener) throws IOException {
        try {
            running = true;
            super.startTime = Instant.now();
            downloadDir = targetFile.getParentFile().toPath();
            mergeThread = createMergeThread(targetFile, progressListener, false);
            LOG.debug("Merge thread started");
            mergeThread.start();
            if(Config.getInstance().getSettings().requireAuthentication) {
                URL u = new URL(segmentPlaylistUri);
                String path = u.getPath();
                byte[] key = Config.getInstance().getSettings().key;
                String hmac = Hmac.calculate(path, key);
                segmentPlaylistUri = segmentPlaylistUri + "?hmac=" + hmac;
            }
            LOG.debug("Downloading segments");
            downloadSegments(segmentPlaylistUri, false);
            LOG.debug("Waiting for merge thread to finish");
            mergeThread.join();
            LOG.debug("Merge thread to finished");
        } catch(ParseException e) {
            throw new IOException("Couldn't parse stream information", e);
        } catch(PlaylistException e) {
            throw new IOException("Couldn't parse HLS playlist", e);
        } catch (InterruptedException e) {
            throw new IOException("Couldn't wait for write thread to finish. Recording might be cut off", e);
        } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalStateException e) {
            throw new IOException("Couldn't add HMAC to playlist url", e);
        } finally {
            alive = false;
            streamer.stop();
            LOG.debug("Download terminated for {}", segmentPlaylistUri);
        }
    }

    @Override
    public void start(Model model, Config config) throws IOException {
        this.config = config;
        try {
            running = true;
            super.startTime = Instant.now();
            super.model = model;
            startTime = ZonedDateTime.now();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
            String startTime = sdf.format(new Date());
            Path modelDir = FileSystems.getDefault().getPath(config.getSettings().recordingsDir, model.getName());
            downloadDir = FileSystems.getDefault().getPath(modelDir.toString(), startTime);

            if(!model.isOnline(IGNORE_CACHE)) {
                throw new IOException(model.getName() +"'s room is not public");
            }

            targetFile = Recording.mergedFileFromDirectory(downloadDir.toFile());
            File target = targetFile;
            if(config.getSettings().splitRecordings > 0) {
                LOG.debug("Splitting recordings every {} seconds", config.getSettings().splitRecordings);
                target = new File(targetFile.getAbsolutePath().replaceAll("\\.ts", "-00000.ts"));
            }

            String segments = getSegmentPlaylistUrl(model);
            mergeThread = createMergeThread(target, null, true);
            mergeThread.start();
            if(segments != null) {
                downloadSegments(segments, true);
            } else {
                throw new IOException("Couldn't determine segments uri");
            }
        } catch(ParseException e) {
            throw new IOException("Couldn't parse stream information", e);
        } catch(PlaylistException e) {
            throw new IOException("Couldn't parse HLS playlist", e);
        } catch(EOFException e) {
            // end of playlist reached
            LOG.debug("Reached end of playlist for model {}", model);
        } catch(Exception e) {
            throw new IOException("Couldn't download segment", e);
        } finally {
            alive = false;
            if(streamer != null) {
                streamer.stop();
            }
            LOG.debug("Download for {} terminated", model);
        }
    }

    private void downloadSegments(String segmentPlaylistUri, boolean livestreamDownload) throws IOException, ParseException, PlaylistException {
        int lastSegment = 0;
        int nextSegment = 0;
        while(running) {
            try {
                SegmentPlaylist lsp = getNextSegments(segmentPlaylistUri);
                if(!livestreamDownload) {
                    multiSource.setTotalSegments(lsp.segments.size());
                }

                // download segments, which might have been skipped
                downloadMissedSegments(lsp, nextSegment);

                // download new segments
                downloadNewSegments(lsp, nextSegment);

                if(livestreamDownload) {
                    // split up the recording, if configured
                    splitRecording();

                    // wait some time until requesting the segment playlist again to not hammer the server
                    waitForNewSegments(lsp, lastSegment);

                    lastSegment = lsp.seq;
                    nextSegment = lastSegment + lsp.segments.size();
                } else {
                    break;
                }
            } catch(HttpException e) {
                if(e.getResponseCode() == 404) {
                    // playlist is gone -> model probably logged out
                    LOG.debug("Playlist not found. Assuming model went offline");
                    running = false;
                } else {
                    throw e;
                }
            }
        }
    }

    private void downloadMissedSegments(SegmentPlaylist lsp, int nextSegment) throws MalformedURLException {
        if(nextSegment > 0 && lsp.seq > nextSegment) {
            LOG.warn("Missed segments {} < {} in download for {}", nextSegment, lsp.seq, lsp.url);
            String first = lsp.segments.get(0);
            int seq = lsp.seq;
            for (int i = nextSegment; i < lsp.seq; i++) {
                URL segmentUrl = new URL(first.replaceAll(Integer.toString(seq), Integer.toString(i)));
                LOG.debug("Loading missed segment {} for model {}", i, lsp.url);
                byte[] segmentData;
                try {
                    segmentData = new SegmentDownload(segmentUrl, client).call();
                    writeSegment(segmentData);
                } catch (Exception e) {
                    LOG.error("Error while downloading segment {}", segmentUrl, e);
                }
            }
            // TODO switch to a lower bitrate/resolution ?!?
        }
    }

    private void downloadNewSegments(SegmentPlaylist lsp, int nextSegment) throws MalformedURLException {
        int skip = nextSegment - lsp.seq;
        for (String segment : lsp.segments) {
            if(skip > 0) {
                skip--;
            } else {
                URL segmentUrl = new URL(segment);
                try {
                    byte[] segmentData = new SegmentDownload(segmentUrl, client).call();
                    writeSegment(segmentData);
                } catch (Exception e) {
                    LOG.error("Error while downloading segment {}", segmentUrl, e);
                }
            }
        }
    }

    private void writeSegment(byte[] segmentData) throws InterruptedException {
        InputStream in = new ByteArrayInputStream(segmentData);
        InputStreamMTSSource source = InputStreamMTSSource.builder().setInputStream(in).build();
        multiSource.addSource(source);
    }

    private void splitRecording() {
        if(config.getSettings().splitRecordings > 0) {
            Duration recordingDuration = Duration.between(startTime, ZonedDateTime.now());
            long seconds = recordingDuration.getSeconds();
            if(seconds >= config.getSettings().splitRecordings) {
                streamer.stop();
                File target = new File(targetFile.getAbsolutePath().replaceAll("\\.ts", "-"+df.format(++splitCounter)+".ts"));
                mergeThread = createMergeThread(target, null, true);
                mergeThread.start();
                startTime = ZonedDateTime.now();
            }
        }
    }

    private void waitForNewSegments(SegmentPlaylist lsp, int lastSegment) {
        try {
            long wait = 0;
            if (lastSegment == lsp.seq) {
                // playlist didn't change -> wait for at least half the target duration
                wait = (long) lsp.targetDuration * 1000 / 2;
                LOG.trace("Playlist didn't change... waiting for {}ms", wait);
            } else {
                // playlist did change -> wait for at least last segment duration
                wait = 1;// (long) lsp.lastSegDuration * 1000;
                LOG.trace("Playlist changed... waiting for {}ms", wait);
            }
            Thread.sleep(wait);
        } catch (InterruptedException e) {
            if (running) {
                LOG.error("Couldn't sleep between segment downloads. This might mess up the download!");
            }
        }
    }

    @Override
    public void stop() {
        running = false;
        alive = false;
        streamer.stop();
        LOG.debug("Download stopped");
    }

    private Thread createMergeThread(File targetFile, ProgressListener listener, boolean liveStream) {
        Thread t = new Thread(() -> {
            multiSource = BlockingMultiMTSSource.builder()
                    .setFixContinuity(true)
                    .setProgressListener(listener)
                    .build();

            FileChannel channel = null;
            try {
                if (!Files.exists(downloadDir, LinkOption.NOFOLLOW_LINKS)) {
                    Files.createDirectories(downloadDir);
                }
                channel = FileChannel.open(targetFile.toPath(), CREATE, WRITE);
                MTSSink sink = ByteChannelSink.builder().setByteChannel(channel).build();

                streamer = Streamer.builder()
                        .setSource(multiSource)
                        .setSink(sink)
                        .setSleepingEnabled(liveStream)
                        .setBufferSize(10)
                        .build();

                // Start streaming
                streamer.stream();
                LOG.debug("Streamer finished");
            } catch (InterruptedException e) {
                if(running) {
                    LOG.error("Error while waiting for a download future", e);
                }
            }  catch(Exception e) {
                LOG.error("Error while saving stream to file", e);
            } finally {
                closeFile(channel);
                deleteEmptyRecording(targetFile);
            }
        });
        t.setName("Segment Merger Thread");
        t.setDaemon(true);
        return t;
    }

    private void deleteEmptyRecording(File targetFile) {
        try {
            if (targetFile.exists() && targetFile.length() == 0) {
                Files.delete(targetFile.toPath());
                Files.delete(targetFile.getParentFile().toPath());
            }
        } catch (IOException e) {
            LOG.error("Error while deleting empty recording {}", targetFile);
        }
    }

    private void closeFile(FileChannel channel) {
        try {
            if (channel != null) {
                channel.close();
            }
        } catch (IOException e) {
            LOG.error("Error while closing file channel", e);
        }
    }

    private static class SegmentDownload implements Callable<byte[]> {
        private URL url;
        private HttpClient client;

        public SegmentDownload(URL url, HttpClient client) {
            this.url = url;
            this.client = client;
        }

        @Override
        public byte[] call() throws Exception {
            LOG.trace("Downloading segment " + url.getFile());
            int maxTries = 3;
            for (int i = 1; i <= maxTries; i++) {
                try {
                    Request request = new Request.Builder().url(url).addHeader("connection", "keep-alive").build();
                    Response response = client.execute(request);
                    byte[] segment = response.body().bytes();
                    return segment;
                } catch(Exception e) {
                    if (i == maxTries) {
                        LOG.warn("Error while downloading segment. Segment {} finally failed", url.getFile());
                    } else {
                        LOG.warn("Error while downloading segment {} on try {}", url.getFile(), i);
                    }
                }
            }
            throw new IOException("Unable to download segment " + url.getFile() + " after " + maxTries + " tries");
        }
    }
}
