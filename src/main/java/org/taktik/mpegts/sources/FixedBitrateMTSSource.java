package org.taktik.mpegts.sources;

import org.taktik.mpegts.MTSPacket;

/**
 * Decorate a source with a declared bitrate.
 */
public class FixedBitrateMTSSource extends AbstractMTSSource {
    private final MTSSource source;
    private final long bitrate;

    public FixedBitrateMTSSource(MTSSource source, long bitrate) {
        this.source = source;
        this.bitrate = bitrate;
    }

    public long getBitrate() {
        return bitrate;
    }

    @Override
    protected MTSPacket nextPacketInternal() throws Exception {
        return source.nextPacket();
    }

    @Override
    protected void closeInternal() throws Exception {
        source.close();
    }
}
