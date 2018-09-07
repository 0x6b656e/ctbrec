package ctbrec.recorder.download;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

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
import ctbrec.HttpClient;
import ctbrec.Model;
import ctbrec.recorder.Chaturbate;
import ctbrec.recorder.StreamInfo;
import okhttp3.Request;
import okhttp3.Response;

public class MergedHlsDownload extends AbstractHlsDownload {

    private static final transient Logger LOG = LoggerFactory.getLogger(MergedHlsDownload.class);
    private BlockingQueue<Future<InputStream>> mergeQueue = new LinkedBlockingQueue<>();
    private BlockingMultiMTSSource multiSource;
    private Thread mergeThread;
    private Thread handoverThread;
    private Streamer streamer;

    public MergedHlsDownload(HttpClient client) {
        super(client);
    }

    @Override
    public void start(Model model, Config config) throws IOException {
        try {
            running = true;
            StreamInfo streamInfo = Chaturbate.getStreamInfo(model, client);
            if(!Objects.equals(streamInfo.room_status, "public")) {
                throw new IOException(model.getName() +"'s room is not public");
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
            String startTime = sdf.format(new Date());
            Path modelDir = FileSystems.getDefault().getPath(config.getSettings().recordingsDir, model.getName());
            downloadDir = FileSystems.getDefault().getPath(modelDir.toString(), startTime);
            if (!Files.exists(downloadDir, LinkOption.NOFOLLOW_LINKS)) {
                Files.createDirectories(downloadDir);
            }

            mergeThread = createMergeThread(downloadDir);
            mergeThread.start();
            handoverThread = createHandoverThread();
            handoverThread.start();

            String segments = parseMaster(streamInfo.url, model.getStreamUrlIndex());
            if(segments != null) {
                int lastSegment = 0;
                int nextSegment = 0;
                while(running) {
                    LiveStreamingPlaylist lsp = getNextSegments(segments);
                    if(nextSegment > 0 && lsp.seq > nextSegment) {
                        LOG.warn("Missed segments {} < {} in download for {}", nextSegment, lsp.seq, model);
                        String first = lsp.segments.get(0);
                        int seq = lsp.seq;
                        for (int i = nextSegment; i < lsp.seq; i++) {
                            URL segmentUrl = new URL(first.replaceAll(Integer.toString(seq), Integer.toString(i)));
                            LOG.debug("Loading missed segment {} for model {}", i, model.getName());
                            Future<InputStream> downloadFuture = downloadThreadPool.submit(new SegmentDownload(segmentUrl, client));
                            mergeQueue.add(downloadFuture);
                        }
                        // TODO switch to a lower bitrate/resolution ?!?
                    }
                    int skip = nextSegment - lsp.seq;
                    for (String segment : lsp.segments) {
                        if(skip > 0) {
                            skip--;
                        } else {
                            URL segmentUrl = new URL(segment);
                            Future<InputStream> downloadFuture = downloadThreadPool.submit(new SegmentDownload(segmentUrl, client));
                            mergeQueue.add(downloadFuture);
                        }
                    }

                    long wait = 0;
                    if(lastSegment == lsp.seq) {
                        // playlist didn't change -> wait for at least half the target duration
                        wait = (long) lsp.targetDuration * 1000 / 2;
                        LOG.trace("Playlist didn't change... waiting for {}ms", wait);
                    } else {
                        // playlist did change -> wait for at least last segment duration
                        wait = 1;//(long) lsp.lastSegDuration * 1000;
                        LOG.trace("Playlist changed... waiting for {}ms", wait);
                    }

                    try {
                        Thread.sleep(wait);
                    } catch (InterruptedException e) {
                        if(running) {
                            LOG.error("Couldn't sleep between segment downloads. This might mess up the download!");
                        }
                    }

                    lastSegment = lsp.seq;
                    nextSegment = lastSegment + lsp.segments.size();
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
            alive = false;
            LOG.debug("Download for {} terminated", model);
        }
    }

    private Thread createHandoverThread() {
        Thread t = new Thread(() -> {
            while(running) {
                try {
                    Future<InputStream> downloadFuture = mergeQueue.take();
                    InputStream tsData = downloadFuture.get();
                    InputStreamMTSSource source = InputStreamMTSSource.builder().setInputStream(tsData).build();
                    multiSource.addSource(source);
                } catch (InterruptedException e) {
                    if(running) {
                        LOG.error("Interrupted while waiting for a download future", e);
                    }
                } catch (ExecutionException e) {
                    LOG.error("Error while executing download", e);
                }
            }
        });
        t.setName("Segment Handover Thread");
        t.setDaemon(true);
        return t;
    }

    private Thread createMergeThread(Path downloadDir) {
        Thread t = new Thread(() -> {
            multiSource = BlockingMultiMTSSource.builder().setFixContinuity(true).build();

            File out = new File(downloadDir.toFile(), "record.ts");
            FileChannel channel = null;
            try {
                channel = FileChannel.open(out.toPath(), CREATE, WRITE);
                MTSSink sink = ByteChannelSink.builder().setByteChannel(channel).build();

                streamer = Streamer.builder()
                        .setSource(multiSource)
                        .setSink(sink)
                        .setSleepingEnabled(false)
                        .setBufferSize(10)
                        .build();

                // Start streaming
                streamer.stream();
            } catch (InterruptedException e) {
                if(running) {
                    LOG.error("Error while waiting for a download future", e);
                }
            }  catch(Exception e) {
                LOG.error("Error while saving stream to file", e);
            } finally {
                try {
                    channel.close();
                } catch (IOException e) {
                    LOG.error("Error while closing file {}", out);
                }
            }
        });
        t.setName("Segment Merger Thread");
        t.setDaemon(true);
        return t;
    }

    @Override
    public void stop() {
        running = false;
        alive = false;
        LOG.debug("Stopping streamer");
        streamer.stop();
        LOG.debug("Sending interrupt to merger");
        mergeThread.interrupt();
        LOG.debug("Sending interrupt to handover thread");
        handoverThread.interrupt();
        LOG.debug("Download stopped");
    }

    private static class SegmentDownload implements Callable<InputStream> {
        private URL url;
        private HttpClient client;

        public SegmentDownload(URL url, HttpClient client) {
            this.url = url;
            this.client = client;
        }

        @Override
        public InputStream call() throws Exception {
            LOG.trace("Downloading segment " + url.getFile());
            int maxTries = 3;
            for (int i = 1; i <= maxTries; i++) {
                Request request = new Request.Builder().url(url).addHeader("connection", "keep-alive").build();
                Response response = client.execute(request);
                try {
                    InputStream in = response.body().byteStream();
                    return in;
                } catch(Exception e) {
                    if (i == maxTries) {
                        LOG.warn("Error while downloading segment. Segment {} finally failed", url.getFile());
                    } else {
                        LOG.warn("Error while downloading segment {} on try {}", url.getFile(), i);
                    }
                } /*finally {
                    response.close();
                }*/
            }
            throw new IOException("Unable to download segment " + url.getFile() + " after " + maxTries + " tries");
        }
    }
}
