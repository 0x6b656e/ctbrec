package org.taktik.mpegts;

import java.nio.ByteBuffer;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.taktik.mpegts.sinks.MTSSink;
import org.taktik.mpegts.sources.MTSSource;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

public class Streamer {
    static final Logger log = LoggerFactory.getLogger("streamer");

    private MTSSource source;
    private MTSSink sink;

    private ArrayBlockingQueue<MTSPacket> buffer;
    private int bufferSize;
    private boolean endOfSourceReached;
    private boolean streamingShouldStop;

    private PATSection patSection;
    private TreeMap<Integer,PMTSection> pmtSection;

    private Thread bufferingThread;
    private Thread streamingThread;

    private boolean sleepingEnabled;
    private String name;

    private Streamer(MTSSource source, MTSSink sink, int bufferSize, boolean sleepingEnabled, String name) {
        this.source = source;
        this.sink = sink;
        this.bufferSize = bufferSize;
        this.sleepingEnabled = sleepingEnabled;
        this.name = name;
    }

    public void stream() throws InterruptedException {
        buffer = new ArrayBlockingQueue<>(bufferSize);
        patSection = null;
        pmtSection = Maps.newTreeMap();
        endOfSourceReached = false;
        streamingShouldStop = false;
        log.info("PreBuffering {} packets", bufferSize);
        try {
            preBuffer();
        } catch (Exception e) {
            throw new IllegalStateException("Error while buffering", e);
        }
        log.info("Done PreBuffering");

        bufferingThread = new Thread(this::fillBuffer, "Buffering ["+name+"]");
        bufferingThread.setDaemon(true);
        bufferingThread.start();

        streamingThread = new Thread(this::internalStream, "Streaming ["+name+"]");
        streamingThread.setDaemon(true);
        streamingThread.start();

        bufferingThread.join();
        streamingThread.join();

        try {
            sink.close();
        } catch(Exception e) {
            log.error("Couldn't close sink", e);
        }
    }

    public void stop() {
        streamingShouldStop = true;
        try {
            source.close();
        } catch (Exception e) {
            log.error("Couldn't close source", e);
        }
        try {
            sink.close();
        } catch (Exception e) {
            log.error("Couldn't close sink", e);
        }
        buffer.clear();
        try {
            bufferingThread.interrupt();
            streamingThread.interrupt();
        } catch (Exception e) {
            log.error("Couldn't interrupt streamer threads");
        }
    }

    public void switchSink(MTSSink sink) {
        MTSSink old = this.sink;
        this.sink = sink;
        try {
            old.close();
        } catch (Exception e) {
            log.error("Couldn't close old sink while switching sinks", e);
        }
    }

