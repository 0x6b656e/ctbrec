package ctbrec.recorder;

import java.io.File;
import java.io.IOException;

import com.iheartradio.m3u8.ParseException;
import com.iheartradio.m3u8.PlaylistException;


public interface SegmentMerger {


    public void merge(File recDir, File targetFile) throws IOException, ParseException, PlaylistException;

    public int getProgress();
}
