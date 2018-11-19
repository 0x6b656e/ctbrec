package ctbrec.ui.sites.fc2live;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.sites.fc2live.Fc2Live;
import ctbrec.sites.fc2live.Fc2Model;
import ctbrec.ui.PaginatedScheduledService;
import javafx.concurrent.Task;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Fc2UpdateService extends PaginatedScheduledService {
    private static final transient Logger LOG = LoggerFactory.getLogger(Fc2UpdateService.class);

    private String url;
    private Fc2Live fc2live;
    private int modelsPerPage = 30;

    public Fc2UpdateService(String url, Fc2Live fc2live) {
        this.url = url;
        this.fc2live = fc2live;
    }

    @Override
    protected Task<List<Model>> createTask() {
        return new Task<List<Model>>() {
            @Override
            public List<Model> call() throws IOException {
                RequestBody body = RequestBody.create(null, new byte[0]);
                Request req = new Request.Builder()
                        .url(url)
                        .method("POST", body)
                        .header("Accept", "*/*")
                        .header("Accept-Language", "en-US,en;q=0.5")
                        .header("Referer", Fc2Live.BASE_URL)
                        .header("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                        .header("X-Requested-With", "XMLHttpRequest")
                        .build();
                LOG.debug("Fetching page {}", url);
                try(Response resp = fc2live.getHttpClient().execute(req)) {
                    if(resp.isSuccessful()) {
                        List<Fc2Model> models = new ArrayList<>();
                        String msg = resp.body().string();
                        JSONObject json = new JSONObject(msg);
                        JSONArray channels = json.getJSONArray("channel");
                        for (int i = 0; i < channels.length(); i++) {
                            JSONObject channel = channels.getJSONObject(i);
                            Fc2Model model = (Fc2Model) fc2live.createModel(channel.getString("name"));
                            model.setId(channel.getString("id"));
                            model.setUrl(Fc2Live.BASE_URL + '/' + model.getId());
                            String previewUrl = channel.getString("image");
                            if(previewUrl == null || previewUrl.trim().isEmpty()) {
                                previewUrl = getClass().getResource("/image_not_found.png").toString();
                            }
                            model.setPreview(previewUrl);
                            model.setDescription(channel.optString("title"));
                            model.setViewerCount(channel.optInt("count"));
                            if(channel.getInt("login") == 0) {
                                models.add(model);
                            }
                        }
                        return models.stream()
                                .sorted((m1, m2) -> m2.getViewerCount() - m1.getViewerCount())
                                .skip( (page - 1) * modelsPerPage)
                                .limit(modelsPerPage)
                                .collect(Collectors.toList());
                    } else {
                        resp.close();
                        throw new IOException("HTTP status " + resp.code() + " " + resp.message());
                    }
                }
            }
        };
    }
}
