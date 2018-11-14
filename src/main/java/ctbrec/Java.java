package ctbrec;

public class Java {

    public static int version() {
        String specVersion = System.getProperty("java.specification.version");
        switch(specVersion) {
        case "1.7":
            return 7;
        case "1.8":
            return 8;
        case "9":
            return 9;
        case "10":
            return 10;
        case "11":
            return 11;
        case "12":
            return 12;
        default:
            return 0;
        }
    }

    public static void main(String[] args) {
        System.out.println(Java.version());
    }
}
