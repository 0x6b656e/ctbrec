package ctbrec.ui;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import ctbrec.Model;
import ctbrec.recorder.download.StreamSource;
import javafx.concurrent.Task;
import javafx.scene.control.ChoiceDialog;

public class StreamSourceSelectionDialog {
    public static void show(Model model, Function<Model,Void> onSuccess, Function<Throwable, Void> onFail) {
        Task<List<StreamSource>> selectStreamSource = new Task<List<StreamSource>>() {
            @Override
            protected List<StreamSource> call() throws Exception {
                List<StreamSource> sources = model.getStreamSources();
                Collections.sort(sources);
                return sources;
            }
        };
        selectStreamSource.setOnSucceeded((e) -> {
            List<StreamSource> sources;
            try {
                sources = selectStreamSource.get();
                ChoiceDialog<StreamSource> choiceDialog = new ChoiceDialog<StreamSource>(sources.get(sources.size()-1), sources);
                choiceDialog.setTitle("Stream Quality");
                choiceDialog.setHeaderText("Select your preferred stream quality");
                choiceDialog.setResizable(true);
                Optional<StreamSource> selectedSource = choiceDialog.showAndWait();
                if(selectedSource.isPresent()) {
                    int index = sources.indexOf(selectedSource.get());
                    model.setStreamUrlIndex(index);
                    onSuccess.apply(model);
                }
            } catch (InterruptedException | ExecutionException e1) {
                onFail.apply(e1);
            }
        });
        selectStreamSource.setOnFailed((e) -> {
            onFail.apply(selectStreamSource.getException());
        });
        new Thread(selectStreamSource).start();
    }
}
