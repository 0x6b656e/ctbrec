package ctbrec.recorder;

import static ctbrec.Model.State.*;

import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.event.EventBusHolder;
import ctbrec.event.ModelIsOnlineEvent;
import ctbrec.event.ModelStateChangedEvent;
import ctbrec.io.HttpException;

public class OnlineMonitor extends Thread {
    private static final transient Logger LOG = LoggerFactory.getLogger(OnlineMonitor.class);
    private static final boolean IGNORE_CACHE = true;

    private volatile boolean running = false;
    private Recorder recorder;

    private Map<Model, Model.State> states = new HashMap<>();

    public OnlineMonitor(Recorder recorder) {
        this.recorder = recorder;
        setName("OnlineMonitor");
        setDaemon(true);
    }

    @Override
    public void run() {
        running = true;
        while (running) {
            Instant begin = Instant.now();
            List<Model> models = recorder.getModelsRecording();

            // remove models, which are not recorded anymore
            for (Iterator<Model> iterator = states.keySet().iterator(); iterator.hasNext();) {
                Model model = iterator.next();
                if(!models.contains(model)) {
                    iterator.remove();
                }
            }

            // update the currently recorded models
            for (Model model : models) {
                try {
                    if(model.isOnline(IGNORE_CACHE)) {
                        EventBusHolder.BUS.post(new ModelIsOnlineEvent(model));
                    }
                    Model.State state = model.getOnlineState(false);
                    Model.State oldState = states.getOrDefault(model, UNKNOWN);
                    states.put(model, state);
                    if(state != oldState) {
                        EventBusHolder.BUS.post(new ModelStateChangedEvent(model, oldState, state));
                    }
                } catch (HttpException e) {
                    LOG.error("Couldn't check if model {} is online. HTTP Response: {} - {}",
                            model.getName(), e.getResponseCode(), e.getResponseMessage());
                } catch (SocketTimeoutException e) {
                    LOG.error("Couldn't check if model {} is online. Request timed out", model.getName());
                } catch (InterruptedException | InterruptedIOException e) {
                    if(running) {
                        LOG.error("Couldn't check if model {} is online", model.getName(), e);
                    }
                } catch (Exception e) {
                    LOG.error("Couldn't check if model {} is online", model.getName(), e);
                }
            }
            Instant end = Instant.now();
            Duration timeCheckTook = Duration.between(begin, end);
            LOG.trace("Online check for {} models took {} seconds", models.size(), timeCheckTook.getSeconds());

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

    public void shutdown() {
        running = false;
        interrupt();
    }
}