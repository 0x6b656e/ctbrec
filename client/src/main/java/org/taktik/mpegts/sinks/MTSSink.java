package org.taktik.mpegts.sinks;

import org.taktik.mpegts.MTSPacket;

public interface MTSSink extends AutoCloseable {
	void send(MTSPacket packet) throws Exception;
}
