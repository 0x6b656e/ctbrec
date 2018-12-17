package ctbrec.recorder.download;

import static ctbrec.Recording.State.*;
import static java.nio.file.StandardOpenOption.*;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
import ctbrec.event.EventBusHolder;
import ctbrec.event.RecordingStateChangedEvent;
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
    private ZonedDateTime splitRecStartTime;
    private Config config;
    private File targetFile;
    private BlockingQueue<Runnable> downloadQueue = new LinkedBlockingQueue<>(50);
    private ExecutorService downloadThreadPool = new ThreadPoolExecutor(5, 5, 2, TimeUnit.MINUTES, downloadQueue);
    private FileChannel fileChannel = null;
    private Object downloadFinished = new Object();

    public MergedHlsDownload(HttpClient client) {
        super(client);
    }

    @Override
    public File getTarget() {
        return targetFile;
    }

    public void start(String segmentPlaylistUri, File targetFile, ProgressListener progressListener) throws IOException {
        try {
            running = true;
            super.startTime = Instant.now();
            splitRecStartTime = ZonedDateTime.now();
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
            LOG.debug("Merge thread finished");
        } catch(ParseException e) {
            throw new IOException("Couldn't parse stream information", e);
        } catch(PlaylistException e) {
            throw new IOException("Couldn't parse HLS playlist", e);
        } catch (InterruptedException e) {
            throw new IOException("Couldn't wait for write thread to finish. Recording might be cut off", e);
        } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalStateException e) {
            throw new IOException("Couldn't add HMAC to playlist url", e);
        } finally {
            try {
                streamer.stop();
            } catch(Exception e) {
                LOG.error("Couldn't stop streamer", e);
            }
            downloadThreadPool.shutdown();
            try {
                LOG.debug("Waiting for last segments for {}", model);
                downloadThreadPool.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {}
            alive = false;
            synchronized (downloadFinished) {
                downloadFinished.notifyAll();
            }
            LOG.debug("Download terminated for {}", segmentPlaylistUri);
        }
    }

    @Override
    public void start(Model model, Config config) throws IOException {
        this.config = config;
        try {
            if(!model.isOnline(IGNORE_CACHE)) {
                throw new IOException(model.getName() +"'s room is not public");
            }

            running = true;
            super.startTime = Instant.now();
            splitRecStartTime = ZonedDateTime.now();
            super.model = model;
            targetFile = Config.getInstance().getFileForRecording(model);

            // let the world know, that we are recording now
            RecordingStateChangedEvent evt = new RecordingStateChangedEvent(getTarget(), RECORDING, model, getStartTime());
            EventBusHolder.BUS.post(evt);

            String segments = getSegmentPlaylistUrl(model);
            mergeThread = createMergeThread(targetFile, null, true);
            mergeThread.start();
            if(segments != null) {
                downloadSegments(segments, true);
                if(config.getSettings().splitRecordings > 0) {
                    LOG.debug("Splitting recordings every {} seconds", config.getSettings().splitRecordings);
                }
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
            if(streamer != null) {
                try {
                    streamer.stop();
                } catch(Exception e) {
                    LOG.error("Couldn't stop streamer", e);
                }
            }
            downloadThreadPool.shutdown();
            try {
                LOG.debug("Waiting for last segments for {}", model);
                downloadThreadPool.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {}
            alive = false;
            synchronized (downloadFinished) {
                downloadFinished.notifyAll();
            }
            LOG.debug("Download for {} terminated", model);
        }
    }

    private void downloadSegments(String segmentPlaylistUri, boolean livestreamDownload) throws IOException, ParseException, PlaylistException {
        int lastSegment = 0;
        int nextSegment = 0;
        long playlistNotFoundFirstEncounter = -1;
        while(running) {
            try {
                if(playlistNotFoundFirstEncounter != -1) {
                    LOG.debug("Downloading playlist {}", segmentPlaylistUri);
                }
                SegmentPlaylist lsp = getNextSegments(segmentPlaylistUri);
                playlistNotFoundFirstEncounter = -1;
                if(!livestreamDownload) {
                    multiSource.setTotalSegments(lsp.segments.size());
                }

                // download new segments
                long downloadStart = System.currentTimeMillis();
                if(livestreamDownload) {
                    downloadNewSegments(lsp, nextSegment);
                } else {
                    downloadRecording(lsp);
                }
                long downloadTookMillis = System.currentTimeMillis() - downloadStart;

                // download segments, which might have been skipped
                //downloadMissedSegments(lsp, nextSegment);
                if(nextSegment > 0 && lsp.seq > nextSegment) {
                    LOG.warn("Missed segments {} < {} in download for {}. Download took {}ms. Playlist is {}sec", nextSegment, lsp.seq, lsp.url, downloadTookMillis, lsp.totalDuration);
                }

                if(livestreamDownload) {
                    // split up the recording, if configured
                    splitRecording();

                    // wait some time until requesting the segment playlist again to not hammer the server
                    waitForNewSegments(lsp, lastSegment, downloadTookMillis);

                    lastSegment = lsp.seq;
                    nextSegment = lastSegment + lsp.segments.size();
                } else {
                    break;
                }
            } catch(Exception e) {
                if(model != null) {
                    LOG.info("Unexpected error while downloading {}", model.getName(), e);
                } else {
                    LOG.info("Unexpected error while downloading", e);
                }
                running = false;
            }
        }
    }

    private void downloadRecording(SegmentPlaylist lsp) throws IOException, InterruptedException {
        for (String segment : lsp.segments) {
            URL segmentUrl = new URL(segment);
            SegmentDownload segmentDownload = new SegmentDownload(segmentUrl, client);
            byte[] segmentData = segmentDownload.call();
            writeSegment(segmentData);
        }
    }

    private void downloadNewSegments(SegmentPlaylist lsp, int nextSegment) throws MalformedURLException, MissingSegmentException, ExecutionException, HttpException {
        int skip = nextSegment - lsp.seq;
        if(lsp.segments.isEmpty()) {
            LOG.debug("Empty playlist: {}", lsp.url);
        }

        // add segments to download threadpool
        Queue<Future<byte[]>> downloads = new LinkedList<>();
        if(downloadQueue.remainingCapacity() == 0) {
            LOG.warn("Download to slow for this stream. Download queue is full. Skipping segment");
        } else {
            for (String segment : lsp.segments) {
                if(!running) {
                    break;
                }
                if(skip > 0) {
                    skip--;
                } else {
                    URL segmentUrl = new URL(segment);
                    Future<byte[]> download = downloadThreadPool.submit(new SegmentDownload(segmentUrl, client));
                    downloads.add(download);
                }
            }
        }

        // get completed downloads and write them to the file
        // TODO it might be a good idea to do this in a separate thread, so that the main download loop isn't blocked
        writeFinishedSegments(downloads);
    }

    private void writeFinishedSegments(Queue<Future<byte[]>> downloads) throws ExecutionException, HttpException {
        for (Future<byte[]> downloadFuture : downloads) {
            try {
                byte[] segmentData = downloadFuture.get();
                writeSegment(segmentData);
            } catch (InterruptedException e) {
                LOG.error("Error while downloading segment", e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if(cause instanceof MissingSegmentException) {
                    if(model != null && !isModelOnline()) {
                        LOG.debug("Error while downloading segment, because model {} is offline. Stopping now", model.getName());
                        running = false;
                    } else {
                        LOG.debug("Segment not available, but model {} still online. Going on", model.getName());
                    }
                } else if(cause instanceof HttpException) {
                    HttpException he = (HttpException) cause;
                    if(model != null && !isModelOnline()) {
                        LOG.debug("Error {} while downloading segment, because model {} is offline. Stopping now", he.getResponseCode(), model.getName());
                        running = false;
                    } else {
                        if(he.getResponseCode() == 404) {
                            LOG.info("Playlist for {} not found [HTTP 404]. Stopping now", model.getName());
                            running = false;
                        } else if(he.getResponseCode() == 403) {
                            LOG.info("Playlist for {} not accessible [HTTP 403]. Stopping now", model.getName());
                            running = false;
                        } else {
                            throw he;
                        }
                    }
                } else {
                    throw e;
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
            Duration recordingDuration = Duration.between(splitRecStartTime, ZonedDateTime.now());
            long seconds = recordingDuration.getSeconds();
            if(seconds >= config.getSettings().splitRecordings) {
                try {
                    targetFile = Config.getInstance().getFileForRecording(model);
                    LOG.debug("Switching to file {}", targetFile.getAbsolutePath());
                    fileChannel = FileChannel.open(targetFile.toPath(), CREATE, WRITE);
                    MTSSink sink = ByteChannelSink.builder().setByteChannel(fileChannel).build();
                    streamer.switchSink(sink);
                    splitRecStartTime = ZonedDateTime.now();
                } catch (IOException e) {
                    LOG.error("Error while splitting recording", e);
                    running = false;
                }
            }
        }
    }

    private void waitForNewSegments(SegmentPlaylist lsp, int lastSegment, long downloadTookMillis) {
        try {
            long wait = 0;
            if (lastSegment == lsp.seq) {
                int timeLeftMillis = (int)(lsp.totalDuration * 1000 - downloadTookMillis);
                if(timeLeftMillis < 3000) { // we have less than 3 seconds to get the new playlist and start downloading it
                    wait = 1;
                } else {
                    // wait a second to be nice to the server (don't hammer it with requests)
                    // 1 second seems to be a good compromise. every other calculation resulted in more missing segments
                    wait = 1000;
                }
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
        if(streamer != null) {
            streamer.stop();
        }
        try {
            synchronized (downloadFinished) {
                downloadFinished.wait();
            }
        } catch (InterruptedException e) {
            LOG.error("Couldn't wait for download to finish", e);
        }
        LOG.debug("Download stopped");
    }

    private Thread createMergeThread(File targetFile, ProgressListener listener, boolean liveStream) {
        Thread t = new Thread(() -> {
            multiSource = BlockingMultiMTSSource.builder()
                    .setFixContinuity(true)
                    .setProgressListener(listener)
                    .build();

            try {
                Path downloadDir = targetFile.getParentFile().toPath();
                if (!Files.exists(downloadDir, LinkOption.NOFOLLOW_LINKS)) {
                    Files.createDirectories(downloadDir);
                }
                fileChannel = FileChannel.open(targetFile.toPath(), CREATE, WRITE);
                MTSSink sink = ByteChannelSink.builder().setByteChannel(fileChannel).build();

                streamer = Streamer.builder()
                        .setSource(multiSource)
                        .setSink(sink)
                        .setSleepingEnabled(liveStream)
                        .setBufferSize(10)
                        .setName(Optional.ofNullable(model).map(m -> m.getName()).orElse(""))
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
                deleteEmptyRecording(targetFile);
                running = false;
                closeFile(fileChannel);
            }
        });
        if(model != null) {
            t.setName("Segment Merger Thread [" + model.getName() + "]");
        } else {
            t.setName("Segment Merger Thread");
        }
        t.setDaemon(true);
        return t;
    }

    private void deleteEmptyRecording(File targetFile) {
        try {
            if (targetFile.exists() && targetFile.length() == 0) {
                Files.delete(targetFile.toPath());
                Files.delete(targetFile.getParentFile().toPath());
            }
        } catch (Exception e) {
            LOG.error("Error while deleting empty recording {}", targetFile);
        }
    }

    private void closeFile(FileChannel channel) {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
        } catch (Exception e) {
            LOG.error("Error while closing file channel", e);
        }
    }

    private class SegmentDownload implements Callable<byte[]> {
        private URL url;
        private HttpClient client;

        public SegmentDownload(URL url, HttpClient client) {
            this.url = url;
            this.client = client;
        }

        @Override
        public byte[] call() throws IOException {
            LOG.trace("Downloading segment " + url.getFile());
            int maxTries = 3;
            for (int i = 1; i <= maxTries && running; i++) {
                Request request = new Request.Builder().url(url).addHeader("connection", "keep-alive").build();
                try (Response response = client.execute(request)) {
                    if(response.isSuccessful()) {
                        byte[] segment = response.body().bytes();
                        return segment;
                    } else {
                        throw new HttpException(response.code(), response.message());
                    }
                } catch(Exception e) {
                    if (i == maxTries) {
                        LOG.warn("Error while downloading segment. Segment {} finally failed", url.getFile());
                    } else {
                        LOG.warn("Error while downloading segment {} on try {}", url.getFile(), i, e);
                    }
                    if(model != null && !isModelOnline()) {
                        break;
                    }
                }
            }
            throw new MissingSegmentException("Unable to download segment " + url.getFile() + " after " + maxTries + " tries");
        }
    }

    public boolean isModelOnline() {
        try {
            return model.isOnline(IGNORE_CACHE);
        } catch (IOException | ExecutionException | InterruptedException e) {
            return false;
        }
    }
}
