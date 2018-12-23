package ctbrec.ui.action;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import ctbrec.Model;
import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.Node;

public class ModelMassEditAction {

    static BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    static ExecutorService threadPool = new ThreadPoolExecutor(2, 2, 10, TimeUnit.MINUTES, queue);

    protected List<? extends Model> models;
    protected Consumer<Model> action;
    protected Node source;

    protected ModelMassEditAction(Node source, List<? extends Model> models) {
        this.source = source;
        this.models = models;
    }

    public ModelMassEditAction(Node source, List<? extends Model> models, Consumer<Model> action) {
        this.source = source;
        this.models = models;
        this.action = action;
    }

    public void execute() {
        execute((m) -> {});
    }

    public void execute(Consumer<Model> callback) {
        Consumer<Model> cb = Objects.requireNonNull(callback);
        source.setCursor(Cursor.WAIT);
        threadPool.submit(() -> {
            for (Model model : models) {
                action.accept(model);
                cb.accept(model);
            }
            Platform.runLater(() -> source.setCursor(Cursor.DEFAULT));
        });
    }
}
