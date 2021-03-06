package ctbrec.sites.chaturbate;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Model;
import ctbrec.io.HtmlParser;

public class ChaturbateModelParser {
    private static final transient Logger LOG = LoggerFactory.getLogger(ChaturbateModelParser.class);

    public static List<Model> parseModels(Chaturbate chaturbate, String html) {
        List<Model> models = new ArrayList<>();
        Elements cells = HtmlParser.getTags(html, "ul.list > li");
        for (Element cell : cells) {
            String cellHtml = cell.html();
            try {
                Model model = chaturbate.createModel(HtmlParser.getText(cellHtml, "div.title > a").trim());
                model.setName(HtmlParser.getText(cellHtml, "div.title > a").trim());
                model.setPreview(HtmlParser.getTag(cellHtml, "a img").attr("src"));
                model.setDescription(HtmlParser.getText(cellHtml, "div.details ul.subject"));
                Elements tags = HtmlParser.getTags(cellHtml, "div.details ul.subject li a");
                if(tags != null) {
                    for (Element tag : tags) {
                        model.getTags().add(tag.text());
                    }
                }
                models.add(model);
            } catch (Exception e) {
                LOG.error("Parsing of model details failed: {}", cellHtml, e);
            }
        }
        return models;
    }
}
