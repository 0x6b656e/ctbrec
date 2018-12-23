package ctbrec.recorder;

import static ctbrec.Recording.State.*;
import static ctbrec.event.Event.Type.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileStore;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.iheartradio.m3u8.Encoding;
import com.iheartradio.m3u8.Format;
import com.iheartradio.m3u8.ParseException;
import com.iheartradio.m3u8.ParsingMode;
import com.iheartradio.m3u8.PlaylistException;
import com.iheartradio.m3u8.PlaylistParser;
import com.iheartradio.m3u8.data.MediaPlaylist;
import com.iheartradio.m3u8.data.Playlist;
import com.iheartradio.m3u8.data.TrackData;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.MpegUtil;
import ctbrec.OS;
import ctbrec.Recording;
import ctbrec.Recording.State;
import ctbrec.event.Event;
import ctbrec.event.EventBusHolder;
import ctbrec.event.ModelIsOnlineEvent;
import ctbrec.event.RecordingStateChangedEvent;
import ctbrec.io.HttpClient;
import ctbrec.io.StreamRedirectThread;
import ctbrec.recorder.PlaylistGenerator.InvalidPlaylistException;
import ctbrec.recorder.download.Download;

public class LocalRecorder implements Recorder {

    private static final transient Logger LOG = LoggerFactory.getLogger(LocalRecorder.class);
    private static final boolean IGNORE_CACHE = true;
    private static final String DATE_FORMAT = "yyyy-MM-dd_HH-mm";

    private List<Model> models = Collections.synchronizedList(new ArrayList<>());
    private Map<Model, Download> recordingProcesses = Collections.synchronizedMap(new HashMap<>());
    private Map<File, PlaylistGenerator> playlistGenerators = new HashMap<>();
    private Config config;
    private ProcessMonitor processMonitor;
    private volatile boolean recording = true;
    private List<File> deleteInProgress = Collections.synchronizedList(new ArrayList<>());
    private RecorderHttpClient client = new RecorderHttpClient();
    private ReentrantLock lock = new ReentrantLock();
    private long lastSpaceMessage = 0;

    private ExecutorService ppThreadPool = Executors.newFixedThreadPool(2);

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

        registerEventBusListener();
        if(Config.isServerMode()) {
            processUnfinishedRecordings();
        }

