package ctbrec.ui.sites.jasmin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.io.HttpException;
import ctbrec.sites.jasmin.LiveJasmin;
import ctbrec.sites.jasmin.LiveJasminModel;
import ctbrec.ui.PaginatedScheduledService;
import javafx.concurrent.Task;
import okhttp3.Request;
import okhttp3.Response;

public class LiveJasminUpdateService extends PaginatedScheduledService {

    private static final transient Logger LOG = LoggerFactory.getLogger(LiveJasminUpdateService.class);
    private String url;
    private LiveJasmin liveJasmin;

    public LiveJasminUpdateService(LiveJasmin liveJasmin, String url) {
        this.liveJasmin = liveJasmin;
        this.url = url;
    }

    @Override
    protected Task<List<Model>> createTask() {
        return new Task<List<Model>>() {
            @Override
            public List<Model> call() throws IOException {
                String _url = url + ((page-1) * 36); // TODO find out how to switch pages
                LOG.debug("Fetching page {}", url);
                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                        .addHeader("Accept", "application/json, text/javascript, */*")
                        .addHeader("Accept-Language", "en")
                        .addHeader("Referer", liveJasmin.getBaseUrl())
                        .addHeader("X-Requested-With", "XMLHttpRequest")
                        .build();
                try (Response response = liveJasmin.getHttpClient().execute(request)) {
                    if (response.isSuccessful()) {
                        String body = response.body().string();
                        List<Model> models = new ArrayList<>();
                        JSONObject json = new JSONObject(body);
                        //LOG.debug(json.toString(2));
                        if(json.optBoolean("success")) {
                            JSONObject data = json.getJSONObject("data");
                            JSONObject content = data.getJSONObject("content");
                            JSONArray performers = content.getJSONArray("performers");
                            for (int i = 0; i < performers.length(); i++) {
                                JSONObject m = performers.getJSONObject(i);
                                String name = m.optString("pid");
                                if(name.isEmpty()) {
                                    continue;
                                }
                                LiveJasminModel model = (LiveJasminModel) liveJasmin.createModel(name);
                                model.setId(m.getString("id"));
                                model.setPreview(m.getString("profilePictureUrl"));
                                model.setOnline(true);
                                models.add(model);
                            }
                        } else {
                            LOG.error("Request failed:\n{}", body);
                            throw new IOException("Response was not successfull");
                        }
                        return models;
                    } else {
                        throw new HttpException(response.code(), response.message());
                    }
                }
            }
        };
    }
}
