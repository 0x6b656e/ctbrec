package ctbrec;

import java.io.File;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.time.Duration;
import java.util.Set;

import org.jcodec.common.Demuxer;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.TrackType;
import org.jcodec.common.Tuple;
import org.jcodec.common.Tuple._2;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Packet;
import org.jcodec.containers.mps.MPSDemuxer;
import org.jcodec.containers.mps.MTSDemuxer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MpegUtil {
    private static final transient Logger LOG = LoggerFactory.getLogger(MpegUtil.class);

    public static void main(String[] args) throws IOException {
        readFile(new File("../../test-recs/ff.ts"));
    }

    public static void readFile(File file) throws IOException {
        System.out.println(file.getCanonicalPath());
        double duration = MpegUtil.getFileDuration(file);
        System.out.println(Duration.ofSeconds((long) duration));
    }

    public static double getFileDuration(File file) throws IOException {
        try(FileChannelWrapper ch = NIOUtils.readableChannel(file)) {
            _2<Integer,Demuxer> m2tsDemuxer = createM2TSDemuxer(ch, TrackType.VIDEO);
            Demuxer demuxer = m2tsDemuxer.v1;
            DemuxerTrack videoDemux = demuxer.getTracks().get(0);
            Packet videoFrame = null;
            double totalDuration = 0;
            while( (videoFrame = videoDemux.nextFrame()) != null) {
                totalDuration += videoFrame.getDurationD();
            }
            return totalDuration;
        }
    }

    public static _2<Integer, Demuxer> createM2TSDemuxer(FileChannelWrapper ch, TrackType targetTrack) throws IOException {
        MTSDemuxer mts = new MTSDemuxer(ch);
        Set<Integer> programs = mts.getPrograms();
        if (programs.size() == 0) {
            LOG.error("The MPEG TS stream contains no programs");
            return null;
        }
        Tuple._2<Integer, Demuxer> found = null;
        for (Integer pid : programs) {
            ReadableByteChannel program = mts.getProgram(pid);
            if (found != null) {
                program.close();
                continue;
            }
            MPSDemuxer demuxer = new MPSDemuxer(program);
            if (targetTrack == TrackType.AUDIO && demuxer.getAudioTracks().size() > 0
                    || targetTrack == TrackType.VIDEO && demuxer.getVideoTracks().size() > 0) {
                found = org.jcodec.common.Tuple._2(pid, (Demuxer) demuxer);
            } else {
                program.close();
            }
        }
        return found;
    }
}
