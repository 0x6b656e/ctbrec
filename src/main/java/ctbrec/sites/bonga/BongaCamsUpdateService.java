package ctbrec.sites.bonga;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.ui.PaginatedScheduledService;
import javafx.concurrent.Task;
import okhttp3.Request;
import okhttp3.Response;

public class BongaCamsUpdateService extends PaginatedScheduledService {

    private static final transient Logger LOG = LoggerFactory.getLogger(BongaCamsUpdateService.class);

    private BongaCams bongaCams;
    private String url;

    public BongaCamsUpdateService(BongaCams bongaCams, String url) {
        this.bongaCams = bongaCams;
        this.url = url;
    }

    @Override
    protected Task<List<Model>> createTask() {
        return new Task<List<Model>>() {
            @Override
            public List<Model> call() throws IOException {
                String _url = url + ((page-1) * 36);
                LOG.debug("Fetching page {}", _url);
                Request request = new Request.Builder()
                        .url(_url)
                        .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                        .addHeader("Accept", "application/json, text/javascript, */*")
                        .addHeader("Accept-Language", "en")
                        .addHeader("Referer", bongaCams.getBaseUrl())
                        .addHeader("X-Requested-With", "XMLHttpRequest")
                        .build();
                Response response = bongaCams.getHttpClient().execute(request);
                if (response.isSuccessful()) {
                    String content = response.body().string();
                    List<Model> models = new ArrayList<>();
                    JSONObject json = new JSONObject(content);
                    if(json.optString("status").equals("success")) {
                        JSONArray _models = json.getJSONArray("models");
                        for (int i = 0; i < _models.length(); i++) {
                            JSONObject m = _models.getJSONObject(i);
                            String name = m.getString("username");
                            BongaCamsModel model = (BongaCamsModel) bongaCams.createModel(name);
                            model.setUserId(m.getInt("user_id"));
                            boolean away = m.optBoolean("is_away");
                            boolean online = m.optBoolean("online") && !away;
                            model.setOnline(online);
                            if(online) {
                                if(away) {
                                    model.setOnlineState("away");
                                } else {
                                    model.setOnlineState(m.getString("room"));
                                }
                            } else {
                                model.setOnlineState("offline");
                            }
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
