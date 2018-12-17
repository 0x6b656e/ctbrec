package ctbrec;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ctbrec.event.EventHandlerConfiguration;

public class Settings {

    public enum ProxyType {
        DIRECT,
        HTTP,
        SOCKS4,
        SOCKS5
    }

    public enum DirectoryStructure {
        FLAT("all recordings in one directory"),
        ONE_PER_MODEL("one directory for each model"),
        ONE_PER_RECORDING("one directory for each recording");

        private String description;
        DirectoryStructure(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    public boolean singlePlayer = true;
    public boolean showPlayerStarting = false;
    public boolean localRecording = true;
    public int httpPort = 8080;
    public int httpTimeout = 10000;
    public String httpUserAgent = "Mozilla/5.0 Gecko/20100101 Firefox/62.0";
    public String httpServer = "localhost";
    public String recordingsDir = System.getProperty("user.home") + File.separator + "ctbrec";
    public DirectoryStructure recordingsDirStructure = DirectoryStructure.FLAT;
    public long minimumSpaceLeftInBytes = 0;
    public int minimumLengthInSeconds = 0;
    public String mediaPlayer = "/usr/bin/mpv";
    public String postProcessing = "";
    public String username = ""; // chaturbate username TODO maybe rename this onetime
    public String password = ""; // chaturbate password TODO maybe rename this onetime
    public String chaturbateBaseUrl = "https://chaturbate.com";
    public String bongaUsername = "";
    public String bongaPassword = "";
    public String mfcUsername = "";
    public String mfcPassword = "";
    public String mfcBaseUrl = "https://www.myfreecams.com";
    public String mfcModelsTableSortColumn = "";
    public String mfcModelsTableSortType = "";
    public double[] mfcModelsTableColumnWidths = new double[0];
    public String mfcModelsTableFilter = "";
    public List<String> mfcDisabledModelsTableColumns = new ArrayList<>();
    public boolean mfcIgnoreUpscaled = false;
    public String camsodaUsername = "";
    public String camsodaPassword = "";
    public String cam4Username = "";
    public String cam4Password = "";
    public String streamateUsername = "";
    public String streamatePassword = "";
    public String lastDownloadDir = "";

    public List<Model> models = new ArrayList<>();
    public List<EventHandlerConfiguration> eventHandlers = new ArrayList<>();
    public boolean determineResolution = false;
    public boolean livePreviews = false;
    public boolean requireAuthentication = false;
    public boolean chooseStreamQuality = false;
    public int maximumResolution = 0;
    public byte[] key = null;
    public ProxyType proxyType = ProxyType.DIRECT;
    public String proxyHost;
    public String proxyPort;
    public String proxyUser;
    public String proxyPassword;
    public String startTab = "Settings";
    public int thumbWidth = 180;
    public boolean updateThumbnails = true;
    public int windowWidth = 1340;
    public int windowHeight = 800;
    public boolean windowMaximized = false;
    public int windowX;
    public int windowY;
    public int splitRecordings = 0;
    public List<String> disabledSites = new ArrayList<>();
    public String colorBase = "#FFFFFF";
    public String colorAccent = "#FFFFFF";
    public int onlineCheckIntervalInSecs = 60;
    public String recordedModelsSortColumn = "";
    public String recordedModelsSortType = "";
    public double[] recordedModelsColumnWidths = new double[0];
    public String recordingsSortColumn = "";
    public String recordingsSortType = "";
    public double[] recordingsColumnWidths = new double[0];
}
