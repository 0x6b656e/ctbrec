package ctbrec.sites.mfc;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jetty.util.StringUtil;
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
    private Mode mode = Mode.ONLINE;

    public static enum Mode {
        ONLINE,
        OFFLINE
    }

    public FriendsUpdateService(MyFreeCams myFreeCams) {
        this.myFreeCams = myFreeCams;
    }

    @Override
    protected Task<List<Model>> createTask() {
        return new Task<List<Model>>() {
            @Override
            public List<Model> call() throws IOException {
                if(StringUtil.isBlank(ctbrec.Config.getInstance().getSettings().mfcUsername)) {
                    return Collections.emptyList();
                } else {
                    List<MyFreeCamsModel> models = new ArrayList<>();
                    String url = myFreeCams.getBaseUrl() + "/php/manage_lists2.php?passcode=&list_type=friends&data_mode=online&get_user_list=1";
                    Request req = new Request.Builder()
                            .url(url)
                            .header("Referer", myFreeCams.getBaseUrl())
                            .build();
                    Response resp = myFreeCams.getHttpClient().execute(req, true);
                    if(resp.isSuccessful()) {
                        String body = resp.body().string().substring(4);
                        try {
                            JSONObject json = new JSONObject(body);
                            for (String key : json.keySet()) {
                                int uid = Integer.parseInt(key);
                                MyFreeCamsModel model = MyFreeCamsClient.getInstance().getModel(uid);
                                if(model == null) {
                                    JSONObject modelObject = json.getJSONObject(key);
                                    String name = modelObject.getString("u");
                                    model = myFreeCams.createModel(name);
                                    SessionState st = new SessionState();
                                    st.setM(new ctbrec.sites.mfc.Model());
                                    st.getM().setCamscore(0.0);
                                    st.setU(new User());
                                    st.setUid(uid);
                                    st.setLv(modelObject.getInt("lv"));
                                    st.setVs(127);

                                    model.update(st, myFreeCams.getClient().getStreamUrl(st));
                                }
                                models.add(model);
                            }
                        } catch(Exception e) {
                            LOG.info("Exception getting friends list. Response was: {}", body);
                        }
                    } else {
                        LOG.error("Couldn't load friends list {} {}", resp.code(), resp.message());
                        resp.close();
                    }
                    boolean filterOnline = mode == Mode.ONLINE;
                    return models.stream()
                            .filter(m -> {
                                try {
                                    return m.isOnline() == filterOnline;
                                } catch(Exception e) {
                                    return false;
                                }
                            })
                            .sorted((m1,m2) -> (int)(m2.getCamScore() - m1.getCamScore()))
                            .skip((page-1) * 50)
                            .limit(50)
                            .collect(Collectors.toList());
                }
            }
        };
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }
}
