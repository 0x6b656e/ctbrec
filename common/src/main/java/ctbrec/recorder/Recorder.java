package ctbrec.recorder;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import ctbrec.Model;
import ctbrec.Recording;
import ctbrec.io.HttpClient;

public interface Recorder {
    public void startRecording(Model model) throws IOException, InvalidKeyException, NoSuchAlgorithmException, IllegalStateException;

    public void stopRecording(Model model) throws IOException, InvalidKeyException, NoSuchAlgorithmException, IllegalStateException;

    public void switchStreamSource(Model model) throws IOException, InvalidKeyException, NoSuchAlgorithmException, IllegalStateException;

    /**
     * Returns, if a model is in the list of models to record. This does not reflect, if there currently is a recording running. The model might be offline
     * aswell.
     */
    public boolean isRecording(Model model);

    public List<Model> getModelsRecording();

    public List<Recording> getRecordings() throws IOException, InvalidKeyException, NoSuchAlgorithmException, IllegalStateException;

    public void delete(Recording recording) throws IOException, InvalidKeyException, NoSuchAlgorithmException, IllegalStateException;

    public void shutdown();

    public void suspendRecording(Model model) throws IOException, InvalidKeyException, NoSuchAlgorithmException, IllegalStateException;
    public void resumeRecording(Model model) throws IOException, InvalidKeyException, NoSuchAlgorithmException, IllegalStateException;

    public boolean isSuspended(Model model);

    /**
     * Returns only the models from getModelsRecording(), which are online
     * @return
     */
    public List<Model> getOnlineModels();

    public HttpClient getHttpClient();

    /**
     * Get the total size of the filesystem we are recording to
     * @return the total size in bytes
     * @throws IOException
     */
    public long getTotalSpaceBytes() throws IOException;

    /**
     * Get the free space left on the filesystem we are recording to
     * @return the free space in bytes
     * @throws IOException
     */
    public long getFreeSpaceBytes() throws IOException;
}