    private void internalStream() {
        boolean resetState = false;
        MTSPacket packet = null;
        long packetCount = 0;
        //long pcrPidPacketCount = 0;
        Long firstPcrValue = null;
        Long firstPcrTime = null;
        //Long firstPcrPacketCount = null;
        Long lastPcrValue = null;
        Long lastPcrTime = null;
        //Long lastPcrPacketCount = null;
        Long averageSleep = null;
        while (!streamingShouldStop) {
            if (resetState) {
                firstPcrValue = null;
                firstPcrTime = null;
                lastPcrValue = null;
                lastPcrTime = null;
                averageSleep = null;
                resetState = false;
            }

            // Initialize time to sleep
            long sleepNanos = 0;

            try {
                packet = buffer.take();
                if (packet == null) {
                    if (endOfSourceReached) {
                        packet = buffer.take();
                        if (packet == null) {
                            break;
                        }
                    } else {
                        continue;
                    }
                }
            } catch (InterruptedException e1) {
                if(!endOfSourceReached && !streamingShouldStop) {
                    log.error("Interrupted while waiting for packet");
                    continue;
                } else {
                    break;
                }
            }

            int pid = packet.getPid();

            if (pid == 0 && packet.isPayloadUnitStartIndicator()) {
                ByteBuffer payload = packet.getPayload();
                payload.rewind();
                int pointer = payload.get() & 0xff;
                payload.position(payload.position() + pointer);
                patSection = PATSection.parse(payload);
                for (Integer pmtPid : pmtSection.keySet()) {
                    if (!patSection.getPrograms().values().contains(pmtPid)) {
                        pmtSection.remove(pmtPid);
                    }
                }
            }

            if (pid != 0 && patSection!=null) {
                if (patSection.getPrograms().values().contains(pid)) {
                    if (packet.isPayloadUnitStartIndicator()) {
                        ByteBuffer payload = packet.getPayload();
                        payload.rewind();
                        int pointer = payload.get() & 0xff;
                        payload.position(payload.position() + pointer);
                        pmtSection.put(pid, PMTSection.parse(payload));
                    }
                }

            }

            // Check PID matches PCR PID
            if (true) {//mtsPacket.pid == pmt.getPcrPid()) {
                //pcrPidPacketCount++;

                if (averageSleep != null) {
                    sleepNanos = averageSleep;
                } else {
                    //						if (pcrPidPacketCount < 2) {
                    //							if (pcrPidPacketCount % 10 == 0) {
                    //								sleepNanos = 15;
                    //							}
                    //						}
                }
            }

            // Check for PCR
            if (packet.getAdaptationField() != null) {
                if (packet.getAdaptationField().getPcr() != null) {
                    if (packet.getPid() == getPCRPid()) {
                        if (!packet.getAdaptationField().isDiscontinuityIndicator()) {
                            // Get PCR and current nano time
                            long pcrValue = packet.getAdaptationField().getPcr().getValue();
                            long pcrTime = System.nanoTime();

                            // Compute sleepNanosOrig
                            if (firstPcrValue == null || firstPcrTime == null) {
                                firstPcrValue = pcrValue;
                                firstPcrTime = pcrTime;
                                //firstPcrPacketCount = pcrPidPacketCount;
                            }

                            // Compute sleepNanosPrevious
                            Long sleepNanosPrevious = null;
                            if (lastPcrValue != null && lastPcrTime != null) {
                                if (pcrValue <= lastPcrValue) {
                                    log.trace("PCR discontinuity ! "  + packet.getPid());
                                    resetState = true;
                                } else {
                                    sleepNanosPrevious = ((pcrValue - lastPcrValue) / 27 * 1000) - (pcrTime - lastPcrTime);
                                }
                            }
                            //								System.out.println("pcrValue=" + pcrValue + ", lastPcrValue=" + lastPcrValue + ", sleepNanosPrevious=" + sleepNanosPrevious + ", sleepNanosOrig=" + sleepNanosOrig);

                            // Set sleep time based on PCR if possible
                            if (sleepNanosPrevious != null) {
                                // Safety : We should never have to wait more than 100ms
                                if (sleepNanosPrevious > 100000000) {
                                    log.warn("PCR sleep ignored, too high !");
                                    resetState = true;
                                } else {
                                    sleepNanos = sleepNanosPrevious;
                                    //										averageSleep = sleepNanosPrevious / (pcrPidPacketCount - lastPcrPacketCount - 1);
                                }
                            }

                            // Set lastPcrValue/lastPcrTime
                            lastPcrValue = pcrValue;
                            lastPcrTime = pcrTime + sleepNanos;
                            //lastPcrPacketCount = pcrPidPacketCount;
                        } else {
                            log.warn("Skipped PCR - Discontinuity indicator");
                        }
                    } else {
                        log.debug("Skipped PCR - PID does not match");
                    }
                }
            }

            // Sleep if needed
            if (sleepNanos > 0 && sleepingEnabled) {
                log.trace("Sleeping " + sleepNanos / 1000000 + " millis, " + sleepNanos % 1000000 + " nanos");
                try {
                    Thread.sleep(sleepNanos / 1000000, (int) (sleepNanos % 1000000));
                } catch (InterruptedException e) {
                    log.warn("Streaming sleep interrupted!");
                }
            }

            // Stream packet
            // System.out.println("Streaming packet #" + packetCount + ", PID=" + mtsPacket.getPid() + ", pcrCount=" + pcrCount + ", continuityCounter=" + mtsPacket.getContinuityCounter());

            if(!streamingShouldStop && !Thread.interrupted()) {
                try {
                    sink.send(packet);
                } catch (Exception e) {
                    log.error("Error sending packet to sink", e);
                }
            }

            packetCount++;
        }
        log.info("Sent {} MPEG-TS packets", packetCount);
        synchronized (this) {
            notifyAll();
        }
    }

    private void preBuffer() throws Exception {
        MTSPacket packet;
        int packetNumber = 0;
        while ((packetNumber < bufferSize) && (packet = source.nextPacket()) != null) {
            buffer.add(packet);
            packetNumber++;
        }
    }

    private void fillBuffer() {
        try {
            MTSPacket packet;
            while (!streamingShouldStop && (packet = source.nextPacket()) != null) {
                boolean put = false;
                while (!put) {
                    try {
                        buffer.put(packet);
                        put = true;
                    } catch (InterruptedException ignored) {
                        log.error("Error adding packet to buffer", ignored);
                    }
                }
            }
        } catch (InterruptedException e) {
            if(!streamingShouldStop) {
                log.error("Error reading from source", e);
            }
        } catch (Exception e) {
            log.error("Error reading from source", e);
        } finally {
            endOfSourceReached = true;
            try {
                streamingThread.interrupt();
            } catch(Exception e) {
                log.error("Couldn't interrupt streaming thread", e);
            }
        }
    }

    private int getPCRPid() {
        if ((!pmtSection.isEmpty())) {
            // TODO change this
            return pmtSection.values().iterator().next().getPcrPid();
        }
        return -1;
    }

    public static StreamerBuilder builder() {
        return new StreamerBuilder();
    }

    public static class StreamerBuilder {
        private MTSSink sink;
        private MTSSource source;
        private int bufferSize = 1000;
        private boolean sleepingEnabled = false;
        private String name;

        public StreamerBuilder setSink(MTSSink sink) {
            this.sink = sink;
            return this;
        }

        public StreamerBuilder setSource(MTSSource source) {
            this.source = source;
            return this;
        }

        public StreamerBuilder setBufferSize(int bufferSize) {
            this.bufferSize = bufferSize;
            return this;
        }

        public StreamerBuilder setSleepingEnabled(boolean sleepingEnabled) {
            this.sleepingEnabled = sleepingEnabled;
            return this;
        }

        public StreamerBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public Streamer build() {
            Preconditions.checkNotNull(sink);
            Preconditions.checkNotNull(source);
            return new Streamer(source, sink, bufferSize, sleepingEnabled, name);
        }

    }
}