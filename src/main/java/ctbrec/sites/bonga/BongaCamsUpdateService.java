package ctbrec.sites.bonga;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Model;
import ctbrec.ui.PaginatedScheduledService;
import javafx.concurrent.Task;
import okhttp3.Request;
import okhttp3.Response;

public class BongaCamsUpdateService extends PaginatedScheduledService {

    private static final transient Logger LOG = LoggerFactory.getLogger(BongaCamsUpdateService.class);

    private BongaCams bongaCams;

    public BongaCamsUpdateService(BongaCams bongaCams) {
        this.bongaCams = bongaCams;
    }

    @Override
    protected Task<List<Model>> createTask() {
        return new Task<List<Model>>() {
            @Override
            public List<Model> call() throws IOException {
                String url = BongaCams.BASE_URL + "/tools/listing_v3.php?livetab=female&online_only=true&is_mobile=true&offset=" + ((page-1) * 50);
                LOG.debug("Fetching page {}", url);
                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("User-Agent", "Mozilla/5.0 (Android 9.0; Mobile; rv:61.0) Gecko/61.0 Firefox/61.0")
                        .addHeader("Accept", "application/json, text/javascript, */*")
                        .addHeader("Accept-Language", "en")
                        .addHeader("Referer", bongaCams.getBaseUrl())
                        .addHeader("X-Requested-With", "XMLHttpRequest")
                        .build();
                Response response = bongaCams.getHttpClient().execute(request);
                if (response.isSuccessful()) {
                    String content = response.body().string();
                    response.close();
                    List<Model> models = new ArrayList<>();
                    JSONObject json = new JSONObject(content);
                    if(json.optString("status").equals("success")) {
                        JSONArray _models = json.getJSONArray("models");
                        for (int i = 0; i < _models.length(); i++) {
                            JSONObject m = _models.getJSONObject(i);
                            String name = m.getString("username");
                            BongaCamsModel model = (BongaCamsModel) bongaCams.createModel(name);
                            model.setUserId(m.getInt("user_id"));
                            model.setOnlineState(m.getString("room"));
                            model.setOnline(m.optBoolean("online") && !m.optBoolean("is_away"));
                            model.setPreview("https:" + m.getString("thumb_image"));
                            models.add(model);
                        }
                    }
                    return models;
                } else {
                    int code = response.code();
                    response.close();
                    throw new IOException("HTTP status " + code);
                }
            }
        };
    }
}
