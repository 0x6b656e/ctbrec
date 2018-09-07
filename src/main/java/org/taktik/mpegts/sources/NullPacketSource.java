package org.taktik.mpegts.sources;

import org.taktik.mpegts.Constants;
import org.taktik.mpegts.MTSPacket;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class NullPacketSource extends AbstractMTSSource {

    public NullPacketSource() {
    }

    @Override
    protected MTSPacket nextPacketInternal() throws Exception {
        byte[] buf = new byte[Constants.MPEGTS_PACKET_SIZE];

        // payload (null bytes)
        Arrays.fill(buf, (byte) 0xff);

        // header
        buf[0] = 0x47; // sync byte
        buf[1] = 0x1f; // PID high
        buf[2] = (byte) 0xff; // PID low
        buf[3] = 0x10; // adaptation control and continuity

        return new MTSPacket(ByteBuffer.wrap(buf));
    }

    @Override
    protected void closeInternal() throws Exception {
        // does nothing
    }
}
