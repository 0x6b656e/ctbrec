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

public class LiveJasminFollowedUpdateService extends PaginatedScheduledService {

    private static final transient Logger LOG = LoggerFactory.getLogger(LiveJasminFollowedUpdateService.class);
    private LiveJasmin liveJasmin;
    private String url;
    private boolean showOnline = true;

    public LiveJasminFollowedUpdateService(LiveJasmin liveJasmin) {
        this.liveJasmin = liveJasmin;
        long ts = System.currentTimeMillis();
        this.url = liveJasmin.getBaseUrl() + "/en/free/favourite/get-favourite-list?_dc=" + ts;
    }

    @Override
    protected Task<List<Model>> createTask() {
        return new Task<List<Model>>() {
            @Override
            public List<Model> call() throws IOException {
                //String _url = url + ((page-1) * 36); // TODO find out how to switch pages
                //LOG.debug("Fetching page {}", url);
                Request request = new Request.Builder()
                        .url(url)
                        .header("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                        .header("Accept", "*/*")
                        .header("Accept-Language", "en")
                        .header("Referer", liveJasmin.getBaseUrl() + "/en/free/favorite")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .build();
                try (Response response = liveJasmin.getHttpClient().execute(request)) {
                    if (response.isSuccessful()) {
                        String body = response.body().string();
                        List<Model> models = new ArrayList<>();
                        JSONObject json = new JSONObject(body);
                        LOG.debug(json.toString(2));
                        if(json.has("success")) {
                            JSONObject data = json.getJSONObject("data");
                            JSONArray performers = data.getJSONArray("performers");
                            for (int i = 0; i < performers.length(); i++) {
                                JSONObject m = performers.getJSONObject(i);
                                String name = m.optString("pid");
                                if(name.isEmpty()) {
                                    continue;
                                }
                                LiveJasminModel model = (LiveJasminModel) liveJasmin.createModel(name);
                                model.setId(m.getString("id"));
                                model.setPreview(m.getString("profilePictureUrl"));
                                Model.State onlineState = LiveJasminModel.mapStatus(m.getInt("status"));
                                boolean online = onlineState == Model.State.ONLINE;
                                model.setOnlineState(onlineState);
                                if(online == showOnline) {
                                    models.add(model);
                                }
                            }
                        } else {
                            LOG.error("Request failed:\n{}", body);
                            throw new IOException("Response was not successful");
                        }
                        return models;
                    } else {
                        throw new HttpException(response.code(), response.message());
                    }
                }
            }
        };
    }

    public void setShowOnline(boolean showOnline) {
        this.showOnline = showOnline;
    }
}
