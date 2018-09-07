package org.taktik.mpegts.sources;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.taktik.mpegts.MTSPacket;

public class BlockingMultiMTSSource extends AbstractMTSSource implements AutoCloseable {

    private final boolean fixContinuity;
    private ContinuityFixer continuityFixer;

    private final BlockingQueue<MTSSource> sources;
    private MTSSource currentSource;

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
            currentSource = sources.take();
            packet = currentSource.nextPacket();
        }

        if (fixContinuity) {
            continuityFixer.fixContinuity(packet);
        }
        return packet;
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

        public BlockingMultiMTSSourceBuilder setFixContinuity(boolean fixContinuity) {
            this.fixContinuity = fixContinuity;
            return this;
        }

        public BlockingMultiMTSSource build() {
            return new BlockingMultiMTSSource(fixContinuity);
        }
    }
}
