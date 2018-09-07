package org.taktik.mpegts.sinks;

import org.taktik.mpegts.MTSPacket;

import java.nio.channels.ByteChannel;

public class ByteChannelSink implements MTSSink {

	private ByteChannel byteChannel;

	private ByteChannelSink(ByteChannel byteChannel) {
		this.byteChannel = byteChannel;
	}

	public static ByteChannelSinkBuilder builder() {
		return new ByteChannelSinkBuilder();
	}

	@Override
	public void send(MTSPacket packet) throws Exception {
		byteChannel.write(packet.getBuffer());
	}

	@Override
	public void close() throws Exception {
		byteChannel.close();
	}

	public static class ByteChannelSinkBuilder {
		private ByteChannel byteChannel;

		private ByteChannelSinkBuilder(){}

		public ByteChannelSink build() {
			return new ByteChannelSink(byteChannel);
		}

		public ByteChannelSinkBuilder setByteChannel(ByteChannel byteChannel) {
			this.byteChannel = byteChannel;
			return this;
		}
	}
}
