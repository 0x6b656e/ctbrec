package ctbrec;

import static java.nio.file.StandardOpenOption.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import ctbrec.io.ModelJsonAdapter;
import ctbrec.sites.Site;
import okio.Buffer;
import okio.BufferedSource;

public class Config {

    private static final transient Logger LOG = LoggerFactory.getLogger(Config.class);

    private static Config instance;
    private Settings settings;
    private String filename;
    private List<Site> sites;
    private File configDir;

    private Config(List<Site> sites) throws FileNotFoundException, IOException {
        this.sites = sites;
        if(System.getProperty("ctbrec.config.dir") != null) {
            configDir = new File(System.getProperty("ctbrec.config.dir"));
        } else {
            configDir = OS.getConfigDir();
        }

        if(System.getProperty("ctbrec.config") != null) {
            filename = System.getProperty("ctbrec.config");
        } else {
            filename = "settings.json";
        }
        load();
    }

    private void load() throws FileNotFoundException, IOException {
        Moshi moshi = new Moshi.Builder()
                .add(Model.class, new ModelJsonAdapter(sites))
                .build();
        JsonAdapter<Settings> adapter = moshi.adapter(Settings.class);
        File configFile = new File(configDir, filename);
        LOG.debug("Loading config from {}", configFile.getAbsolutePath());
        if(configFile.exists()) {
            try(FileInputStream fin = new FileInputStream(configFile); Buffer buffer = new Buffer()) {
                BufferedSource source =  buffer.readFrom(fin);
                settings = adapter.fromJson(source);
                settings.httpTimeout = Math.max(settings.httpTimeout, 10_000);
            }
        } else {
            LOG.error("Config file does not exist. Falling back to default values.");
            settings = OS.getDefaultSettings();
        }
        for (Site site : sites) {
            site.setEnabled(!settings.disabledSites.contains(site.getName()));
        }
    }

    public static synchronized void init(List<Site> sites) throws FileNotFoundException, IOException {
        if(instance == null) {
            instance = new Config(sites);
        }
    }

    public static synchronized Config getInstance() {
        if(instance == null) {
            throw new IllegalStateException("Config not initialized, call init() first");
        }
        return instance;
    }

    public Settings getSettings() {
        return settings;
    }

    public void save() throws IOException {
        Moshi moshi = new Moshi.Builder()
                .add(Model.class, new ModelJsonAdapter())
                .build();
        JsonAdapter<Settings> adapter = moshi.adapter(Settings.class).indent("  ");
        String json = adapter.toJson(settings);
        File configFile = new File(configDir, filename);
        LOG.debug("Saving config to {}", configFile.getAbsolutePath());
        Files.createDirectories(configDir.toPath());
        Files.write(configFile.toPath(), json.getBytes("utf-8"), CREATE, WRITE, TRUNCATE_EXISTING);
    }

    public boolean isServerMode() {
        return Objects.equals(System.getProperty("ctbrec.server.mode"), "1");
    }

    public File getConfigDir() {
        return configDir;
    }

    public File getFileForRecording(Model model) {
        File dirForRecording = getDirForRecording(model);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
        String startTime = sdf.format(new Date());
        File targetFile = new File(dirForRecording, model.getName() + '_' + startTime + ".ts");
        if(getSettings().splitRecordings > 0) {
            LOG.debug("Splitting recordings every {} seconds", getSettings().splitRecordings);
            targetFile = new File(targetFile.getAbsolutePath().replaceAll("\\.ts", "-00000.ts"));
        }
        return targetFile;
    }

    private File getDirForRecording(Model model) {
        switch(getSettings().recordingsDirStructure) {
        case ONE_PER_MODEL:
            return new File(getSettings().recordingsDir, model.getName());
        case ONE_PER_RECORDING:
            File modelDir = new File(getSettings().recordingsDir, model.getName());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
            String startTime = sdf.format(new Date());
            return new File(modelDir, startTime);
        case FLAT:
        default:
            return new File(getSettings().recordingsDir);
        }
    }
}
