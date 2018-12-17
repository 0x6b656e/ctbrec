package ctbrec;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.io.StreamRedirectThread;

public class OS {

    private static final transient Logger LOG = LoggerFactory.getLogger(OS.class);

    public static enum TYPE {
        LINUX,
        MAC,
        WINDOWS,
        OTHER
    }

    public static TYPE getOsType() {
        if(System.getProperty("os.name").contains("Linux")) {
            return TYPE.LINUX;
        } else if(System.getProperty("os.name").contains("Windows")) {
            return TYPE.WINDOWS;
        } else if(System.getProperty("os.name").contains("Mac")) {
            return TYPE.MAC;
        } else {
            return TYPE.OTHER;
        }
    }

    public static File getConfigDir() {
        File configDir;
        switch (getOsType()) {
        case LINUX:
            String userHome = System.getProperty("user.home");
            configDir = new File(new File(userHome, ".config"), "ctbrec");
            break;
        case MAC:
            userHome = System.getProperty("user.home");
            configDir = new File(userHome, "Library/Preferences/ctbrec");
            break;
        case WINDOWS:
            String appData = System.getenv("APPDATA");
            configDir = new File(appData, "ctbrec");
            break;
        default:
            throw new RuntimeException("Unsupported operating system " + System.getProperty("os.name"));
        }
        return configDir;
    }

    public static Settings getDefaultSettings() {
        Settings settings = new Settings();
        if(getOsType() == TYPE.WINDOWS) {
            String userHome = System.getProperty("user.home");
            Path path = Paths.get(userHome, "Videos", "ctbrec");
            settings.recordingsDir = path.toString();
            String programFiles = System.getenv("ProgramFiles");
            programFiles = programFiles != null ? programFiles : "C:\\Program Files";
            settings.mediaPlayer = Paths.get(programFiles, "VideoLAN", "VLC", "vlc.exe").toString();
        } else if(getOsType() == TYPE.MAC) {
            String userHome = System.getProperty("user.home");
            settings.recordingsDir = Paths.get(userHome, "Movies", "ctbrec").toString();
            settings.mediaPlayer = "/Applications/VLC.app/Contents/MacOS/VLC";
        }
        return settings;
    }

    public static String[] getEnvironment() {
        String[] env = new String[System.getenv().size()];
        int index = 0;
        for (Entry<String, String> entry : System.getenv().entrySet()) {
            env[index++] = entry.getKey() + "=" + entry.getValue();
        }
        return env;
    }

    public static void notification(String title, String header, String msg) {
        if(OS.getOsType() == OS.TYPE.LINUX) {
            notifyLinux(title, header, msg);
        } else if(OS.getOsType() == OS.TYPE.WINDOWS) {
            notifyWindows(title, header, msg);
        } else if(OS.getOsType() == OS.TYPE.MAC) {
            notifyMac(title, header, msg);
        } else {
            // unknown system, try systemtray notification anyways
            notifySystemTray(title, header, msg);
        }
    }

    private static void notifyLinux(String title, String header, String msg) {
        try {
            Process p = Runtime.getRuntime().exec(new String[] {
                    "notify-send",
                    "-u", "normal",
                    "-t", "5000",
                    "-a", title,
                    header,
                    msg.replaceAll("-", "\\\\-").replaceAll("\\s", "\\\\ "),
                    "--icon=dialog-information"
            });
            new Thread(new StreamRedirectThread(p.getInputStream(), System.out)).start();
            new Thread(new StreamRedirectThread(p.getErrorStream(), System.err)).start();
        } catch (IOException e1) {
            LOG.error("Notification failed", e1);
        }
    }

    private static void notifyWindows(String title, String header, String msg) {
        notifySystemTray(title, header, msg);
    }

    private static void notifyMac(String title, String header, String msg) {
        notifySystemTray(title, header, msg);
    }

    private static void notifySystemTray(String title, String header, String msg) {
        if(SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = Toolkit.getDefaultToolkit().createImage(OS.class.getResource("/icon64.png"));
            TrayIcon trayIcon = new TrayIcon(image, title);
            trayIcon.setImageAutoSize(true);
            trayIcon.setToolTip(title);
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                LOG.error("Coulnd't add tray icon", e);
            }
            trayIcon.displayMessage(header, msg, MessageType.INFO);
        } else {
            LOG.error("SystemTray notifications not supported by this OS");
        }
    }
}
