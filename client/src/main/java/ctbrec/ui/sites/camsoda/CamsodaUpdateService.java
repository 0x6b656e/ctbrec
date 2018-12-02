package ctbrec.ui.sites.camsoda;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Model;
import ctbrec.StringUtil;
import ctbrec.io.HttpException;
import ctbrec.sites.camsoda.Camsoda;
import ctbrec.sites.camsoda.CamsodaModel;
import ctbrec.ui.PaginatedScheduledService;
import ctbrec.ui.SiteUiFactory;
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
                List<CamsodaModel> models = new ArrayList<>();
                if(loginRequired && StringUtil.isBlank(ctbrec.Config.getInstance().getSettings().username)) {
                    return Collections.emptyList();
                } else {
                    String url = CamsodaUpdateService.this.url;
                    LOG.debug("Fetching page {}", url);
                    if(loginRequired) {
                        SiteUiFactory.getUi(camsoda).login();
                    }
                    Request request = new Request.Builder().url(url).build();
                    try(Response response = camsoda.getHttpClient().execute(request)) {
                        if (response.isSuccessful()) {
                            JSONObject json = new JSONObject(response.body().string());
                            if(json.has("status") && json.getBoolean("status")) {
                                JSONArray results = json.getJSONArray("results");
                                for (int i = 0; i < results.length(); i++) {
                                    JSONObject result = results.getJSONObject(i);
                                    if(result.has("tpl")) {
                                        JSONArray tpl = result.getJSONArray("tpl");
                                        String name = tpl.getString(0);
                                        String displayName = tpl.getString(1);
                                        // int connections = tpl.getInt(2);
                                        String streamName = tpl.getString(5);
                                        String tsize = tpl.getString(6);
                                        String serverPrefix = tpl.getString(7);
                                        CamsodaModel model = (CamsodaModel) camsoda.createModel(name);
                                        model.setDescription(tpl.getString(4));
                                        model.setSortOrder(tpl.getFloat(3));
                                        long unixtime = System.currentTimeMillis() / 1000;
                                        String preview = "https://thumbs-orig.camsoda.com/thumbs/"
                                                + streamName + '/' + serverPrefix + '/' + tsize + '/' + unixtime + '/' + name + ".jpg?cb=" + unixtime;
                                        model.setPreview(preview);
                                        if(result.has("edge_servers")) {
                                            JSONArray edgeServers = result.getJSONArray("edge_servers");
                                            model.setStreamUrl("https://" + edgeServers.getString(0) + "/cam/mp4:" + streamName + "_h264_aac_480p/playlist.m3u8");
                                        }
                                        model.setDisplayName(displayName);
                                        models.add(model);
                                    } else {
                                        String name = result.getString("username");
                                        CamsodaModel model = (CamsodaModel) camsoda.createModel(name);
                                        if(result.has("server_prefix")) {
                                            String serverPrefix = result.getString("server_prefix");
                                            String streamName = result.getString("stream_name");
                                            model.setSortOrder(result.getFloat("sort_value"));
                                            models.add(model);
                                            if(result.has("status")) {
                                                model.setOnlineState(result.getString("status"));
                                            }

                                            if(result.has("display_name")) {
                                                model.setDisplayName(result.getString("display_name"));
                                            }

                                            if(result.has("edge_servers")) {
                                                JSONArray edgeServers = result.getJSONArray("edge_servers");
                                                model.setStreamUrl("https://" + edgeServers.getString(0) + "/cam/mp4:" + streamName + "_h264_aac_480p/playlist.m3u8");
                                            }

                                            if(result.has("tsize")) {
                                                long unixtime = System.currentTimeMillis() / 1000;
                                                String tsize = result.getString("tsize");
                                                String preview = "https://thumbs-orig.camsoda.com/thumbs/"
                                                        + streamName + '/' + serverPrefix + '/' + tsize + '/' + unixtime + '/' + name + ".jpg?cb=" + unixtime;
                                                model.setPreview(preview);
                                            }
                                        }
                                    }
                                }
                                return models.stream()
                                        .sorted((m1,m2) -> (int)(m2.getSortOrder() - m1.getSortOrder()))
                                        .skip( (page-1) * modelsPerPage)
                                        .limit(modelsPerPage)
                                        .collect(Collectors.toList());
                            } else {
                                return Collections.emptyList();
                            }
                        } else {
                            throw new HttpException(response.code(), response.message());
                        }
                    }
                }
            }
        };
    }

}
