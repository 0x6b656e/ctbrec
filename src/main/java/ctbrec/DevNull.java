package ctbrec;

import java.io.IOException;
import java.io.OutputStream;

public class DevNull extends OutputStream {
    @Override
    public void write(int b) throws IOException {
    }

    @Override
    public void write(byte[] b) throws IOException {
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
    }
}
