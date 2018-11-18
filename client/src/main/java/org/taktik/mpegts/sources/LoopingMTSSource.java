package org.taktik.mpegts.sources;

import org.taktik.mpegts.MTSPacket;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class LoopingMTSSource extends AbstractMTSSource{
    private ResettableMTSSource source;
    private Integer maxLoops;
    private long currentLoop;

    public LoopingMTSSource(ResettableMTSSource source, Integer maxLoops) {
        this.source = source;
        this.maxLoops = maxLoops;
        currentLoop = 1;
    }

    public static LoopingMTSSourceBuilder builder() {
        return new LoopingMTSSourceBuilder();
    }

    @Override
    protected MTSPacket nextPacketInternal() throws Exception {
        MTSPacket packet = source.nextPacket();
        if (packet == null) {
            currentLoop++;
            if (maxLoops == null || (currentLoop <= maxLoops)) {
                source.reset();
                packet = source.nextPacket();
            }
        }
        return packet;
    }

    @Override
    protected void closeInternal() throws Exception {
        source.close();
    }

    public static class LoopingMTSSourceBuilder {
        private ResettableMTSSource source;
        private boolean fixContinuity;
        private Integer maxLoops;

        private LoopingMTSSourceBuilder() {
        }

        public MTSSource build() {
            checkNotNull(source);
            checkArgument(maxLoops == null || maxLoops > 0);
            MTSSource result = new LoopingMTSSource(source, maxLoops);
            if (fixContinuity) {
                return new ContinuityFixingMTSSource(result);
            }
            return result;
        }

        public LoopingMTSSourceBuilder setSource(ResettableMTSSource source) {
            this.source = source;
            return this;
        }

        public LoopingMTSSourceBuilder setFixContinuity(boolean fixContinuity) {
            this.fixContinuity = fixContinuity;
            return this;
        }

        public LoopingMTSSourceBuilder setMaxLoops(Integer maxLoops) {
            this.maxLoops = maxLoops;
            return this;
        }
    }
}
