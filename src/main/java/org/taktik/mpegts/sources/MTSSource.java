package org.taktik.mpegts.sources;

import org.taktik.mpegts.MTSPacket;

public interface MTSSource extends AutoCloseable {
	MTSPacket nextPacket() throws Exception;
}
