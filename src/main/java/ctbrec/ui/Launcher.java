package ctbrec.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Java;

public class Launcher {

    private static final transient Logger LOG = LoggerFactory.getLogger(Launcher.class);

    public static void main(String[] args) {
        int javaVersion = Java.version();
        if(javaVersion == 0) {
            LOG.warn("Unknown Java version {}. App might not work as expected", javaVersion);
        } else if (javaVersion < 10) {
            LOG.error("Your Java version ({}) is too old. Please update to Java 10 or newer", javaVersion);
            System.exit(1);
        }
        CamrecApplication.main(args);
    }
}
