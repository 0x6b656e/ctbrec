package ctbrec.recorder;

import static ctbrec.Recording.STATUS.FINISHED;
import static ctbrec.Recording.STATUS.GENERATING_PLAYLIST;
import static ctbrec.Recording.STATUS.RECORDING;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iheartradio.m3u8.ParseException;
import com.iheartradio.m3u8.PlaylistException;

import ctbrec.ChaturbateModel;
import ctbrec.Config;
import ctbrec.HttpClient;
import ctbrec.Model;
import ctbrec.ModelParser;
import ctbrec.Recording;
import ctbrec.recorder.PlaylistGenerator.InvalidPlaylistException;
import ctbrec.recorder.download.Download;
import ctbrec.recorder.download.HlsDownload;
import ctbrec.recorder.download.MergedHlsDownload;
import okhttp3.Request;
import okhttp3.Response;

public class LocalRecorder implements Recorder {

    private static final transient Logger LOG = LoggerFactory.getLogger(LocalRecorder.class);

    private static final boolean IGNORE_CACHE = true;
    private List<Model> followedModels = Collections.synchronizedList(new ArrayList<>());
    private List<Model> models = Collections.synchronizedList(new ArrayList<>());
    private Map<Model, Download> recordingProcesses = Collections.synchronizedMap(new HashMap<>());
    private Map<File, PlaylistGenerator> playlistGenerators = new HashMap<>();
    private Config config;
    private ProcessMonitor processMonitor;
    private OnlineMonitor onlineMonitor;
    private FollowedMonitor followedMonitor;
    private PlaylistGeneratorTrigger playlistGenTrigger;
    private HttpClient client = HttpClient.getInstance();
    private volatile boolean recording = true;
    private List<File> deleteInProgress = Collections.synchronizedList(new ArrayList<>());

    public LocalRecorder(Config config) {
        this.config = config;
        config.getSettings().models.stream().forEach((m) -> {
            models.add(m);
        });

        recording = true;
        processMonitor = new ProcessMonitor();
        processMonitor.start();
        onlineMonitor = new OnlineMonitor();
        onlineMonitor.start();

        playlistGenTrigger = new PlaylistGeneratorTrigger();
        if(Config.getInstance().isServerMode()) {
            playlistGenTrigger.start();
        }

        if (config.getSettings().recordFollowed) {
            followedMonitor = new FollowedMonitor();
            followedMonitor.start();
        }

        LOG.debug("Recorder initialized");
        LOG.info("Models to record: {}", models);
        LOG.info("Saving recordings in {}", config.getSettings().recordingsDir);
    }

    @Override
    public void startRecording(Model model) {
        if (!models.contains(model)) {
            LOG.info("Model {} added", model);
            if (followedModels.contains(model)) {
                followedModels.remove(model);
            }
            models.add(model);
            config.getSettings().models.add(model);
        }
    }

    @Override
    public void stopRecording(Model model) throws IOException {
        if (models.contains(model) || followedModels.contains(model)) {
            models.remove(model);
            followedModels.remove(model);
            config.getSettings().models.remove(model);
            if (recordingProcesses.containsKey(model)) {
                stopRecordingProcess(model);
            }
            LOG.info("Model {} removed", model);
        } else {
            throw new NoSuchElementException("Model " + model.getName() + " ["+model.getUrl()+"] not found in list of recorded models");
        }
    }

    private void startRecordingProcess(Model model) throws IOException {
        LOG.debug("Restart recording for model {}", model.getName());
        if (recordingProcesses.containsKey(model)) {
            LOG.error("A recording for model {} is already running", model);
            return;
        }

        if (!models.contains(model) && !followedModels.contains(model)) {
            LOG.info("Model {} has been removed. Restarting of recording cancelled.", model);
            return;
        }

        Download download;
        if (Config.getInstance().isServerMode()) {
            download = new HlsDownload(client);
        } else {
            download = new MergedHlsDownload(client);
        }

        recordingProcesses.put(model, download);
        new Thread() {
            @Override
            public void run() {
                try {
                    download.start(model, config);
                } catch (IOException e) {
                    LOG.error("Download failed. Download alive: {}", download.isAlive(), e);
                }
            }
        }.start();
    }

