package ctbrec.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Launcher {

    private static final transient Logger LOG = LoggerFactory.getLogger(Launcher.class);

    public static void main(String[] args) {
        String jvmName = System.getProperty("java.vm.name");
        if (jvmName.startsWith("OpenJDK")) {
            // check for OpenJFX
            try {
                Class.forName("javafx.application.Application");
                CtbrecApplication.main(args);
            } catch (ClassNotFoundException e) {
                LOG.error("You are running ctbrec with OpenJDK, but OpenJFX can not be found.\n"
                        + "Please either install OpenJFX or use the Oracle JRE, which you can download at\n"
                        + "http://www.oracle.com/technetwork/java/javase/downloads/index.html");
                System.exit(1);
            }
        } else {
            CtbrecApplication.main(args);
        }
    }
}
