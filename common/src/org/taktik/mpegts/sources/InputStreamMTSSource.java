package org.taktik.mpegts.sources;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.taktik.mpegts.Constants;
import org.taktik.mpegts.MTSPacket;

import com.google.common.base.Preconditions;

public class InputStreamMTSSource extends AbstractMTSSource {

    private InputStream inputStream;

    private InputStreamMTSSource(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public static InputStreamMTSSourceBuilder builder() {
        return new InputStreamMTSSourceBuilder();
    }

    @Override
    protected MTSPacket nextPacketInternal() throws IOException {
        byte[] packetData = new byte[Constants.MPEGTS_PACKET_SIZE];
        int bytesRead = 0;
        while(bytesRead < Constants.MPEGTS_PACKET_SIZE) {
            int bytesLeft = Constants.MPEGTS_PACKET_SIZE - bytesRead;
            int length = inputStream.read(packetData, bytesRead, bytesLeft);
            bytesRead += length;
            if(length == -1) {
                // no more bytes available
                return null;
            }
        }

        // Parse the packet
        return new MTSPacket(ByteBuffer.wrap(packetData));
    }

    @Override
    protected void closeInternal() throws Exception {
        inputStream.close();
    }

    public static class InputStreamMTSSourceBuilder {
        private InputStream inputStream;

        private InputStreamMTSSourceBuilder() {
        }

        public InputStreamMTSSourceBuilder setInputStream(InputStream inputStream) {
            this.inputStream = inputStream;
            return this;
        }

        public InputStreamMTSSource build() {
            Preconditions.checkNotNull(inputStream, "InputStream cannot be null");
            return new InputStreamMTSSource(inputStream);
        }
    }
}
