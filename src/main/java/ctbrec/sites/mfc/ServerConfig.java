package ctbrec.sites.mfc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ServerConfig {

    List<String> ajaxServers;
    List<String> videoServers;
    List<String> chatServers;
    Map<String, String> h5Servers;
    Map<String, String> wsServers;
    Map<String, String> wzobsServers;
    Map<String, String> ngVideo;

    public ServerConfig(OkHttpClient client) throws IOException {
        Request req = new Request.Builder().url("http://www.myfreecams.com/_js/serverconfig.js").build();
        Response resp = client.newCall(req).execute();
        String json = resp.body().string();

        JSONObject serverConfig = new JSONObject(json);
        ajaxServers = parseList(serverConfig, "ajax_servers");
        videoServers = parseList(serverConfig, "video_servers");
        chatServers = parseList(serverConfig, "chat_servers");
        h5Servers = parseMap(serverConfig, "h5video_servers");
        wsServers = parseMap(serverConfig, "websocket_servers");
        wzobsServers = parseMap(serverConfig, "wzobs_servers");
        ngVideo = parseMap(serverConfig, "ngvideo_servers");
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

}
