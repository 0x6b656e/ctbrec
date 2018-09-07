package org.taktik.mpegts.sources;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.taktik.mpegts.MTSPacket;

import ctbrec.recorder.ProgressListener;

public class BlockingMultiMTSSource extends AbstractMTSSource implements AutoCloseable {

    private static final transient Logger LOG = LoggerFactory.getLogger(BlockingMultiMTSSource.class);

    private final boolean fixContinuity;
    private ContinuityFixer continuityFixer;

    private final BlockingQueue<MTSSource> sources;
    private MTSSource currentSource;
    private int downloadedSegments = 0;
    private int totalSegments = -1;
    private ProgressListener listener;

    private BlockingMultiMTSSource(boolean fixContinuity) {
        this.fixContinuity = fixContinuity;
        if (fixContinuity) {
            continuityFixer = new ContinuityFixer();
        }
        this.sources = new LinkedBlockingQueue<>();
    }

    public void addSource(MTSSource source) throws InterruptedException {
        this.sources.put(source);
    }

    @Override
    protected MTSPacket nextPacketInternal() throws Exception {
        if(currentSource == null) {
            currentSource = sources.take();
        }

        MTSPacket packet = currentSource.nextPacket();
        if(packet == null) {
            // end of source has been reached, switch to the next source
            currentSource.close();
            downloadedSegments++;
            if(listener != null && totalSegments > 0) {
                int progress = (int)(downloadedSegments * 100.0 / totalSegments);
                listener.update(progress);
            }
            if(downloadedSegments == totalSegments) {
                LOG.debug("All segments written. Queue size {}", sources.size());
                return null;
            }

            currentSource = sources.take();
            packet = currentSource.nextPacket();
            //            }
        }

        if (fixContinuity) {
            continuityFixer.fixContinuity(packet);
        }
        return packet;
    }

    private void setProgressListener(ProgressListener listener) {
        this.listener = listener;
    }

    public void setTotalSegments(int total) {
        this.totalSegments = total;
    }

    @Override
    protected void closeInternal() throws Exception {
        for (MTSSource source : sources) {
            source.close();
        }
    }

    public static BlockingMultiMTSSourceBuilder builder() {
        return new BlockingMultiMTSSourceBuilder();
    }

    public static class BlockingMultiMTSSourceBuilder {
        boolean fixContinuity = false;
        ProgressListener listener;

        public BlockingMultiMTSSourceBuilder setFixContinuity(boolean fixContinuity) {
            this.fixContinuity = fixContinuity;
            return this;
        }
        public BlockingMultiMTSSourceBuilder setProgressListener(ProgressListener listener) {
            this.listener = listener;
            return this;
        }

        public BlockingMultiMTSSource build() {
            BlockingMultiMTSSource source = new BlockingMultiMTSSource(fixContinuity);
            if(listener != null) {
                source.setProgressListener(listener);
            }
            return source;
        }

    }
}
