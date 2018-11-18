package org.taktik.mpegts.sources;

import org.taktik.mpegts.MTSPacket;

/**
 * Decorates an MTSSource with continuity fixing. Not suitable for use with multiple different
 * MTSSources as it will not reset counters when switching sources.
 */
public class ContinuityFixingMTSSource extends AbstractMTSSource {
    private final ContinuityFixer continuityFixer = new ContinuityFixer();
    private final MTSSource source;

    public ContinuityFixingMTSSource(MTSSource source) {
        this.source = source;
    }

    @Override
    protected MTSPacket nextPacketInternal() throws Exception {
        MTSPacket packet = source.nextPacket();
        if (packet != null) {
            continuityFixer.fixContinuity(packet);
        }
        return packet;
    }

    @Override
    protected void closeInternal() throws Exception {
        source.close();
    }
}