        LOG.debug("Recorder initialized");
        LOG.info("Models to record: {}", models);
        LOG.info("Saving recordings in {}", config.getSettings().recordingsDir);
    }

    private void registerEventBusListener() {
        EventBusHolder.BUS.register(new Object() {
            @Subscribe
            public void modelEvent(Event e) {
                try {
                    if (e.getType() == MODEL_ONLINE) {
                        ModelIsOnlineEvent evt = (ModelIsOnlineEvent) e;
                        Model model = evt.getModel();
                        if(!isSuspended(model) && !recordingProcesses.containsKey(model)) {
                            startRecordingProcess(model);
                        }
                    }
                } catch (Exception e1) {
                    LOG.error("Error while handling model state changed event", e);
                }
            }
        });
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

        if(!enoughSpaceForRecording()) {
            long now = System.currentTimeMillis();
            if( (now - lastSpaceMessage) > TimeUnit.MINUTES.toMillis(1)) {
                LOG.info("Not enough space for recording, not starting recording for {}", model);
                lastSpaceMessage = now;
            }
            return;
        }

        LOG.debug("Starting recording for model {}", model.getName());
        Download download = model.createDownload();

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
        recordingProcesses.remove(model);
        fireRecordingStateChanged(download.getTarget(), STOPPED, model, download.getStartTime());

        Runnable stopAndThePostProcess = () -> {
            download.stop();
            createPostProcessor(download).run();
        };
        ppThreadPool.submit(stopAndThePostProcess);
    }

    private void postprocess(Download download) {
        String postProcessing = Config.getInstance().getSettings().postProcessing;
        if (postProcessing != null && !postProcessing.isEmpty()) {
            Runtime rt = Runtime.getRuntime();
            try {
                String[] args = new String[] {
                        postProcessing,
                        download.getTarget().getParentFile().getAbsolutePath(),
                        download.getTarget().getAbsolutePath(),
                        download.getModel().getName(),
                        download.getModel().getSite().getName(),
                        Long.toString(download.getStartTime().getEpochSecond())
                };
                LOG.debug("Running {}", Arrays.toString(args));
                Process process = rt.exec(args, OS.getEnvironment());
                // TODO maybe write these to a separate log file, e.g. recname.ts.pp.log
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
        processMonitor.running = false;
        LOG.debug("Stopping all recording processes");
        stopRecordingProcesses();
        ppThreadPool.shutdown();
        client.shutdown();
    }

    private void stopRecordingProcesses() {
        lock.lock();
        try {
            for (Model model : models) {
                Download recordingProcess = recordingProcesses.get(model);
                if (recordingProcess != null) {
                    stopRecordingProcess(model);
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
                try {
                    if(!enoughSpaceForRecording() && !recordingProcesses.isEmpty()) {
                        LOG.info("No space left -> Stopping all recordings");
                        stopRecordingProcesses();
                    }
                } catch (IOException e1) {
                    LOG.warn("Couldn't check free space left", e1);
                }

                List<Model> restart = new ArrayList<>();
                for (Iterator<Entry<Model, Download>> iterator = recordingProcesses.entrySet().iterator(); iterator.hasNext();) {
                    Entry<Model, Download> entry = iterator.next();
                    Model m = entry.getKey();
                    Download download = entry.getValue();
                    if (!download.isAlive()) {
                        LOG.debug("Recording terminated for model {}", m.getName());
                        iterator.remove();
                        restart.add(m);
                        fireRecordingStateChanged(download.getTarget(), STOPPED, m, download.getStartTime());
                        Runnable pp = createPostProcessor(download);
                        ppThreadPool.submit(pp);
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

    private void fireRecordingStateChanged(File path, Recording.State newState, Model model, Instant startTime) {
        RecordingStateChangedEvent evt = new RecordingStateChangedEvent(path, newState, model, startTime);
        EventBusHolder.BUS.post(evt);
    }

    /**
     * This is called once at start for server mode. When the server is killed, recordings are
     * left without playlist. This method creates playlists for them.
     */
    private void processUnfinishedRecordings() {
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
                        ppThreadPool.submit(() -> {
                            generatePlaylist(recDir);
                        });
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Unexpected error in playlist trigger", e);
        }
    }

    @Override
    public List<Recording> getRecordings() {
        if(Config.isServerMode()) {
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

    private State getStatus(Recording recording) {
        File absolutePath = new File(Config.getInstance().getSettings().recordingsDir, recording.getPath());

        PlaylistGenerator playlistGenerator = playlistGenerators.get(absolutePath);
        if (playlistGenerator != null) {
            recording.setProgress(playlistGenerator.getProgress());
            return GENERATING_PLAYLIST;
        }

        if (Config.isServerMode()) {
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
                        // don't list recordings, which currently get deleted
                        if (deleteInProgress.contains(rec)) {
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
        if (models.contains(model)) {
            int index = models.indexOf(model);
            models.get(index).setStreamUrlIndex(model.getStreamUrlIndex());
            config.save();
            LOG.debug("Switching stream source to index {} for model {}", model.getStreamUrlIndex(), model.getName());
            Download download = recordingProcesses.get(model);
            if(download != null) {
                stopRecordingProcess(model);
            }
            tryRestartRecording(model);
        } else {
            LOG.warn("Couldn't switch stream source for model {}. Not found in list", model.getName());
            return;
        }
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
                if(m.isOnline()) {
                    startRecordingProcess(m);
                }
                model.setSuspended(false);
                config.save();
            } else {
                LOG.warn("Couldn't resume model {}. Not found in list", model.getName());
                return;
            }
        } catch (ExecutionException | InterruptedException e) {
            LOG.error("Couldn't check, if model {} is online", model.getName());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public HttpClient getHttpClient() {
        return client;
    }

    @Override
    public long getTotalSpaceBytes() throws IOException {
        return getRecordingsFileStore().getTotalSpace();
    }

    @Override
    public long getFreeSpaceBytes() throws IOException {
        return getRecordingsFileStore().getUsableSpace();
    }

    private FileStore getRecordingsFileStore() throws IOException {
        File recordingsDir = new File(config.getSettings().recordingsDir);
        FileStore store = Files.getFileStore(recordingsDir.toPath());
        return store;
    }

    private boolean enoughSpaceForRecording() throws IOException {
        long minimum = config.getSettings().minimumSpaceLeftInBytes;
        if(minimum == 0) { // 0 means don't check
            return true;
        } else {
            return getFreeSpaceBytes() > minimum;
        }
    }

    private Runnable createPostProcessor(Download download) {
        return () -> {
            LOG.debug("Starting post-processing for {}", download.getTarget());
            if(Config.isServerMode()) {
                fireRecordingStateChanged(download.getTarget(), GENERATING_PLAYLIST, download.getModel(), download.getStartTime());
                generatePlaylist(download.getTarget());
            }
            boolean deleted = deleteIfTooShort(download);
            if(deleted) {
                // recording was too short. stop here and don't do post-processing
                return;
            }
            fireRecordingStateChanged(download.getTarget(), POST_PROCESSING, download.getModel(), download.getStartTime());
            postprocess(download);
            fireRecordingStateChanged(download.getTarget(), FINISHED, download.getModel(), download.getStartTime());
        };
    }


    // TODO maybe get file size and bitrate and check, if the values are plausible
    // we could also compare the length with the time elapsed since starting the recording
    private boolean deleteIfTooShort(Download download) {
        long minimumLengthInSeconds = Config.getInstance().getSettings().minimumLengthInSeconds;
        if(minimumLengthInSeconds <= 0) {
            return false;
        }

        try {
            LOG.debug("Determining video length for {}", download.getTarget());
            File target = download.getTarget();
            double duration = 0;
            if(target.isDirectory()) {
                File playlist = new File(target, "playlist.m3u8");
                duration = getPlaylistLength(playlist);
            } else {
                duration = MpegUtil.getFileDuration(target);
            }
            Duration minLength = Duration.ofSeconds(minimumLengthInSeconds);
            Duration videoLength = Duration.ofSeconds((long) duration);
            LOG.debug("Recording started at:{}. Video length is {}", download.getStartTime(), videoLength);
            if(videoLength.minus(minLength).isNegative()) {
                LOG.debug("Video too short {} {}", videoLength, download.getTarget());
                LOG.debug("Deleting {}", target);
                if(target.isDirectory()) {
                    deleteDirectory(target);
                    deleteEmptyParents(target);
                } else {
                    Files.delete(target.toPath());
                    deleteEmptyParents(target.getParentFile());
                }
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            LOG.error("Couldn't check video length", e);
            return false;
        }
    }

    private double getPlaylistLength(File playlist) throws IOException, ParseException, PlaylistException {
        if(playlist.exists()) {
            PlaylistParser playlistParser = new PlaylistParser(new FileInputStream(playlist), Format.EXT_M3U, Encoding.UTF_8, ParsingMode.LENIENT);
            Playlist m3u = playlistParser.parse();
            MediaPlaylist mediaPlaylist = m3u.getMediaPlaylist();
            double length = 0;
            for (TrackData trackData : mediaPlaylist.getTracks()) {
                length += trackData.getTrackInfo().duration;
            }
            return length;
        } else {
            throw new FileNotFoundException(playlist.getAbsolutePath() + " does not exist");
        }
    }
}
