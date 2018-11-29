package ctbrec.ui.sites.nood;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.io.HtmlParser;
import ctbrec.sites.nood.Nood;
import ctbrec.ui.PaginatedScheduledService;
import javafx.concurrent.Task;
import okhttp3.Request;
import okhttp3.Response;

public class NoodUpdateService extends PaginatedScheduledService {

    private static final transient Logger LOG = LoggerFactory.getLogger(NoodUpdateService.class);

    private Nood nood;
    private String url;

    public NoodUpdateService(Nood nood, String url) {
        this.nood = nood;
        this.url = url;
    }

    @Override
    protected Task<List<Model>> createTask() {
        return new Task<List<Model>>() {
            @Override
            public List<Model> call() throws IOException {
                String _url = url;
                LOG.debug("Fetching page {}", _url);
                Request request = new Request.Builder()
                        .url(_url)
                        .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                        .addHeader("Accept", "application/json, text/javascript, */*")
                        .addHeader("Accept-Language", "en")
                        .addHeader("Referer", nood.getBaseUrl())
                        .addHeader("X-Requested-With", "XMLHttpRequest")
                        .build();
                Response response = nood.getHttpClient().execute(request);
                if (response.isSuccessful()) {
                    String content = response.body().string();
                    List<Model> models = new ArrayList<>();
                    Elements boxes = HtmlParser.getTags(content, "div[class~=content_box_stream_source]");
                    for (Element box : boxes) {
                        String boxHtml = box.html();
                        String href = box.attr("data-href");
                        String name = href.substring(href.lastIndexOf('/')+1);
                        Model model = nood.createModel(name);
                        model.setUrl(nood.getBaseUrl() + href);
                        parsePreview(model, boxHtml);
                        parseDescription(model, boxHtml);
                        if(model.getPreview() != null) {
                            models.add(model);
                        }
                    }

                    Model model = nood.createModel("5727");
                    model.setUrl(nood.getBaseUrl() + "/broadcasts/5727");
                    parsePreview(model, "https://www.nood.tv/snapshots/W1siZiIsImJyb2FkY2FzdC81NzI5L3NuYXAiXSxbInAiLCJ0aHVtYiIsIjY0MHgzNjAjbiJdXQ/4969f2a9aa1d420f");
                    parseDescription(model, "");
                    models.add(model);

                    return models;
                } else {
                    int code = response.code();
                    response.close();
                    throw new IOException("HTTP status " + code);
                }
            }

            private void parseDescription(Model model, String boxHtml) {
                try {
                    String desc = HtmlParser.getText(boxHtml, "div[class~=content-box-title] span");
                    model.setDescription(desc);
                } catch(Exception e) {
                    LOG.warn("Couldn't parse description: {}", e.getMessage());
                    model.setDescription("");
                }
            }

            private void parsePreview(Model model, String boxHtml) {
                try {
                    String style = HtmlParser.getTag(boxHtml, "div[class~=content-box-poster]").attr("style");
                    Matcher m = Pattern.compile("background:url\\((.*?)\\)").matcher(style);
                    if(m.find()) {
                        String path = m.group(1);
                        String url = nood.getBaseUrl() + path;
                        model.setPreview(url);
                    }
                } catch(Exception e) {
                    LOG.warn("Couldn't parse preview: {}", e.getMessage());
                }
            }
        };
    }

}