    private void stopRecordingProcess(Model model) throws IOException {
        Download download = recordingProcesses.get(model);
        download.stop();
        recordingProcesses.remove(model);
    }

    @Override
    public boolean isRecording(Model model) {
        return models.contains(model) || followedModels.contains(model);
    }

    @Override
    public List<Model> getModelsRecording() {
        List<Model> union = new ArrayList<>();
        union.addAll(models);
        union.addAll(followedModels);
        return Collections.unmodifiableList(union);
    }

    @Override
    public void shutdown() {
        LOG.info("Shutting down");
        recording = false;
        LOG.debug("Stopping monitor threads");
        onlineMonitor.running = false;
        processMonitor.running = false;
        playlistGenTrigger.running = false;
        if (followedMonitor != null) {
            followedMonitor.running = false;
        }
        LOG.debug("Stopping all recording processes");
        stopRecordingProcesses();
    }

    private void stopRecordingProcesses() {
        for (Model model : models) {
            Download recordingProcess = recordingProcesses.get(model);
            if (recordingProcess != null) {
                try {
                    recordingProcess.stop();
                    LOG.debug("Stopped recording for {}", model);
                } catch (Exception e) {
                    LOG.error("Couldn't stop recording for model {}", model, e);
                }
            }
        }
    }

    private void tryRestartRecording(Model model) {
        if (!recording) {
            // recorder is not in recording state
            return;
        }

        try {
            boolean modelInRecordingList = isRecording(model);
            boolean online = model.isOnline(IGNORE_CACHE);
            if (modelInRecordingList && online) {
                LOG.info("Restarting recording for model {}", model);
                recordingProcesses.remove(model);
                startRecordingProcess(model);
            }
        } catch (Exception e) {
            LOG.error("Couldn't restart recording for model {}", model);
        }
    }

    private class ProcessMonitor extends Thread {
        private volatile boolean running = false;

        public ProcessMonitor() {
            setName("ProcessMonitor");
            setDaemon(true);
        }

        @Override
        public void run() {
            running = true;
            while (running) {
                List<Model> restart = new ArrayList<>();
                for (Iterator<Entry<Model, Download>> iterator = recordingProcesses.entrySet().iterator(); iterator.hasNext();) {
                    Entry<Model, Download> entry = iterator.next();
                    Model m = entry.getKey();
                    Download d = entry.getValue();
                    if (!d.isAlive()) {
                        LOG.debug("Recording terminated for model {}", m.getName());
                        iterator.remove();
                        restart.add(m);
                        try {
                            finishRecording(d.getDirectory());
                        } catch(NullPointerException e) {}//fail silently
                    }
                }
                for (Model m : restart) {
                    tryRestartRecording(m);
                }

                try {
                    if (running)
                        Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOG.error("Couldn't sleep", e);
                }
            }
            LOG.debug(getName() + " terminated");
        }
    }

    private class FollowedMonitor extends Thread {
        private volatile boolean running = false;

        public FollowedMonitor() {
            setName("FollowedMonitor");
            setDaemon(true);
        }

        @Override
        public void run() {
            running = true;
            while (running) {
                try {
                    String url = "https://chaturbate.com/followed-cams/?page=1&keywords=&_=" + System.currentTimeMillis();
                    LOG.debug("Fetching page {}", url);
                    Request request = new Request.Builder().url(url).build();
                    Response response = client.execute(request, true);
                    if (response.isSuccessful()) {
                        List<ChaturbateModel> followed = ModelParser.parseModels(response.body().string());
                        response.close();
                        followedModels.clear();
                        for (ChaturbateModel model : followed) {
                            if (!followedModels.contains(model) && !models.contains(model)) {
                                LOG.info("Model {} added", model);
                                followedModels.add(model);
                            }
                        }
                        onlineMonitor.interrupt();
                    } else {
                        int code = response.code();
                        response.close();
                        LOG.error("Couldn't retrieve followed models. HTTP status {}", code);
                    }
                } catch (IOException e) {
                    LOG.error("Couldn't retrieve followed models.", e);
                }

                try {
                    if (running)
                        Thread.sleep(10000);
                } catch (InterruptedException e) {
                    LOG.error("Couldn't sleep", e);
                }
            }
            LOG.debug(getName() + " terminated");
        }
    }

