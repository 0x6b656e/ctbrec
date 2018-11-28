package ctbrec.recorder.download;

import java.io.IOException;

public class MissingSegmentException extends IOException {

    public MissingSegmentException(String msg) {
        super(msg);
    }

}
