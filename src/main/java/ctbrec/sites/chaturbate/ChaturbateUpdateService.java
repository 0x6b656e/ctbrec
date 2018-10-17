package ctbrec.sites.chaturbate;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Model;
import ctbrec.io.HttpClient;
import ctbrec.ui.PaginatedScheduledService;
import javafx.concurrent.Task;
import okhttp3.Request;
import okhttp3.Response;

public class ChaturbateUpdateService extends PaginatedScheduledService {

    private static final transient Logger LOG = LoggerFactory.getLogger(ChaturbateUpdateService.class);
    private String url;
    private boolean loginRequired;

    public ChaturbateUpdateService(String url, boolean loginRequired) {
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
                String url = ChaturbateUpdateService.this.url + "?page="+page+"&keywords=&_=" + System.currentTimeMillis();
                LOG.debug("Fetching page {}", url);
                Request request = new Request.Builder().url(url).build();
                Response response = HttpClient.getInstance().execute(request, loginRequired);
                if (response.isSuccessful()) {
                    List<Model> models = ChaturbateModelParser.parseModels(response.body().string());
                    response.close();
                    return models;
                } else {
                    int code = response.code();
                    response.close();
                    throw new IOException("HTTP status " + code);
                }
            }
        };
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