    private void finishRecording(File directory) {
        Thread t = new Thread() {
            @Override
            public void run() {
                if(Config.getInstance().isServerMode()) {
                    generatePlaylist(directory);
                }
            }
        };
        t.setDaemon(true);
        t.setName("Postprocessing" + directory.toString());
        t.start();
    }

    private void generatePlaylist(File recDir) {
        PlaylistGenerator playlistGenerator = new PlaylistGenerator();
        playlistGenerators.put(recDir, playlistGenerator);
        try {
            File playlist = playlistGenerator.generate(recDir);
            if(playlist != null) {
                playlistGenerator.validate(recDir);
            }
        } catch (IOException | ParseException | PlaylistException e) {
            LOG.error("Couldn't generate playlist file", e);
        } catch (InvalidPlaylistException e) {
            LOG.error("Playlist is invalid and will be deleted", e);
            File playlist = new File(recDir, "playlist.m3u8");
            playlist.delete();
        } finally {
            playlistGenerators.remove(recDir);
        }
    }

    private class OnlineMonitor extends Thread {
        private volatile boolean running = false;

        public OnlineMonitor() {
            setName("OnlineMonitor");
            setDaemon(true);
        }

        @Override
        public void run() {
            running = true;
            while (running) {
                for (Model model : getModelsRecording()) {
                    try {
                        if (!recordingProcesses.containsKey(model)) {
                            boolean isOnline = model.isOnline(IGNORE_CACHE);
                            LOG.trace("Checking online state for {}: {}", model, (isOnline ? "online" : "offline"));
                            if (isOnline) {
                                LOG.info("Model {}'s room back to public. Starting recording", model);
                                startRecordingProcess(model);
                            }
                        }
                    } catch (Exception e) {
                        LOG.error("Couldn't check if model {} is online", model.getName(), e);
                    }
                }

                try {
                    if (running)
                        Thread.sleep(10000);
                } catch (InterruptedException e) {
                    LOG.trace("Sleep interrupted");
                }
            }
            LOG.debug(getName() + " terminated");
        }
    }

    private class PlaylistGeneratorTrigger extends Thread {
        private volatile boolean running = false;

        public PlaylistGeneratorTrigger() {
            setName("PlaylistGeneratorTrigger");
            setDaemon(true);
        }

        @Override
        public void run() {
            running = true;
            while (running) {
                try {
                    List<Recording> recs = getRecordings();
                    for (Recording rec : recs) {
                        if (rec.getStatus() == RECORDING) {
                            boolean recordingProcessFound = false;
                            File recordingsDir = new File(config.getSettings().recordingsDir);
                            File recDir = new File(recordingsDir, rec.getPath());
                            for (Entry<Model, Download> download : recordingProcesses.entrySet()) {
                                if (download.getValue().getDirectory().equals(recDir)) {
                                    recordingProcessFound = true;
                                }
                            }
                            if (!recordingProcessFound) {
                                if (deleteInProgress.contains(recDir)) {
                                    LOG.debug("{} is being deleted. Not going to generate a playlist", recDir);
                                } else {
                                    finishRecording(recDir);
                                }
                            }
                        }
                    }

                    if (running)
                        Thread.sleep(10000);
                } catch (InterruptedException e) {
                    LOG.error("Couldn't sleep", e);
                } catch (Exception e) {
                    LOG.error("Unexpected error in playlist trigger thread", e);
                }
            }
            LOG.debug(getName() + " terminated");
        }
    }

