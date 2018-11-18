package ctbrec.ui.sites.cam4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.io.HtmlParser;
import ctbrec.io.HttpException;
import ctbrec.sites.cam4.Cam4;
import ctbrec.sites.cam4.Cam4Model;
import ctbrec.ui.PaginatedScheduledService;
import ctbrec.ui.SiteUiFactory;
import javafx.concurrent.Task;
import okhttp3.Request;
import okhttp3.Response;

public class Cam4FollowedUpdateService extends PaginatedScheduledService {

    private static final transient Logger LOG = LoggerFactory.getLogger(Cam4FollowedUpdateService.class);
    private Cam4 site;
    private boolean showOnline = true;

    public Cam4FollowedUpdateService(Cam4 site) {
        this.site = site;
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
                // login first
                SiteUiFactory.getUi(site).login();
                List<Model> models = new ArrayList<>();
                String username = Config.getInstance().getSettings().cam4Username;
                String url = site.getBaseUrl() + '/' + username + "/edit/friends_favorites";
                Request req = new Request.Builder().url(url).build();
                try(Response response = site.getHttpClient().execute(req)) {
                    if(response.isSuccessful()) {
                        String content = response.body().string();
                        Elements cells = HtmlParser.getTags(content, "div#favorites div.ff_thumb");
                        for (Element cell : cells) {
                            String cellHtml = cell.html();
                            Element link = HtmlParser.getTag(cellHtml, "div.ff_img a");
                            String path = link.attr("href");
                            String modelName = path.substring(1);
                            Cam4Model model = (Cam4Model) site.createModel(modelName);
                            model.setPreview("https://snapshots.xcdnpro.com/thumbnails/"+model.getName()+"?s=" + System.currentTimeMillis());
                            model.setOnlineState(parseOnlineState(cellHtml));
                            models.add(model);
                        }
                        return models.stream()
                                .filter(m -> {
                                    try {
                                        return m.isOnline() == showOnline;
                                    } catch (IOException | ExecutionException | InterruptedException e) {
                                        LOG.error("Couldn't determine online state", e);
                                        return false;
                                    }
                                }).collect(Collectors.toList());
                    } else {
                        throw new HttpException(response.code(), response.message());
                    }
                }
            }

            private String parseOnlineState(String cellHtml) {
                Element state = HtmlParser.getTag(cellHtml, "div.ff_name div");
                return state.attr("class").equals("online") ? "NORMAL" : "OFFLINE";
            }
        };
    }

    public void setShowOnline(boolean online) {
        this.showOnline = online;
    }
}
