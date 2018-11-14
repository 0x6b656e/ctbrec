package ctbrec.io;

import java.io.IOException;

public class HttpException extends IOException {

    private int code;
    private String msg;

    public HttpException(int code, String msg) {
        super(code + " - " + msg);
        this.code = code;
        this.msg = msg;
    }

    public int getResponseCode() {
        return code;
    }

    public String getResponseMessage() {
        return msg;
    }
}
