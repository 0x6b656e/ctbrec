package org.taktik.mpegts.sources;

public interface ResettableMTSSource extends MTSSource {
	void reset() throws Exception;
}
