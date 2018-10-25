package ctbrec.sites.camsoda;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jetty.util.StringUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Model;
import ctbrec.ui.PaginatedScheduledService;
import javafx.concurrent.Task;
import okhttp3.Request;
import okhttp3.Response;

public class CamsodaUpdateService extends PaginatedScheduledService {

    private static final transient Logger LOG = LoggerFactory.getLogger(CamsodaUpdateService.class);

    private String url;
    private boolean loginRequired;
    private Camsoda camsoda;
    int modelsPerPage = 50;

    public CamsodaUpdateService(String url, boolean loginRequired, Camsoda camsoda) {
        this.url = url;
        this.loginRequired = loginRequired;
        this.camsoda = camsoda;
    }

    @Override
    protected Task<List<Model>> createTask() {
        return new Task<List<Model>>() {
            @Override
            public List<Model> call() throws IOException {
                List<Model> models = new ArrayList<>();
                if(loginRequired && StringUtil.isBlank(ctbrec.Config.getInstance().getSettings().username)) {
                    return models;
                } else {
                    String url = CamsodaUpdateService.this.url;
                    LOG.debug("Fetching page {}", url);
                    Request request = new Request.Builder().url(url).build();
                    Response response = camsoda.getHttpClient().execute(request, loginRequired);
                    if (response.isSuccessful()) {
                        JSONObject json = new JSONObject(response.body().string());
                        if(json.has("status") && json.getBoolean("status")) {
                            JSONArray results = json.getJSONArray("results");
                            for (int i = 0; i < results.length(); i++) {
                                JSONObject result = results.getJSONObject(i);
                                if(result.has("tpl")) {
                                    JSONArray tpl = result.getJSONArray("tpl");
                                    String name = tpl.getString(0);
                                    //                                    int connections = tpl.getInt(2);
                                    //                                    float sortValue = tpl.getFloat(3);
                                    String streamName = tpl.getString(5);
                                    String tsize = tpl.getString(6);
                                    String serverPrefix = tpl.getString(7);
                                    JSONArray edgeServers = result.getJSONArray("edge_servers");
                                    CamsodaModel model = (CamsodaModel) camsoda.createModel(name);
                                    model.setDescription(tpl.getString(4));
                                    model.setStreamUrl("https://" + edgeServers.getString(0) + "/cam/mp4:" + streamName + "_h264_aac_480p/playlist.m3u8");
                                    long unixtime = System.currentTimeMillis() / 1000;
                                    String preview = "https://thumbs-orig.camsoda.com/thumbs/"
                                            + streamName + '/' + serverPrefix + '/' + tsize + '/' + unixtime + '/' + name + ".jpg?cb=" + unixtime;
                                    model.setPreview(preview);
                                    //LOG.debug(model.getPreview());
                                    models.add(model);
                                    // https://vide16-ord.camsoda.com/cam/mp4:kipsyrose-enc6-ord_h264_aac_480p/playlist.m3u8
                                    // https://enc42-ord.camsoda.com/cam/mp4:elizasmile-enc42-ord_h264_aac_480p/playlist.m3u8
                                    // https://thumbs-orig.camsoda.com/thumbs/marriednaughtycol-enc35-ord/enc35-ord/340x255/51349794/marriednaughtycol.jpg?cb=51349794
                                } else {
                                    //LOG.debug("HÖ? {}", result.toString(2));
                                }
                            }
                            return models.stream()
                                    .skip( (page-1) * modelsPerPage)
                                    .limit(modelsPerPage)
                                    .collect(Collectors.toList());
                        } else {
                            response.close();
                            return models;
                        }
                    } else {
                        int code = response.code();
                        response.close();
                        throw new IOException("HTTP status " + code);
                    }
                }
            }
        };
    }

}
