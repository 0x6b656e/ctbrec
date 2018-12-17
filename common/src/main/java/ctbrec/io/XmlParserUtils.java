package ctbrec.io;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XmlParserUtils {

    public static Document parse(String xml) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    public static Node getFirstElementByTagName(Document doc, String tagName) {
        NodeList list = doc.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            return list.item(0);
        } else {
            return null;
        }
    }

    public static String getTextContent(Document doc, String tagName) {
        Node node = getFirstElementByTagName(doc, tagName);
        if (node != null) {
            return node.getTextContent();
        } else {
            return null;
        }
    }

    public static String getTextContent(Node parent, String tagName) {
        Node node = findChildWithTagName(parent, tagName);
        if (node != null) {
            return node.getTextContent();
        } else {
            return null;
        }
    }

    public static Node findChildWithTagName(Node parent, String tagName) {
        if (parent == null) {
            return null;
        }

        NodeList childs = parent.getChildNodes();
        for (int i = 0; i < childs.getLength(); i++) {
            Node child = childs.item(i);
            if (child.getNodeName().equals(tagName)) {
                return child;
            } else if (child.hasChildNodes()) {
                Node result = findChildWithTagName(child, tagName);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    public static void getElementsByTagName(Node parent, String tagName, List<Node> result) {
        if (parent == null) {
            return;
        }

        NodeList childs = parent.getChildNodes();
        for (int i = 0; i < childs.getLength(); i++) {
            Node child = childs.item(i);
            if (child.getNodeName().equals(tagName)) {
                result.add(child);
            } else if (child.hasChildNodes()) {
                getElementsByTagName(child, tagName, result);
            }
        }
    }

    public static String getStringWithXpath(String xml, String xpath) throws XPathExpressionException {
        XPath xp = XPathFactory.newInstance().newXPath();
        return xp.evaluate(xpath, new InputSource(new StringReader(xml)));
    }

    public static String getStringWithXpath(Node node, String xpath) throws XPathExpressionException {
        XPath xp = XPathFactory.newInstance().newXPath();
        return xp.evaluate(xpath, node);
    }

    public static Node getNodeWithXpath(String xml, String xpath) throws XPathExpressionException {
        XPath xp = XPathFactory.newInstance().newXPath();
        return (Node) xp.evaluate(xpath, new InputSource(new StringReader(xml)), XPathConstants.NODE);
    }

    public static Node getNodeWithXpath(Node node, String xpath) throws XPathExpressionException {
        XPath xp = XPathFactory.newInstance().newXPath();
        return (Node) xp.evaluate(xpath, node, XPathConstants.NODE);
    }

    public static NodeList getNodeListWithXpath(String xml, String xpath) throws XPathExpressionException {
        XPath xp = XPathFactory.newInstance().newXPath();
        return (NodeList) xp.evaluate(xpath, new InputSource(new StringReader(xml)), XPathConstants.NODESET);
    }
}
