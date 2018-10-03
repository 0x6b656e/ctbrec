package ctbrec.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iheartradio.m3u8.data.MasterPlaylist;
import com.iheartradio.m3u8.data.PlaylistData;

import ctbrec.HttpClient;
import ctbrec.Model;
import ctbrec.recorder.StreamInfo;
import ctbrec.recorder.download.StreamSource;
import javafx.concurrent.Task;
import javafx.scene.control.ChoiceDialog;

public class StreamSourceSelectionDialog {
    private static final transient Logger LOG = LoggerFactory.getLogger(StreamSourceSelectionDialog.class);

    public static void show(Model model, HttpClient client, Function<Model,Void> onSuccess, Function<Throwable, Void> onFail) {
        Task<List<StreamSource>> selectStreamSource = new Task<List<StreamSource>>() {
            @Override
            protected List<StreamSource> call() throws Exception {
                StreamInfo streamInfo = model.getStreamInfo();
                MasterPlaylist masterPlaylist = model.getMasterPlaylist();
                List<StreamSource> sources = new ArrayList<>();
                for (PlaylistData playlist : masterPlaylist.getPlaylists()) {
                    if (playlist.hasStreamInfo()) {
                        StreamSource src = new StreamSource();
                        src.bandwidth = playlist.getStreamInfo().getBandwidth();
                        src.height = playlist.getStreamInfo().getResolution().height;
                        String masterUrl = streamInfo.url;
                        String baseUrl = masterUrl.substring(0, masterUrl.lastIndexOf('/') + 1);
                        String segmentUri = baseUrl + playlist.getUri();
                        src.mediaPlaylistUrl = segmentUri;
                        LOG.trace("Media playlist {}", src.mediaPlaylistUrl);
                        sources.add(src);
                    }
                }
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
