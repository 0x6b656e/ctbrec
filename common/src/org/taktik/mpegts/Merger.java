package org.taktik.mpegts;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import org.taktik.mpegts.sinks.ByteChannelSink;
import org.taktik.mpegts.sinks.MTSSink;
import org.taktik.mpegts.sources.MTSSource;
import org.taktik.mpegts.sources.MTSSources;
import org.taktik.mpegts.sources.MultiplexingMTSSource;
import org.taktik.mpegts.sources.MultiplexingMTSSource.MultiplexingMTSSourceBuilder;

public class Merger {

    public static void main(String[] args) throws IOException {
        File[] files = new File("/home/henni/devel/ctbrec/remux-ts-mp4/src/test/resources").listFiles((f) -> f.getName().startsWith("63"));
        //File[] files = new File("/home/henni/devel/ctbrec/mpegts-streamer/src/test/resources/yesikasaenz/2018-09-04_23-30").listFiles((f) -> {
        //        return f.getName().startsWith("media") && f.getName().endsWith(".ts");
        //    });
        Arrays.sort(files);

        MultiplexingMTSSourceBuilder builder = MultiplexingMTSSource.builder()
                .setFixContinuity(true);


        for (File file : files) {
            MTSSource source = MTSSources.from(file);
            builder.addSource(source);
        }

        File out = new File("merged.ts");
        FileChannel channel = null;
        try {
            channel = FileChannel.open(out.toPath(), CREATE, WRITE);
            MTSSource mtsSource = builder.build();
            MTSSink sink = ByteChannelSink.builder().setByteChannel(channel).build();

            // build streamer
            Streamer streamer = Streamer.builder()
                    .setSource(mtsSource)
                    .setSink(sink)
                    .build();

            // Start streaming
            streamer.stream();

            //            synchronized (streamer) {
            //                System.out.println("Waiting for streamer to finish");
            //                streamer.wait();
            //                System.out.println("Streamer finished");
            //            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            channel.close();
        }
    }

}
