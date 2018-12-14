package ctbrec.ui.sites.streamate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.io.HttpException;
import ctbrec.sites.streamate.Streamate;
import ctbrec.sites.streamate.StreamateHttpClient;
import ctbrec.sites.streamate.StreamateModel;
import ctbrec.ui.PaginatedScheduledService;
import javafx.concurrent.Task;
import okhttp3.Request;
import okhttp3.Response;

public class StreamateFollowedService extends PaginatedScheduledService {

    private static final transient Logger LOG = LoggerFactory.getLogger(StreamateFollowedService.class);

    private static final int MODELS_PER_PAGE = 48;
    private Streamate streamate;
    private StreamateHttpClient httpClient;
    private String url;
    private boolean showOnline = true;

    public StreamateFollowedService(Streamate streamate) {
        this.streamate = streamate;
        this.httpClient = (StreamateHttpClient) streamate.getHttpClient();
        this.url = streamate.getBaseUrl() + "/api/search/v1/favorites?host=streamate.com&domain=streamate.com";
    }

    @Override
    protected Task<List<Model>> createTask() {
        return new Task<List<Model>>() {
            @Override
            public List<Model> call() throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
                httpClient.login();
                String saKey = httpClient.getSaKey();
                Long userId = httpClient.getUserId();
                String _url = url + "&page_number=" + page + "&results_per_page=" + MODELS_PER_PAGE + "&sakey=" + saKey + "&userid=" + userId;
                LOG.debug("Fetching page {}", _url);
                Request request = new Request.Builder()
                        .url(_url)
                        .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                        .addHeader("Accept", "application/json, */*")
                        .addHeader("Accept-Language", "en")
                        .addHeader("Referer", streamate.getBaseUrl())
                        .build();
                try(Response response = streamate.getHttpClient().execute(request)) {
                    if (response.isSuccessful()) {
                        List<Model> models = new ArrayList<>();
                        String content = response.body().string();
                        JSONObject json = new JSONObject(content);
                        if(json.optString("status").equals("SM_OK")) {
                            JSONArray performers = json.getJSONArray("Results");
                            for (int i = 0; i < performers.length(); i++) {
                                JSONObject p = performers.getJSONObject(i);
                                String nickname = p.getString("Nickname");
                                StreamateModel model = (StreamateModel) streamate.createModel(nickname);
                                model.setId(p.getLong("PerformerId"));
                                model.setPreview("https://m1.nsimg.net/biopic/320x240/" + model.getId());
                                boolean online = p.optString("LiveStatus").equals("live");
                                model.setOnline(online);
                                if(online == showOnline) {
                                    models.add(model);
                                }
                            }
                        } else {
                            throw new IOException("Status: " + json.optString("status"));
                        }
                        return models;
                    } else {
                        throw new HttpException(response.code(), response.message());
                    }
                }
            }
        };
    }

    public void setOnline(boolean online) {
        this.showOnline = online;
    }
}
