package ctbrec.recorder;

import static ctbrec.Recording.STATUS.*;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iheartradio.m3u8.ParseException;
import com.iheartradio.m3u8.PlaylistException;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.OS;
import ctbrec.Recording;
import ctbrec.Recording.STATUS;
import ctbrec.io.HttpClient;
import ctbrec.io.HttpException;
import ctbrec.io.StreamRedirectThread;
import ctbrec.recorder.PlaylistGenerator.InvalidPlaylistException;
import ctbrec.recorder.download.Download;
import ctbrec.recorder.download.HlsDownload;
import ctbrec.recorder.download.MergedHlsDownload;

public class LocalRecorder implements Recorder {

    private static final transient Logger LOG = LoggerFactory.getLogger(LocalRecorder.class);
    private static final boolean IGNORE_CACHE = true;
    private static final String DATE_FORMAT = "yyyy-MM-dd_HH-mm";

    private List<Model> models = Collections.synchronizedList(new ArrayList<>());
    private Map<Model, Download> recordingProcesses = Collections.synchronizedMap(new HashMap<>());
    private Map<File, PlaylistGenerator> playlistGenerators = new HashMap<>();
    private Config config;
    private ProcessMonitor processMonitor;
    private OnlineMonitor onlineMonitor;
    private PostProcessingTrigger postProcessingTrigger;
    private volatile boolean recording = true;
    private List<File> deleteInProgress = Collections.synchronizedList(new ArrayList<>());
    private RecorderHttpClient client = new RecorderHttpClient();
    private ReentrantLock lock = new ReentrantLock();

    public LocalRecorder(Config config) {
        this.config = config;
        config.getSettings().models.stream().forEach((m) -> {
            if(m.getSite().isEnabled()) {
                models.add(m);
            } else {
                LOG.info("{} disabled -> ignoring {}", m.getSite().getName(), m.getName());
            }
        });

        recording = true;
        processMonitor = new ProcessMonitor();
        processMonitor.start();
        onlineMonitor = new OnlineMonitor();
        onlineMonitor.start();

        postProcessingTrigger = new PostProcessingTrigger();
        if(Config.getInstance().isServerMode()) {
            postProcessingTrigger.start();
        }

        LOG.debug("Recorder initialized");
        LOG.info("Models to record: {}", models);
        LOG.info("Saving recordings in {}", config.getSettings().recordingsDir);
    }

