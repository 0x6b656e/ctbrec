package ctbrec.recorder.download;

import static ctbrec.Recording.State.*;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iheartradio.m3u8.ParseException;
import com.iheartradio.m3u8.PlaylistException;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.event.EventBusHolder;
import ctbrec.event.RecordingStateChangedEvent;
import ctbrec.io.HttpClient;
import ctbrec.io.HttpException;
import okhttp3.Request;
import okhttp3.Response;

public class HlsDownload extends AbstractHlsDownload {

    private static final transient Logger LOG = LoggerFactory.getLogger(HlsDownload.class);

    protected Path downloadDir;

    private int segmentCounter = 1;
    private NumberFormat nf = new DecimalFormat("000000");

    public HlsDownload(HttpClient client) {
        super(client);
    }

    @Override
    public void start(Model model, Config config) throws IOException {
        try {
            running = true;
            startTime = Instant.now();
            super.model = model;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
            String startTime = sdf.format(new Date());
            Path modelDir = FileSystems.getDefault().getPath(config.getSettings().recordingsDir, model.getName());
            downloadDir = FileSystems.getDefault().getPath(modelDir.toString(), startTime);

            if(!model.isOnline()) {
                throw new IOException(model.getName() +"'s room is not public");
            }

            // let the world know, that we are recording now
            RecordingStateChangedEvent evt = new RecordingStateChangedEvent(getTarget(), RECORDING, model, getStartTime());
            EventBusHolder.BUS.post(evt);

            String segments = getSegmentPlaylistUrl(model);
            if(segments != null) {
                if (!Files.exists(downloadDir, LinkOption.NOFOLLOW_LINKS)) {
                    Files.createDirectories(downloadDir);
                }
                int lastSegment = 0;
                int nextSegment = 0;
                while(running) {
                    SegmentPlaylist lsp = getNextSegments(segments);
                    if(nextSegment > 0 && lsp.seq > nextSegment) {
                        LOG.warn("Missed segments {} < {} in download for {}", nextSegment, lsp.seq, model);
                        String first = lsp.segments.get(0);
                        int seq = lsp.seq;
                        for (int i = nextSegment; i < lsp.seq; i++) {
                            URL segmentUrl = new URL(first.replaceAll(Integer.toString(seq), Integer.toString(i)));
                            LOG.debug("Reloading segment {} for model {}", i, model.getName());
                            String prefix = nf.format(segmentCounter++);
                            downloadThreadPool.submit(new SegmentDownload(segmentUrl, downloadDir, client, prefix));
                        }
                        // TODO switch to a lower bitrate/resolution ?!?
                    }
                    int skip = nextSegment - lsp.seq;
                    for (String segment : lsp.segments) {
                        if(skip > 0) {
                            skip--;
                        } else {
                            URL segmentUrl = new URL(segment);
                            String prefix = nf.format(segmentCounter++);
                            downloadThreadPool.submit(new SegmentDownload(segmentUrl, downloadDir, client, prefix));
                            //new SegmentDownload(segment, downloadDir).call();
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
            throw new IOException("Couldn't parse HLS playlist:\n" + e.getInput(), e);
        } catch(PlaylistException e) {
            throw new IOException("Couldn't parse HLS playlist", e);
        } catch(EOFException e) {
            // end of playlist reached
            LOG.debug("Reached end of playlist for model {}", model);
        } catch(HttpException e) {
            if(e.getResponseCode() == 404) {
                LOG.debug("Playlist not found (404). Model {} probably went offline", model);
            } else {
                throw e;
            }
        } catch(Exception e) {
            throw new IOException("Couldn't download segment", e);
        } finally {
            alive = false;
            LOG.debug("Download for {} terminated", model);
        }
    }

    @Override
    public void stop() {
        running = false;
        alive = false;
    }

    private static class SegmentDownload implements Callable<Boolean> {
        private URL url;
        private Path file;
        private HttpClient client;

        public SegmentDownload(URL url, Path dir, HttpClient client, String prefix) {
            this.url = url;
            this.client = client;
            File path = new File(url.getPath());
            file = FileSystems.getDefault().getPath(dir.toString(), prefix + '_' + path.getName());
        }

        @Override
        public Boolean call() throws Exception {
            LOG.trace("Downloading segment to " + file);
            int maxTries = 3;
            for (int i = 1; i <= maxTries; i++) {
                Request request = new Request.Builder().url(url).addHeader("connection", "keep-alive").build();
                Response response = client.execute(request);
                try (
                        FileOutputStream fos = new FileOutputStream(file.toFile());
                        InputStream in = response.body().byteStream())
                {
                    byte[] b = new byte[1024 * 100];
                    int length = -1;
                    while( (length = in.read(b)) >= 0 ) {
                        fos.write(b, 0, length);
                    }
                    return true;
                } catch(FileNotFoundException e) {
                    LOG.debug("Segment does not exist {}", url.getFile());
                    break;
                } catch(Exception e) {
                    if (i == maxTries) {
                        LOG.warn("Error while downloading segment. Segment {} finally failed", file.toFile().getName());
                    } else {
                        LOG.warn("Error while downloading segment on try {}", i);
                    }
                } finally {
                    response.close();
                }
            }
            return false;
        }
    }

    @Override
    public File getTarget() {
        return downloadDir.toFile();
    }
}
