package ctbrec.recorder;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iheartradio.m3u8.ParseException;
import com.iheartradio.m3u8.PlaylistException;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import ctbrec.AbstractModel;
import ctbrec.Config;
import ctbrec.Hmac;
import ctbrec.Model;
import ctbrec.Recording;
import ctbrec.event.EventBusHolder;
import ctbrec.event.RecordingStateChangedEvent;
import ctbrec.io.HttpClient;
import ctbrec.io.HttpException;
import ctbrec.io.InstantJsonAdapter;
import ctbrec.io.ModelJsonAdapter;
import ctbrec.recorder.download.StreamSource;
import ctbrec.sites.Site;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RemoteRecorder implements Recorder {

    private static final transient Logger LOG = LoggerFactory.getLogger(RemoteRecorder.class);
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private Moshi moshi = new Moshi.Builder()
            .add(Instant.class, new InstantJsonAdapter())
            .add(Model.class, new ModelJsonAdapter())
            .build();
    private JsonAdapter<ModelListResponse> modelListResponseAdapter = moshi.adapter(ModelListResponse.class);
    private JsonAdapter<RecordingListResponse> recordingListResponseAdapter = moshi.adapter(RecordingListResponse.class);
    private JsonAdapter<ModelRequest> modelRequestAdapter = moshi.adapter(ModelRequest.class);

    private List<Model> models = Collections.emptyList();
    private List<Model> onlineModels = Collections.emptyList();
    private List<Recording> recordings = Collections.emptyList();
    private List<Site> sites;
    private long spaceTotal = -1;
    private long spaceFree = -1;

    private Config config;
    private HttpClient client;
    private Instant lastSync = Instant.EPOCH;
    private SyncThread syncThread;


    public RemoteRecorder(Config config, HttpClient client, List<Site> sites) {
        this.config = config;
        this.client = client;
        this.sites = sites;

        syncThread = new SyncThread();
        syncThread.start();
    }

    @Override
    public void startRecording(Model model) throws IOException, InvalidKeyException, NoSuchAlgorithmException, IllegalStateException {
        sendRequest("start", model);
    }

    @Override
    public void stopRecording(Model model) throws IOException, InvalidKeyException, NoSuchAlgorithmException, IllegalStateException {
        sendRequest("stop", model);
    }

    private void sendRequest(String action, Model model) throws IOException, InvalidKeyException, NoSuchAlgorithmException, IllegalStateException {
        String payload = modelRequestAdapter.toJson(new ModelRequest(action, model));
        LOG.debug("Sending request to recording server: {}", payload);
        RequestBody body = RequestBody.create(JSON, payload);
        Request.Builder builder = new Request.Builder()
                .url("http://" + config.getSettings().httpServer + ":" + config.getSettings().httpPort + "/rec")
                .post(body);
        addHmacIfNeeded(payload, builder);
        Request request = builder.build();
        try (Response response = client.execute(request)) {
            String json = response.body().string();
            if (response.isSuccessful()) {
                ModelListResponse resp = modelListResponseAdapter.fromJson(json);
                if (!resp.status.equals("success")) {
                    throw new IOException("Server returned error " + resp.status + " " + resp.msg);
                }

                if ("start".equals(action)) {
                    models.add(model);
                } else if ("stop".equals(action)) {
                    models.remove(model);
                    onlineModels.remove(model);
                }
            } else {
                throw new HttpException(response.code(), response.message());
            }
        }
    }

    private void addHmacIfNeeded(String msg, Builder builder) throws InvalidKeyException, NoSuchAlgorithmException, IllegalStateException, UnsupportedEncodingException {
        if(Config.getInstance().getSettings().requireAuthentication) {
            byte[] key = Config.getInstance().getSettings().key;
            String hmac = Hmac.calculate(msg, key);
            builder.addHeader("CTBREC-HMAC", hmac);
        }
    }

    @Override
    public boolean isRecording(Model model) {
        return models != null && models.contains(model);
    }

    @Override
    public boolean isSuspended(Model model) {
        int index = models.indexOf(model);
        if(index >= 0) {
            Model m = models.get(index);
            return m.isSuspended();
        } else {
            return false;
        }
    }

    @Override
    public List<Model> getModelsRecording() {
        if(!lastSync.equals(Instant.EPOCH) && lastSync.isBefore(Instant.now().minusSeconds(60))) {
            throw new RuntimeException("Last sync was over a minute ago");
        }
        return new ArrayList<Model>(models);
    }

    @Override
    public void shutdown() {
        syncThread.stopThread();
    }

    private class SyncThread extends Thread {
        private volatile boolean running = false;

        public SyncThread() {
            setName("RemoteRecorder SyncThread");
            setDaemon(true);
        }

        @Override
        public void run() {
            running = true;
            while(running) {
                syncModels();
                syncOnlineModels();
                syncSpace();
                syncRecordings();
                sleep();
            }
        }

        private void syncSpace() {
            try {
                String msg = "{\"action\": \"space\"}";
                RequestBody body = RequestBody.create(JSON, msg);
                Request.Builder builder = new Request.Builder()
                        .url("http://" + config.getSettings().httpServer + ":" + config.getSettings().httpPort + "/rec")
                        .post(body);
                addHmacIfNeeded(msg, builder);
                Request request = builder.build();
                try(Response response = client.execute(request)) {
                    String json = response.body().string();
                    if(response.isSuccessful()) {
                        JSONObject resp = new JSONObject(json);
                        spaceTotal = resp.getLong("spaceTotal");
                        spaceFree = resp.getLong("spaceFree");
                    } else {
                        LOG.error("Couldn't synchronize with server. HTTP status: {} - {}", response.code(), json);
                    }
                }
            } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | IllegalStateException e) {
                LOG.error("Couldn't synchronize with server", e);
            }
        }

        private void syncModels() {
            try {
                String msg = "{\"action\": \"list\"}";
                RequestBody body = RequestBody.create(JSON, msg);
                Request.Builder builder = new Request.Builder()
                        .url("http://" + config.getSettings().httpServer + ":" + config.getSettings().httpPort + "/rec")
                        .post(body);
                addHmacIfNeeded(msg, builder);
                Request request = builder.build();
                try(Response response = client.execute(request)) {
                    String json = response.body().string();
                    if(response.isSuccessful()) {
                        ModelListResponse resp = modelListResponseAdapter.fromJson(json);
                        if(resp.status.equals("success")) {
                            models = resp.models;
                            for (Model model : models) {
                                for (Site site : sites) {
                                    if(site.isSiteForModel(model)) {
                                        model.setSite(site);
                                    }
                                }
                            }
                            lastSync = Instant.now();
                        } else {
                            LOG.error("Server returned error: {} - {}", resp.status, resp.msg);
                        }
                    } else {
                        LOG.error("Couldn't synchronize with server. HTTP status: {} - {}", response.code(), json);
                    }
                }
            } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | IllegalStateException e) {
                LOG.error("Couldn't synchronize with server", e);
            }
        }

        private void syncOnlineModels() {
            try {
                String msg = "{\"action\": \"listOnline\"}";
                RequestBody body = RequestBody.create(JSON, msg);
                Request.Builder builder = new Request.Builder()
                        .url("http://" + config.getSettings().httpServer + ":" + config.getSettings().httpPort + "/rec")
                        .post(body);
                addHmacIfNeeded(msg, builder);
                Request request = builder.build();
                try (Response response = client.execute(request)) {
                    String json = response.body().string();
                    if (response.isSuccessful()) {
                        ModelListResponse resp = modelListResponseAdapter.fromJson(json);
                        if (resp.status.equals("success")) {
                            onlineModels = resp.models;
                            for (Model model : models) {
                                for (Site site : sites) {
                                    if (site.isSiteForModel(model)) {
                                        model.setSite(site);
                                    }
                                }
                            }
                        } else {
                            LOG.error("Server returned error: {} - {}", resp.status, resp.msg);
                        }
                    } else {
                        LOG.error("Couldn't synchronize with server. HTTP status: {} - {}", response.code(), json);
                    }
                }
            } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | IllegalStateException e) {
                LOG.error("Couldn't synchronize with server", e);
            }
        }

        private void syncRecordings() {
            try {
                String msg = "{\"action\": \"recordings\"}";
                RequestBody body = RequestBody.create(JSON, msg);
                Request.Builder builder = new Request.Builder()
                        .url("http://" + config.getSettings().httpServer + ":" + config.getSettings().httpPort + "/rec")
                        .post(body);
                addHmacIfNeeded(msg, builder);
                Request request = builder.build();
                try (Response response = client.execute(request)) {
                    String json = response.body().string();
                    if (response.isSuccessful()) {
                        RecordingListResponse resp = recordingListResponseAdapter.fromJson(json);
                        if (resp.status.equals("success")) {
                            List<Recording> newRecordings = resp.recordings;
                            // fire changed events
                            for (Iterator<Recording> iterator = recordings.iterator(); iterator.hasNext();) {
                                Recording recording = iterator.next();
                                if(newRecordings.contains(recording)) {
                                    int idx = newRecordings.indexOf(recording);
                                    Recording newRecording = newRecordings.get(idx);
                                    if(newRecording.getStatus() != recording.getStatus()) {
                                        File file = new File(recording.getPath());
                                        Model m = new UnknownModel();
                                        m.setName(newRecording.getModelName());
                                        RecordingStateChangedEvent evt = new RecordingStateChangedEvent(file, newRecording.getStatus(), m, recording.getStartDate());
                                        EventBusHolder.BUS.post(evt);
                                    }
                                }
                            }
                            recordings = newRecordings;
                        } else {
                            LOG.error("Server returned error: {} - {}", resp.status, resp.msg);
                        }
                    } else {
                        LOG.error("Couldn't synchronize with server. HTTP status: {} - {}", response.code(), json);
                    }
                }
            } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | IllegalStateException e) {
                LOG.error("Couldn't synchronize with server", e);
            }
        }

        private void sleep() {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                // interrupted, probably by stopThread
            }
        }

        public void stopThread() {
            running = false;
            interrupt();
        }
    }

    private static class ModelListResponse {
        public String status;
        public String msg;
        public List<Model> models;
    }

    private static class RecordingListResponse {
        public String status;
        public String msg;
        public List<Recording> recordings;
    }

    @Override
    public List<Recording> getRecordings() throws IOException, InvalidKeyException, NoSuchAlgorithmException, IllegalStateException {
        return recordings;
    }

    @Override
    public void delete(Recording recording) throws IOException, InvalidKeyException, NoSuchAlgorithmException, IllegalStateException {
        String msg = "{\"action\": \"delete\", \"recording\": \""+recording.getPath()+"\"}";
        RequestBody body = RequestBody.create(JSON, msg);
        Request.Builder builder = new Request.Builder()
                .url("http://" + config.getSettings().httpServer + ":" + config.getSettings().httpPort + "/rec")
                .post(body);
        addHmacIfNeeded(msg, builder);
        Request request = builder.build();
        try(Response response = client.execute(request)) {
            String json = response.body().string();
            RecordingListResponse resp = recordingListResponseAdapter.fromJson(json);
            if(response.isSuccessful()) {
                if(!resp.status.equals("success")) {
                    throw new IOException("Couldn't delete recording: " + resp.msg);
                } else {
                    recordings.remove(recording);
                }
            } else {
                throw new IOException("Couldn't delete recording: " + resp.msg);
            }
        }
    }

    public static class ModelRequest {
        private String action;
        private Model model;

        public ModelRequest(String action, Model model) {
            super();
            this.action = action;
            this.model = model;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public Model getModel() {
            return model;
        }

        public void setModel(Model model) {
            this.model = model;
        }
    }

    @Override
    public void switchStreamSource(Model model) throws IOException, InvalidKeyException, NoSuchAlgorithmException, IllegalStateException {
        sendRequest("switch", model);
    }

    @Override
    public void suspendRecording(Model model) throws InvalidKeyException, NoSuchAlgorithmException, IllegalStateException, IOException {
        sendRequest("suspend", model);
        model.setSuspended(true);
        // update cached model
        int index = models.indexOf(model);
        if(index >= 0) {
            Model m = models.get(index);
            m.setSuspended(true);
        }
    }

    @Override
    public void resumeRecording(Model model) throws IOException, InvalidKeyException, NoSuchAlgorithmException, IllegalStateException {
        sendRequest("resume", model);
        model.setSuspended(false);
        // update cached model
        int index = models.indexOf(model);
        if(index >= 0) {
            Model m = models.get(index);
            m.setSuspended(false);
        }
    }

    @Override
    public List<Model> getOnlineModels() {
        return onlineModels;
    }

    @Override
    public HttpClient getHttpClient() {
        return client;
    }

    @Override
    public long getTotalSpaceBytes() throws IOException {
        return spaceTotal;
    }

    @Override
    public long getFreeSpaceBytes() {
        return spaceFree;
    }

    private static class UnknownModel extends AbstractModel {
        @Override
        public boolean isOnline(boolean ignoreCache) throws IOException, ExecutionException, InterruptedException {
            return false;
        }

        @Override
        public List<StreamSource> getStreamSources() throws IOException, ExecutionException, ParseException, PlaylistException {
            return Collections.emptyList();
        }

        @Override
        public void invalidateCacheEntries() {
        }

        @Override
        public void receiveTip(int tokens) throws IOException {
        }

        @Override
        public int[] getStreamResolution(boolean failFast) throws ExecutionException {
            return new int[2];
        }

        @Override
        public boolean follow() throws IOException {
            return false;
        }

        @Override
        public boolean unfollow() throws IOException {
            return false;
        }

        @Override
        public Site getSite() {
            return new Site() {

                @Override
                public boolean supportsTips() {
                    return false;
                }

                @Override
                public boolean supportsSearch() {
                    return false;
                }

                @Override
                public boolean supportsFollow() {
                    return false;
                }

                @Override
                public void shutdown() {
                }

                @Override
                public void setRecorder(Recorder recorder) {
                }

                @Override
                public void setEnabled(boolean enabled) {
                }

                @Override
                public boolean searchRequiresLogin() {
                    return false;
                }

                @Override
                public List<Model> search(String q) throws IOException, InterruptedException {
                    return Collections.emptyList();
                }

                @Override
                public boolean login() throws IOException {
                    return false;
                }

                @Override
                public boolean isSiteForModel(Model m) {
                    return false;
                }

                @Override
                public boolean isEnabled() {
                    return false;
                }

                @Override
                public void init() throws IOException {
                }

                @Override
                public Integer getTokenBalance() throws IOException {
                    return 0;
                }

                @Override
                public Recorder getRecorder() {
                    return null;
                }

                @Override
                public String getName() {
                    return "unknown";
                }

                @Override
                public HttpClient getHttpClient() {
                    return null;
                }

                @Override
                public String getBuyTokensLink() {
                    return "";
                }

                @Override
                public String getBaseUrl() {
                    return "";
                }

                @Override
                public String getAffiliateLink() {
                    return "";
                }

                @Override
                public boolean credentialsAvailable() {
                    return false;
                }

                @Override
                public Model createModelFromUrl(String url) {
                    return null;
                }

                @Override
                public Model createModel(String name) {
                    return null;
                }
            };
        }
    }
}