    @Override
    public List<Recording> getRecordings() {
        List<Recording> recordings = new ArrayList<>();
        File recordingsDir = new File(config.getSettings().recordingsDir);
        File[] subdirs = recordingsDir.listFiles();
        if (subdirs == null) {
            return Collections.emptyList();
        }

        for (File subdir : subdirs) {
            // only consider directories
            if (!subdir.isDirectory()) {
                continue;
            }

            // ignore empty directories
            File[] recordingsDirs = subdir.listFiles();
            if(recordingsDirs.length == 0) {
                continue;
            }

            // start going over valid directories
            for (File rec : recordingsDirs) {
                String pattern = "yyyy-MM-dd_HH-mm";
                SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                if (rec.isDirectory()) {
                    try {
                        // ignore directories, which are probably not created by ctbrec
                        if (rec.getName().length() != pattern.length()) {
                            continue;
                        }
                        // ignore empty directories
                        if (rec.listFiles().length == 0) {
                            continue;
                        }

                        Date startDate = sdf.parse(rec.getName());
                        Recording recording = new Recording();
                        recording.setModelName(subdir.getName());
                        recording.setStartDate(Instant.ofEpochMilli(startDate.getTime()));
                        recording.setPath(recording.getModelName() + "/" + rec.getName());
                        recording.setSizeInByte(getSize(rec));
                        File playlist = new File(rec, "playlist.m3u8");
                        recording.setHasPlaylist(playlist.exists());

                        PlaylistGenerator playlistGenerator = playlistGenerators.get(rec);
                        if (playlistGenerator != null) {
                            recording.setStatus(GENERATING_PLAYLIST);
                            recording.setProgress(playlistGenerator.getProgress());
                        } else {
                            if (config.isServerMode()) {
                                if (recording.hasPlaylist()) {
                                    recording.setStatus(FINISHED);
                                } else {
                                    recording.setStatus(RECORDING);
                                }
                            } else {
                                boolean dirUsedByRecordingProcess = false;
                                for (Download download : recordingProcesses.values()) {
                                    if(rec.equals(download.getDirectory())) {
                                        dirUsedByRecordingProcess = true;
                                        break;
                                    }
                                }
                                if(dirUsedByRecordingProcess) {
                                    recording.setStatus(RECORDING);
                                } else {
                                    recording.setStatus(FINISHED);
                                }
                            }
                        }
                        recordings.add(recording);
                    } catch (Exception e) {
                        LOG.debug("Ignoring {} - {}", rec.getAbsolutePath(), e.getMessage());
                    }
                }
            }
        }
        return recordings;
    }

    private long getSize(File rec) {
        long size = 0;
        File[] files = rec.listFiles();
        for (File file : files) {
            size += file.length();
        }
        return size;
    }

    @Override
    public void delete(Recording recording) throws IOException {
        File recordingsDir = new File(config.getSettings().recordingsDir);
        File directory = new File(recordingsDir, recording.getPath());
        delete(directory);
    }

    private void delete(File directory) throws IOException {
        if (!directory.exists()) {
            throw new IOException("Recording does not exist");
        }

        try {
            deleteInProgress.add(directory);
            File[] files = directory.listFiles();
            boolean deletedAllFiles = true;
            for (File file : files) {
                try {
                    LOG.trace("Deleting {}", file.getAbsolutePath());
                    Files.delete(file.toPath());
                } catch (Exception e) {
                    deletedAllFiles = false;
                    LOG.debug("Couldn't delete {}", file, e);
                }
            }

            if (deletedAllFiles) {
                LOG.debug("All files deleted");
                if (directory.list().length == 0) {
                    LOG.debug("Deleting directory {}", directory);
                    boolean deleted = directory.delete();
                    if (deleted) {
                        if (directory.getParentFile().list().length == 0) {
                            LOG.debug("Deleting parent directory {}", directory.getParentFile());
                            directory.getParentFile().delete();
                        }
                    } else {
                        throw new IOException("Couldn't delete " + directory);
                    }
                }
            } else {
                throw new IOException("Couldn't delete all files in " + directory);
            }
        } finally {
            deleteInProgress.remove(directory);
        }
    }

    @Override
    public void switchStreamSource(Model model) throws IOException, InvalidKeyException, NoSuchAlgorithmException, IllegalStateException {
        LOG.debug("Switching stream source to index {} for model {}", model.getStreamUrlIndex(), model.getName());
        stopRecordingProcess(model);
        tryRestartRecording(model);
    }
}
