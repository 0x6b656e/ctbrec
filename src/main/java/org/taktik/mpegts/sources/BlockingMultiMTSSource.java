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
    private int lastProgress = 0;

    private BlockingMultiMTSSource(boolean fixContinuity) {
        this.fixContinuity = fixContinuity;
        if (fixContinuity) {
            continuityFixer = new ContinuityFixer();
        }
        this.sources = new LinkedBlockingQueue<>(10);
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
        packet = switchSourceIfNeeded(packet);

        if (fixContinuity) {
            try {
                continuityFixer.fixContinuity(packet);
            } catch(Exception e) {
                LOG.warn("Failed to fix continuity. MTSPacket probably invalid", e);
                return nextPacketInternal();
            }
        }
        return packet;
    }

    private MTSPacket switchSourceIfNeeded(MTSPacket packet) throws Exception {
        if(packet == null) {
            // end of source has been reached, switch to the next source
            closeCurrentSource();

            downloadedSegments++;
            if(listener != null && totalSegments > 0) {
                int progress = (int)(downloadedSegments * 100.0 / totalSegments);
                if(progress > lastProgress) {
                    listener.update(progress);
                    lastProgress = progress;
                }
            }
            if(downloadedSegments == totalSegments) {
                LOG.debug("All segments written. Queue size {}", sources.size());
                return null;
            }

            return firstPacketFromNextSource();
        }
        return packet;
    }

    private MTSPacket firstPacketFromNextSource() throws Exception {
        switchSource();
        return currentSource.nextPacket();
    }

    private void switchSource() throws InterruptedException {
        currentSource = sources.take();
    }

    private void closeCurrentSource() throws Exception {
        currentSource.close();
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
