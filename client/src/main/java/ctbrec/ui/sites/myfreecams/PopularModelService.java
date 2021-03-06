package ctbrec.ui.sites.myfreecams;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import ctbrec.Model;
import ctbrec.sites.mfc.MyFreeCamsClient;
import ctbrec.ui.PaginatedScheduledService;
import javafx.concurrent.Task;

public class PopularModelService extends PaginatedScheduledService {

    @Override
    protected Task<List<Model>> createTask() {
        return new Task<List<Model>>() {
            @Override
            public List<Model> call() throws IOException {
                MyFreeCamsClient client = MyFreeCamsClient.getInstance();
                int modelsPerPage = 50;
                return client.getModels().stream()
                        .filter(m -> m.getPreview() != null)
                        .filter(m -> m.getStreamUrl() != null)
                        .filter(m -> {
                            try {
                                return m.isOnline();
                            } catch (Exception e) {
                                return false;
                            }
                        })
                        .sorted((m1, m2) -> m2.getViewerCount() - m1.getViewerCount())
                        .skip( (page-1) * modelsPerPage)
                        .limit(modelsPerPage)
                        .collect(Collectors.toList());
            }
        };
    }
}