    @Override
    public void startRecording(Model model) {
        if (!models.contains(model)) {
            LOG.info("Model {} added", model);
            lock.lock();
            try {
                models.add(model);
                config.getSettings().models.add(model);
                config.save();
            } catch (IOException e) {
                LOG.error("Couldn't save config", e);
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public void stopRecording(Model model) throws IOException {
        lock.lock();
        try {
            if (models.contains(model)) {
                models.remove(model);
                config.getSettings().models.remove(model);
                if (recordingProcesses.containsKey(model)) {
                    stopRecordingProcess(model);
                }
                LOG.info("Model {} removed", model);
                config.save();
            } else {
                throw new NoSuchElementException("Model " + model.getName() + " ["+model.getUrl()+"] not found in list of recorded models");
            }
        } finally {
            lock.unlock();
        }
    }

    private void startRecordingProcess(Model model) throws IOException {
        if(model.isSuspended()) {
            LOG.info("Recording for model {} is suspended.", model);
            return;
        }

        LOG.debug("Starting recording for model {}", model.getName());
        if (recordingProcesses.containsKey(model)) {
            LOG.error("A recording for model {} is already running", model);
            return;
        }

        lock.lock();
        try {
            if (!models.contains(model)) {
                LOG.info("Model {} has been removed. Restarting of recording cancelled.", model);
                return;
            }
        } finally {
            lock.unlock();
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
                    LOG.error("Download for {} failed. Download alive: {}", model.getName(), download.isAlive(), e);
                }
            }
        }.start();
    }

    private void stopRecordingProcess(Model model)  {
        Download download = recordingProcesses.get(model);
        download.stop();
        recordingProcesses.remove(model);
        if(!Config.getInstance().isServerMode()) {
            postprocess(download);
        }
    }

    private void postprocess(Download download) {
        if(!(download instanceof MergedHlsDownload)) {
            throw new IllegalArgumentException("Download should be of type MergedHlsDownload");
        }
        String postProcessing = Config.getInstance().getSettings().postProcessing;
        if (postProcessing != null && !postProcessing.isEmpty()) {
            new Thread(() -> {
                Runtime rt = Runtime.getRuntime();
                try {
                    MergedHlsDownload d = (MergedHlsDownload) download;
                    String[] args = new String[] {
                            postProcessing,
                            d.getTarget().getParentFile().getAbsolutePath(),
                            d.getTarget().getAbsolutePath(),
                            d.getModel().getName(),
                            d.getModel().getSite().getName(),
                            Long.toString(download.getStartTime().getEpochSecond())
                    };
                    LOG.debug("Running {}", Arrays.toString(args));
                    Process process = rt.exec(args, OS.getEnvironment());
                    Thread std = new Thread(new StreamRedirectThread(process.getInputStream(), System.out));
                    std.setName("Process stdout pipe");
                    std.setDaemon(true);
                    std.start();
                    Thread err = new Thread(new StreamRedirectThread(process.getErrorStream(), System.err));
                    err.setName("Process stderr pipe");
                    err.setDaemon(true);
                    err.start();

                    process.waitFor();
                    LOG.debug("Process finished.");
                } catch (Exception e) {
                    LOG.error("Error in process thread", e);
                }
            }).start();
        }
    }

    @Override
    public boolean isRecording(Model model) {
        lock.lock();
        try {
            return models.contains(model);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isSuspended(Model model) {
        lock.lock();
        try {
            int index = models.indexOf(model);
            if(index >= 0) {
                Model m = models.get(index);
                return m.isSuspended();
            } else {
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<Model> getModelsRecording() {
        lock.lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(models));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<Model> getOnlineModels() {
        return getModelsRecording()
                .stream()
                .filter(m -> {
                    try {
                        return m.isOnline();
                    } catch (IOException | ExecutionException | InterruptedException e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public void shutdown() {
        LOG.info("Shutting down");
        recording = false;
        LOG.debug("Stopping monitor threads");
        onlineMonitor.running = false;
        processMonitor.running = false;
        postProcessingTrigger.running = false;
        LOG.debug("Stopping all recording processes");
        stopRecordingProcesses();
        client.shutdown();
    }

    private void stopRecordingProcesses() {
        lock.lock();
        try {
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
        } finally {
            lock.unlock();
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
                        if(config.isServerMode()) {
                            try {
                                finishRecording(d.getTarget());
                            } catch(Exception e) {
                                LOG.error("Error while finishing recording for model {}", m.getName(), e);
                            }
                        } else {
                            postprocess(d);
                        }
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

    private void finishRecording(File directory) {
        if(Config.getInstance().isServerMode()) {
            Thread t = new Thread() {
                @Override
                public void run() {
                    generatePlaylist(directory);
                }
            };
            t.setDaemon(true);
            t.setName("Post-Processing " + directory.toString());
            t.start();
        }
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
                Instant begin = Instant.now();
                for (Model model : getModelsRecording()) {
                    try {
                        boolean isOnline = model.isOnline(IGNORE_CACHE);
                        LOG.trace("Checking online state for {}: {}", model, (isOnline ? "online" : "offline"));
                        if (isOnline && !isSuspended(model) && !recordingProcesses.containsKey(model)) {
                            LOG.info("Model {}'s room back to public. Starting recording", model);
                            startRecordingProcess(model);
                        }
                    } catch (HttpException e) {
                        LOG.error("Couldn't check if model {} is online. HTTP Response: {} - {}",
                                model.getName(), e.getResponseCode(), e.getResponseMessage());
                    } catch (Exception e) {
                        LOG.error("Couldn't check if model {} is online", model.getName(), e);
                    }
                }
                Instant end = Instant.now();
                Duration timeCheckTook = Duration.between(begin, end);

                long sleepTime = Config.getInstance().getSettings().onlineCheckIntervalInSecs;
                if(timeCheckTook.getSeconds() < sleepTime) {
                    try {
                        if (running) {
                            long millis = TimeUnit.SECONDS.toMillis(sleepTime - timeCheckTook.getSeconds());
                            LOG.trace("Sleeping {}ms", millis);
                            Thread.sleep(millis);
                        }
                    } catch (InterruptedException e) {
                        LOG.trace("Sleep interrupted");
                    }
                }
            }
            LOG.debug(getName() + " terminated");
        }
    }

    private class PostProcessingTrigger extends Thread {
        private volatile boolean running = false;

        public PostProcessingTrigger() {
            setName("PostProcessingTrigger");
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
                                if (download.getValue().getTarget().equals(recDir)) {
                                    recordingProcessFound = true;
                                }
                            }
                            if (!recordingProcessFound) {
                                if (deleteInProgress.contains(recDir)) {
                                    LOG.debug("{} is being deleted. Not going to start post-processing", recDir);
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
        if(Config.getInstance().isServerMode()) {
            return listSegmentedRecordings();
        } else {
            return listMergedRecordings();
        }
    }

    private List<Recording> listMergedRecordings() {
        File recordingsDir = new File(config.getSettings().recordingsDir);
        List<File> possibleRecordings = new LinkedList<>();
        listRecursively(recordingsDir, possibleRecordings, (dir, name) -> name.matches(".*?_\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}\\.ts"));
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        List<Recording> recordings = new ArrayList<>();
        for (File ts: possibleRecordings) {
            try {
                String filename = ts.getName();
                String dateString = filename.substring(filename.length() - 3 - DATE_FORMAT.length(), filename.length() - 3);
                Date startDate = sdf.parse(dateString);
                Recording recording = new Recording();
                recording.setModelName(filename.substring(0, filename.length() - 4 - DATE_FORMAT.length()));
                recording.setStartDate(Instant.ofEpochMilli(startDate.getTime()));
                String path = ts.getAbsolutePath().replace(config.getSettings().recordingsDir, "");
                if(!path.startsWith("/")) {
                    path = '/' + path;
                }
                recording.setPath(path);
                recording.setSizeInByte(ts.length());
                recording.setStatus(getStatus(recording));
                recordings.add(recording);
            } catch(Exception e) {
                LOG.error("Ignoring {} - {}", ts.getAbsolutePath(), e.getMessage());
            }
        }
        return recordings;
    }

    private STATUS getStatus(Recording recording) {
        File absolutePath = new File(Config.getInstance().getSettings().recordingsDir, recording.getPath());

        PlaylistGenerator playlistGenerator = playlistGenerators.get(absolutePath);
        if (playlistGenerator != null) {
            recording.setProgress(playlistGenerator.getProgress());
            return GENERATING_PLAYLIST;
        }

        if (config.isServerMode()) {
            if (recording.hasPlaylist()) {
                return FINISHED;
            } else {
                return RECORDING;
            }
        } else {
            boolean dirUsedByRecordingProcess = false;
            for (Download download : recordingProcesses.values()) {
                if(absolutePath.equals(download.getTarget())) {
                    dirUsedByRecordingProcess = true;
                    break;
                }
            }
            if(dirUsedByRecordingProcess) {
                return RECORDING;
            } else {
                return FINISHED;
            }
        }
    }

    private List<Recording> listSegmentedRecordings() {
        List<Recording> recordings = new ArrayList<>();
        File recordingsDir = new File(config.getSettings().recordingsDir);
        File[] subdirs = recordingsDir.listFiles();
        if (subdirs == null) {
            return Collections.emptyList();
        }

        for (File subdir : subdirs) {
            // ignore empty directories
            File[] recordingsDirs = subdir.listFiles();
            if(recordingsDirs == null || recordingsDirs.length == 0) {
                continue;
            }

            // start going over valid directories
            for (File rec : recordingsDirs) {
                SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
                if (rec.isDirectory()) {
                    try {
                        // ignore directories, which are probably not created by ctbrec
                        if (rec.getName().length() != DATE_FORMAT.length()) {
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
                        recording.setStatus(getStatus(recording));
                        recordings.add(recording);
                    } catch (Exception e) {
                        LOG.debug("Ignoring {} - {}", rec.getAbsolutePath(), e.getMessage());
                    }
                }
            }
        }
        return recordings;
    }

    private void listRecursively(File dir, List<File> result, FilenameFilter filenameFilter) {
        File[] files = dir.listFiles();
        if(files != null) {
            for (File file : files) {
                if(file.isDirectory()) {
                    listRecursively(file, result, filenameFilter);
                }
                if(filenameFilter.accept(dir, file.getName())) {
                    result.add(file);
                }
            }
        }
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
        File path = new File(recordingsDir, recording.getPath());
        LOG.debug("Deleting {}", path);

        if(path.isFile()) {
            Files.delete(path.toPath());
            deleteEmptyParents(path.getParentFile());
        } else {
            deleteDirectory(path);
            deleteEmptyParents(path);
        }
    }

    private void deleteEmptyParents(File parent) throws IOException {
        File recDir = new File(Config.getInstance().getSettings().recordingsDir);
        while(parent != null && parent.list() != null && parent.list().length == 0) {
            if(parent.equals(recDir)) {
                return;
            }
            LOG.debug("Deleting empty directory {}", parent.getAbsolutePath());
            Files.delete(parent.toPath());
            parent = parent.getParentFile();
        }
    }

    private void deleteDirectory(File directory) throws IOException {
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

            if (!deletedAllFiles) {
                throw new IOException("Couldn't delete all files in " + directory);
            }
        } finally {
            deleteInProgress.remove(directory);
        }
    }

    @Override
    public void switchStreamSource(Model model) throws IOException, InvalidKeyException, NoSuchAlgorithmException, IllegalStateException {
        LOG.debug("Switching stream source to index {} for model {}", model.getStreamUrlIndex(), model.getName());
        Download download = recordingProcesses.get(model);
        if(download != null) {
            stopRecordingProcess(model);
        }
        tryRestartRecording(model);
        config.save();
    }

    @Override
    public void suspendRecording(Model model) {
        lock.lock();
        try {
            if (models.contains(model)) {
                int index = models.indexOf(model);
                models.get(index).setSuspended(true);
                model.setSuspended(true);
                config.save();
            } else {
                LOG.warn("Couldn't suspend model {}. Not found in list", model.getName());
                return;
            }
        } catch (IOException e) {
            LOG.error("Couldn't save config", e);
        } finally {
            lock.unlock();
        }

        Download download = recordingProcesses.get(model);
        if(download != null) {
            stopRecordingProcess(model);
        }
    }

    @Override
    public void resumeRecording(Model model) throws IOException {
        lock.lock();
        try {
            if (models.contains(model)) {
                int index = models.indexOf(model);
                Model m = models.get(index);
                m.setSuspended(false);
                startRecordingProcess(m);
                model.setSuspended(false);
                config.save();
            } else {
                LOG.warn("Couldn't resume model {}. Not found in list", model.getName());
                return;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public HttpClient getHttpClient() {
        return client;
    }
}
