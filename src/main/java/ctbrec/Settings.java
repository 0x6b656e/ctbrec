package ctbrec;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Settings {

    public enum ProxyType {
        DIRECT,
        HTTP,
        SOCKS4,
        SOCKS5
    }

    public boolean singlePlayer = true;
    public boolean localRecording = true;
    public int httpPort = 8080;
    public int httpTimeout = 10000;
    public String httpServer = "localhost";
    public String recordingsDir = System.getProperty("user.home") + File.separator + "ctbrec";
    public String mediaPlayer = "/usr/bin/mpv";
    public String username = ""; // chaturbate username TODO maybe rename this onetime
    public String password = ""; // chaturbate password TODO maybe rename this onetime
    public String mfcUsername = "";
    public String mfcPassword = "";
    public String camsodaUsername = "";
    public String camsodaPassword = "";
    public String cam4Username;
    public String cam4Password;
    public String lastDownloadDir = "";

    public List<Model> models = new ArrayList<Model>();
    public boolean determineResolution = false;
    public boolean requireAuthentication = false;
    public boolean chooseStreamQuality = false;
    public byte[] key = null;
    public ProxyType proxyType = ProxyType.DIRECT;
    public String proxyHost;
    public String proxyPort;
    public String proxyUser;
    public String proxyPassword;
    public int thumbWidth = 180;
    public int windowWidth = 1340;
    public int windowHeight = 800;
    public boolean windowMaximized = false;
    public int windowX;
    public int windowY;
    public int splitRecordings = 0;
    public List<String> disabledSites = new ArrayList<>();
}
