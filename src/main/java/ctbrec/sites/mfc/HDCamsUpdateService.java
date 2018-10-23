package ctbrec.sites.mfc;


import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import ctbrec.Model;
import ctbrec.ui.PaginatedScheduledService;
import javafx.concurrent.Task;

public class HDCamsUpdateService extends PaginatedScheduledService {

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
                        .filter(m -> m.getStreamUrl().contains("/x-hls/"))
                        .filter(m -> {
                            try {
                                return m.isOnline();
                            } catch(Exception e) {
                                return false;
                            }
                        })
                        .sorted((m1,m2) -> (int)(m2.getCamScore() - m1.getCamScore()))
                        .skip( (page-1) * modelsPerPage)
                        .limit(modelsPerPage)
                        .collect(Collectors.toList());
            }
        };
    }

}
