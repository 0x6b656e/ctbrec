package ctbrec;

import java.text.DecimalFormat;

public class StringUtil {
    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static boolean isNotBlank(String s) {
        return !isBlank(s);
    }

    public static String formatSize(Number sizeInByte) {
        DecimalFormat df = new DecimalFormat("0.00");
        String unit = "Bytes";
        double size = sizeInByte.doubleValue();
        if(size > 1024.0 * 1024 * 1024) {
            size = size / 1024.0 / 1024 / 1024;
            unit = "GiB";
        } else if(size > 1024.0 * 1024) {
            size = size / 1024.0 / 1024;
            unit = "MiB";
        } else if(size > 1024.0) {
            size = size / 1024.0;
            unit = "KiB";
        }
        return df.format(size) + ' ' + unit;
    }
}
