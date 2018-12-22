package ctbrec.sites.jasmin;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.io.HtmlParser;
import ctbrec.io.HttpClient;
import ctbrec.io.HttpException;
import ctbrec.sites.AbstractSite;
import okhttp3.Request;
import okhttp3.Response;

public class LiveJasmin extends AbstractSite {

    public static final String BASE_URL = "https://www.livejasmin.com";
    private HttpClient httpClient;

    @Override
    public String getName() {
        return "LiveJasmin";
    }

    @Override
    public String getBaseUrl() {
        return BASE_URL;
    }

    @Override
    public String getAffiliateLink() {
        return "https://awejmp.com/?siteId=jasmin&categoryName=girl&pageName=listpage&performerName=&prm[psid]=0xb00bface&prm[pstool]=205_1&prm[psprogram]=pps&prm[campaign_id]=&subAffId={SUBAFFID}&filters=";
    }

    @Override
    public Model createModel(String name) {
        LiveJasminModel model = new LiveJasminModel();
        model.setName(name);
        model.setDescription("");
        model.setSite(this);
        model.setUrl(getBaseUrl() + "/en/chat/" + name);
        return model;
    }

    @Override
    public Integer getTokenBalance() throws IOException {
        return 0;
    }

    @Override
    public String getBuyTokensLink() {
        return getAffiliateLink();
    }

    @Override
    public boolean login() throws IOException {
        return getHttpClient().login();
    }

    @Override
    public HttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = new LiveJasminHttpClient();
        }
        return httpClient;
    }

    @Override
    public void init() throws IOException {
    }

    @Override
    public void shutdown() {
        if (httpClient != null) {
            httpClient.shutdown();
        }
    }

    @Override
    public boolean supportsTips() {
        return false;
    }

    @Override
    public boolean supportsFollow() {
        return true;
    }

    @Override
    public boolean supportsSearch() {
        return true;
    }

    @Override
    public List<Model> search(String q) throws IOException, InterruptedException {
        String query = URLEncoder.encode(q, "utf-8");
        long ts = System.currentTimeMillis();
        String url = getBaseUrl() + "/en/auto-suggest-search/auto-suggest?category=girls&searchText=" + query + "&_dc=" + ts + "&appletType=html5";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                .addHeader("Accept", "*/*")
                .addHeader("Accept-Language", "en")
                .addHeader("Referer", getBaseUrl())
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .build();
        try (Response response = getHttpClient().execute(request)) {
            if (response.isSuccessful()) {
                String body = response.body().string();
                JSONObject json = new JSONObject(body);
                if(json.optBoolean("success")) {
                    List<Model> models = new ArrayList<>();
                    JSONObject data = json.getJSONObject("data");
                    String html = data.getString("content");
                    Elements items = HtmlParser.getTags(html, "li.name");
                    for (Element item : items) {
                        String itemHtml = item.html();
                        Element link = HtmlParser.getTag(itemHtml, "a");
                        LiveJasminModel model = (LiveJasminModel) createModel(link.attr("title"));
                        Element pic = HtmlParser.getTag(itemHtml, "span.pic i");
                        String style = pic.attr("style");
                        Matcher m = Pattern.compile("url\\('(.*?)'\\)").matcher(style);
                        if(m.find()) {
                            model.setPreview(m.group(1));
                        }
                        models.add(model);
                    }
                    return models;
                } else {
                    throw new IOException("Response was not successful: " + url + "\n" + body);
                }
            } else {
                throw new HttpException(response.code(), response.message());
            }
        }
    }

    @Override
    public boolean isSiteForModel(Model m) {
        return m instanceof LiveJasminModel;
    }

    @Override
    public boolean credentialsAvailable() {
        return !Config.getInstance().getSettings().livejasminSession.isEmpty();
    }

}
