package ctbrec.sites.mfc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONObject;

import ctbrec.io.HttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ServerConfig {

    List<String> ajaxServers;
    List<String> videoServers;
    List<String> chatServers;
    Map<String, String> h5Servers;
    Map<String, String> wsServers;
    Map<String, String> wzobsServers;
    Map<String, String> ngVideoServers;

    public ServerConfig(HttpClient client) throws IOException {
        Request req = new Request.Builder().url("http://www.myfreecams.com/_js/serverconfig.js").build();
        Response resp = client.execute(req);
        String json = resp.body().string();

        JSONObject serverConfig = new JSONObject(json);
        ajaxServers = parseList(serverConfig, "ajax_servers");
        videoServers = parseList(serverConfig, "video_servers");
        chatServers = parseList(serverConfig, "chat_servers");
        h5Servers = parseMap(serverConfig, "h5video_servers");
        wsServers = parseMap(serverConfig, "websocket_servers");
        wzobsServers = parseMap(serverConfig, "wzobs_servers");
        ngVideoServers = parseMap(serverConfig, "ngvideo_servers");
        //        System.out.println("wz " + wzobsServers);
        //        System.out.println("ng " + ngVideoServers);
        //        System.out.println("h5 " + h5Servers);
    }

    private static Map<String, String> parseMap(JSONObject serverConfig, String name) {
        JSONObject servers = serverConfig.getJSONObject(name);
        Map<String, String> result = new HashMap<>();
        for (String key : servers.keySet()) {
            result.put(key, servers.getString(key));
        }
        return result;
    }

    private static List<String> parseList(JSONObject serverConfig, String name) {
        JSONArray servers = serverConfig.getJSONArray(name);
        List<String> result = new ArrayList<>(servers.length());
        for (Object server : servers) {
            result.add((String) server);
        }
        return result;
    }

    public boolean isOnNgServer(SessionState state) {
        int camserv = Objects.requireNonNull(Objects.requireNonNull(state.getU()).getCamserv());
        return ngVideoServers.containsKey(Integer.toString(camserv));
    }

    public boolean isOnWzObsVideoServer(SessionState state) {
        int camserv = Objects.requireNonNull(Objects.requireNonNull(state.getU()).getCamserv());
        return wzobsServers.containsKey(Integer.toString(camserv));
    }

    public boolean isOnHtml5VideoServer(SessionState state) {
        int camserv = Objects.requireNonNull(Objects.requireNonNull(state.getU()).getCamserv());
        return h5Servers.containsKey(Integer.toString(camserv)) || (camserv >= 904 && camserv <= 915 || camserv >= 940 && camserv <= 960);
    }
}
