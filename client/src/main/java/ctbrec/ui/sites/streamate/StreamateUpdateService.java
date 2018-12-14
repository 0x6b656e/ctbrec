package ctbrec.ui.sites.streamate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.io.XmlParserUtils;
import ctbrec.sites.streamate.Streamate;
import ctbrec.sites.streamate.StreamateModel;
import ctbrec.ui.PaginatedScheduledService;
import javafx.concurrent.Task;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class StreamateUpdateService extends PaginatedScheduledService {

    private static final transient Logger LOG = LoggerFactory.getLogger(StreamateUpdateService.class);

    private static final String URL = "http://affiliate.streamate.com/SMLive/SMLResult.xml";
    private Streamate streamate;
    private String query;

    public StreamateUpdateService(String query, Streamate streamate) {
        this.query = query;
        this.streamate = streamate;
    }

    @Override
    protected Task<List<Model>> createTask() {
        return new Task<List<Model>>() {
            @Override
            public List<Model> call() throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
                LOG.debug("Fetching page {}", URL);
                String q = query
                        .replace("{maxresults}", "50")
                        .replace("{pagenum}", Integer.toString(page));
                //LOG.debug("Query:\n{}", q);
                RequestBody body = RequestBody.create(MediaType.parse("text/xml"), q);
                Request request = new Request.Builder()
                        .url(URL)
                        .addHeader("User-Agent", Config.getInstance().getSettings().httpUserAgent)
                        .addHeader("Accept", "text/xml, */*")
                        .addHeader("Accept-Language", "en")
                        .addHeader("Referer", streamate.getBaseUrl())
                        .post(body)
                        .build();
                Response response = streamate.getHttpClient().execute(request);
                if (response.isSuccessful()) {
                    List<Model> models = new ArrayList<>();
                    String content = response.body().string();
                    ByteArrayInputStream in = new ByteArrayInputStream(content.getBytes("utf-8"));
                    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
                    NodeList performers = doc.getElementsByTagName("Performer");
                    for (int i = 0; i < performers.getLength(); i++) {
                        Node performer = performers.item(i);
                        String name = performer.getAttributes().getNamedItem("Name").getNodeValue();
                        String id = performer.getAttributes().getNamedItem("Id").getNodeValue();
                        String GoldShow = performer.getAttributes().getNamedItem("GoldShow").getNodeValue();
                        String PreGoldShow = performer.getAttributes().getNamedItem("PreGoldShow").getNodeValue();
                        String PartyChat = performer.getAttributes().getNamedItem("PartyChat").getNodeValue();
                        StreamateModel model = (StreamateModel) streamate.createModel(name);
                        model.setId(id);
                        models.add(model);
                        Node pic = XmlParserUtils.getNodeWithXpath(performer, "Media/Pic/Full");
                        String previewUrl = "https:" + pic.getAttributes().getNamedItem("Src").getNodeValue();
                        model.setPreview(previewUrl);
                        //LOG.debug("Name {} - {}{}{}", name, PartyChat, PreGoldShow, GoldShow);
                    }
                    return models;
                } else {
                    int code = response.code();
                    response.close();
                    throw new IOException("HTTP status " + code);
                }
            }
        };
    }
}
