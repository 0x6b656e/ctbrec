package ctbrec.sites.cam4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.eclipse.jetty.util.StringUtil;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.ui.HtmlParser;
import ctbrec.ui.PaginatedScheduledService;
import javafx.concurrent.Task;
import okhttp3.Request;
import okhttp3.Response;

public class Cam4UpdateService extends PaginatedScheduledService {

    private static final transient Logger LOG = LoggerFactory.getLogger(Cam4UpdateService.class);
    private String url;
    private Cam4 site;
    private boolean loginRequired;

    public Cam4UpdateService(String url, boolean loginRequired, Cam4 site) {
        this.site = site;
        this.url = url;
        this.loginRequired = loginRequired;

        ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("ThumbOverviewTab UpdateService");
                return t;
            }
        });
        setExecutor(executor);
    }

    @Override
    protected Task<List<Model>> createTask() {
        return new Task<List<Model>>() {
            @Override
            public List<Model> call() throws IOException {
                if(loginRequired && StringUtil.isBlank(Config.getInstance().getSettings().username)) { // FIXME change to cam4 username
                    return Collections.emptyList();
                } else {
                    String url = Cam4UpdateService.this.url + "&page=" + page;
                    LOG.debug("Fetching page {}", url);
                    Request request = new Request.Builder().url(url).build();
                    Response response = site.getHttpClient().execute(request, loginRequired);
                    if (response.isSuccessful()) {
                        JSONObject json = new JSONObject(response.body().string());
                        String html = json.getString("html");
                        Elements profilesBoxes = HtmlParser.getTags(html, "div[class~=profileDataBox]");
                        List<Model> models = new ArrayList<>(profilesBoxes.size());
                        for (Element profileBox : profilesBoxes) {
                            String boxHtml = profileBox.html();
                            Element profileLink = HtmlParser.getTag(boxHtml, "a.profile-preview");
                            String path = profileLink.attr("href");
                            String slug = path.substring(1);
                            Cam4Model model = (Cam4Model) site.createModel(slug);
                            String playlistUrl = profileLink.attr("data-hls-preview-url");
                            model.setPlaylistUrl(playlistUrl);
                            //model.setPreview(HtmlParser.getTag(boxHtml, "a img").attr("data-src"));
                            model.setPreview("https://snapshots.xcdnpro.com/thumbnails/"+model.getName()+"?s=" + System.currentTimeMillis());
                            model.setDescription(parseDesription(boxHtml));
                            //model.setOnlineState(parseOnlineState(boxHtml));
                            models.add(model);
                        }
                        response.close();
                        return models;
                    } else {
                        int code = response.code();
                        response.close();
                        throw new IOException("HTTP status " + code);
                    }
                }
            }

            private String parseDesription(String boxHtml) {
                try {
                    return HtmlParser.getText(boxHtml, "div[class~=statusMsg2]");
                } catch(Exception e) {
                    LOG.trace("Couldn't parse description for room");
                }
                return "";
            }
        };
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
