package ctbrec.ui.sites.chaturbate;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Model;
import ctbrec.StringUtil;
import ctbrec.sites.chaturbate.Chaturbate;
import ctbrec.sites.chaturbate.ChaturbateModelParser;
import ctbrec.ui.PaginatedScheduledService;
import ctbrec.ui.SiteUiFactory;
import javafx.concurrent.Task;
import okhttp3.Request;
import okhttp3.Response;

public class ChaturbateUpdateService extends PaginatedScheduledService {

    private static final transient Logger LOG = LoggerFactory.getLogger(ChaturbateUpdateService.class);
    private String url;
    private boolean loginRequired;
    private Chaturbate chaturbate;

    public ChaturbateUpdateService(String url, boolean loginRequired, Chaturbate chaturbate) {
        this.url = url;
        this.loginRequired = loginRequired;
        this.chaturbate = chaturbate;

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
                if(loginRequired && StringUtil.isBlank(ctbrec.Config.getInstance().getSettings().username)) {
                    return Collections.emptyList();
                } else {
                    String url = ChaturbateUpdateService.this.url + "?page="+page+"&keywords=&_=" + System.currentTimeMillis();
                    LOG.debug("Fetching page {}", url);
                    if(loginRequired) {
                        SiteUiFactory.getUi(chaturbate).login();
                    }
                    Request request = new Request.Builder().url(url).build();
                    Response response = chaturbate.getHttpClient().execute(request);
                    if (response.isSuccessful()) {
                        List<Model> models = ChaturbateModelParser.parseModels(chaturbate, response.body().string());
                        response.close();
                        return models;
                    } else {
                        int code = response.code();
                        response.close();
                        throw new IOException("HTTP status " + code);
                    }
                }
            }
        };
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
