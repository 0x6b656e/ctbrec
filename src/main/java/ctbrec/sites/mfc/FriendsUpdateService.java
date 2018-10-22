package ctbrec.sites.mfc;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Model;
import ctbrec.ui.PaginatedScheduledService;
import javafx.concurrent.Task;
import okhttp3.Request;
import okhttp3.Response;

public class FriendsUpdateService extends PaginatedScheduledService {

    private static final transient Logger LOG = LoggerFactory.getLogger(FriendsUpdateService.class);
    private MyFreeCams myFreeCams;

    public FriendsUpdateService(MyFreeCams myFreeCams) {
        this.myFreeCams = myFreeCams;
    }

    @Override
    protected Task<List<Model>> createTask() {
        return new Task<List<Model>>() {
            @Override
            public List<Model> call() throws IOException {
                List<Model> models = new ArrayList<>();
                String url = myFreeCams.getBaseUrl() + "/php/manage_lists2.php?passcode=&list_type=friends&data_mode=online&get_user_list=1";
                Request req = new Request.Builder()
                        .url(url)
                        .header("Referer", myFreeCams.getBaseUrl())
                        .build();
                Response resp = myFreeCams.getHttpClient().execute(req, true);
                if(resp.isSuccessful()) {
                    String json = resp.body().string().substring(4);
                    try {
                        JSONObject object = new JSONObject(json);
                        for (String key : object.keySet()) {
                            int uid = Integer.parseInt(key);
                            MyFreeCamsModel model = MyFreeCamsClient.getInstance().getModel(uid);
                            if(model == null) {
                                JSONObject modelObject = object.getJSONObject(key);
                                String name = modelObject.getString("u");
                                model = myFreeCams.createModel(name);
                                SessionState st = new SessionState();
                                st.setM(new ctbrec.sites.mfc.Model());
                                st.getM().setCamscore(0.0);
                                st.setU(new User());
                                st.setUid(uid);
                                st.setLv(modelObject.getInt("lv"));
                                st.setVs(127);
                                model.update(st);
                            }
                            models.add(model);
                        }
                    } catch(Exception e) {
                        LOG.info("JSON: {}", json);
                        throw e;
                    }
                } else {
                    LOG.error("Couldn't load friends list {} {}", resp.code(), resp.message());
                    resp.close();
                }
                return models.stream()
                        .sorted((a, b) -> {
                            try {
                                if(a.isOnline() && b.isOnline() || !a.isOnline() && !b.isOnline()) {
                                    return a.getName().compareTo(b.getName());
                                } else {
                                    if(a.isOnline()) {
                                        return -1;
                                    }
                                    if(b.isOnline()) {
                                        return 1;
                                    }
                                }
                            } catch (IOException | ExecutionException | InterruptedException e) {
                                LOG.warn("Couldn't sort friends list", e);
                                return 0;
                            }
                            return 0;
                        })
                        .skip((page-1) * 50)
                        .limit(50)
                        .collect(Collectors.toList());
            }
        };
    }

}
