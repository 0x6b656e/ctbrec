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
import ctbrec.sites.streamate.StreamateModel;
import ctbrec.ui.PaginatedScheduledService;
import javafx.concurrent.Task;
import okhttp3.Request;
import okhttp3.Response;

public class StreamateUpdateService extends PaginatedScheduledService {

    private static final transient Logger LOG = LoggerFactory.getLogger(StreamateUpdateService.class);

    private static final int MODELS_PER_PAGE = 48;
    private Streamate streamate;
    private String url;

    public StreamateUpdateService(Streamate streamate, String url) {
        this.streamate = streamate;
        this.url = url;
    }

    @Override
    protected Task<List<Model>> createTask() {
        return new Task<List<Model>>() {
            @Override
            public List<Model> call() throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
                int from = (page - 1) * MODELS_PER_PAGE;
                String _url = url + "&from=" + from + "&size=" + MODELS_PER_PAGE;
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
                        JSONArray performers = json.getJSONArray("performers");
                        for (int i = 0; i < performers.length(); i++) {
                            JSONObject p = performers.getJSONObject(i);
                            String nickname = p.getString("nickname");
                            StreamateModel model = (StreamateModel) streamate.createModel(nickname);
                            model.setId(Long.toString(p.getLong("id")));
                            model.setPreview(p.getString("thumbnail"));
                            model.setOnline(p.optBoolean("online"));
                            // TODO figure out, what all the states mean
                            //                         liveState   {…}
                            //                            exclusiveShow     false
                            //                            goldShow          true
                            //                            onBreak           false
                            //                            partyChat         true
                            //                            preGoldShow       true
                            //                            privateChat       false
                            //                            specialShow       false
                            models.add(model);
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
