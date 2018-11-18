package ctbrec.sites.mfc;

public class Message {
    private int type;
    private int sender;
    private int receiver;
    private int arg1;
    private int arg2;
    private String message;

    public Message(int type, int sender, int receiver, int arg1, int arg2, String message) {
        super();
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.message = message;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getSender() {
        return sender;
    }

    public void setSender(int sender) {
        this.sender = sender;
    }

    public int getReceiver() {
        return receiver;
    }

    public void setReceiver(int receiver) {
        this.receiver = receiver;
    }

    public int getArg1() {
        return arg1;
    }

    public void setArg1(int arg1) {
        this.arg1 = arg1;
    }

    public int getArg2() {
        return arg2;
    }

    public void setArg2(int arg2) {
        this.arg2 = arg2;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return type + " " + sender + " " + receiver + " " + arg1 + " " + arg2 + " " + message;
    }
}
