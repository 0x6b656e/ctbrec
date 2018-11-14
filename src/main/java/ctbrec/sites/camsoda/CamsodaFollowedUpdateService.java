package ctbrec.sites.camsoda;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import ctbrec.Model;
import ctbrec.io.HttpException;
import ctbrec.ui.PaginatedScheduledService;
import javafx.concurrent.Task;
import okhttp3.Request;
import okhttp3.Response;

public class CamsodaFollowedUpdateService extends PaginatedScheduledService {
    private Camsoda camsoda;
    private boolean showOnline = true;

    public CamsodaFollowedUpdateService(Camsoda camsoda) {
        this.camsoda = camsoda;
    }

    @Override
    protected Task<List<Model>> createTask() {
        return new Task<List<Model>>() {
            @Override
            public List<Model> call() throws IOException {
                List<Model> models = new ArrayList<>();
                String url = camsoda.getBaseUrl() + "/api/v1/user/current";
                Request request = new Request.Builder().url(url).build();
                try(Response response = camsoda.getHttpClient().execute(request, true)) {
                    if (response.isSuccessful()) {
                        JSONObject json = new JSONObject(response.body().string());
                        if(json.has("status") && json.getBoolean("status")) {
                            JSONObject user = json.getJSONObject("user");
                            JSONArray following = user.getJSONArray("following");
                            for (int i = 0; i < following.length(); i++) {
                                JSONObject m = following.getJSONObject(i);
                                CamsodaModel model = (CamsodaModel) camsoda.createModel(m.getString("followname"));
                                boolean online = m.getInt("online") == 1;
                                model.setOnlineState(online ? "online" : "offline");
                                model.setPreview("https://md.camsoda.com/thumbs/" + model.getName() + ".jpg");
                                models.add(model);
                            }
                            return models.stream()
                                    .filter((m) -> {
                                        try {
                                            return m.isOnline() == showOnline;
                                        } catch (IOException | ExecutionException | InterruptedException e) {
                                            return false;
                                        }
                                    }).collect(Collectors.toList());
                        } else {
                            response.close();
                            return Collections.emptyList();
                        }
                    } else {
                        throw new HttpException(response.code(), response.message());
                    }
                }
            }
        };
    }

    void showOnline(boolean online) {
        this.showOnline = online;
    }
}
