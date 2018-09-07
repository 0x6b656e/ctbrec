package org.taktik.mpegts.sources;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.taktik.mpegts.MTSPacket;

import java.util.Collection;
import java.util.List;

/**
 * An MTSSource using a simple round-robin packet multiplexing strategy.
 */
public class MultiplexingMTSSource extends AbstractMTSSource {
    private final boolean fixContinuity;
    private final List<MTSSource> sources;

    private ContinuityFixer continuityFixer;
    private int nextSource = 0;

    public MultiplexingMTSSource(boolean fixContinuity, Collection<MTSSource> sources) {
        Preconditions.checkArgument(sources.size() > 0, "Must provide at least contain one source");
        this.fixContinuity = fixContinuity;
        this.sources = Lists.newArrayList(sources);
        if (fixContinuity) {
            continuityFixer = new ContinuityFixer();
        }
    }

    public static MultiplexingMTSSourceBuilder builder() {
        return new MultiplexingMTSSourceBuilder();
    }

    @Override
    protected MTSPacket nextPacketInternal() throws Exception {
        MTSPacket packet = sources.get(nextSource).nextPacket();
        if (packet != null) {
            if (fixContinuity) {
                continuityFixer.fixContinuity(packet);
            }
            return packet;
        } else {
            // FIXME: infinite loop
            nextSource();
            return nextPacket();
        }
    }

    @Override
    protected synchronized void closeInternal() throws Exception {
        for (MTSSource source : sources) {
            source.close();
        }
    }

    private synchronized void nextSource() {
        nextSource++;
        if (nextSource == sources.size()) {
            nextSource = 0;
        }
    }

    public static class MultiplexingMTSSourceBuilder {
        boolean fixContinuity = false;
        private List<MTSSource> sources = Lists.newArrayList();

        public MultiplexingMTSSourceBuilder addSource(MTSSource source) {
            sources.add(source);
            return this;
        }

        public MultiplexingMTSSourceBuilder addSources(MTSSource... sources) {
            this.sources.addAll(Lists.newArrayList(sources));
            return this;
        }

        public MultiplexingMTSSourceBuilder addSources(Collection<MTSSource> sources) {
            this.sources.addAll(sources);
            return this;
        }

        public MultiplexingMTSSourceBuilder setSources(MTSSource... sources) {
            this.sources = Lists.newArrayList(sources);
            return this;
        }

        public MultiplexingMTSSourceBuilder setSources(Collection<MTSSource> sources) {
            this.sources = Lists.newArrayList(sources);
            return this;
        }

        public MultiplexingMTSSourceBuilder setFixContinuity(boolean fixContinuity) {
            this.fixContinuity = fixContinuity;
            return this;
        }

        public MultiplexingMTSSource build() {
            return new MultiplexingMTSSource(fixContinuity, sources);
        }
    }
}
