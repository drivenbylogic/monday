package com.monday.app.intelligence.ingestion;

import com.monday.app.intelligence.dto.RawRssFeed;
import com.monday.app.intelligence.dto.RawRssItem;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@Component
public class RssParser {

    public RawRssFeed parse(String xmlContent) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // Prevent XXE
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(xmlContent)));

        Element channel = (Element) document.getElementsByTagName("channel").item(0);
        if (channel == null) {
            throw new IllegalArgumentException("Invalid RSS format: Missing <channel>");
        }

        String feedTitle = getTextContent(channel, "title");
        String feedLink = getTextContent(channel, "link");
        String feedDescription = getTextContent(channel, "description");
        String feedLanguage = getTextContent(channel, "language");

        List<RawRssItem> items = new ArrayList<>();
        NodeList itemNodes = document.getElementsByTagName("item");
        for (int i = 0; i < itemNodes.getLength(); i++) {
            Element itemElement = (Element) itemNodes.item(i);
            
            String title = getTextContent(itemElement, "title");
            String link = getTextContent(itemElement, "link");
            String description = getTextContent(itemElement, "description");
            // Some feeds use content:encoded
            if (description == null || description.isBlank()) {
                description = getTextContent(itemElement, "content:encoded");
            }
            String author = getTextContent(itemElement, "author");
            if (author == null || author.isBlank()) {
                author = getTextContent(itemElement, "dc:creator");
            }
            String guid = getTextContent(itemElement, "guid");
            String pubDate = getTextContent(itemElement, "pubDate");
            
            List<String> categories = new ArrayList<>();
            NodeList categoryNodes = itemElement.getElementsByTagName("category");
            for (int j = 0; j < categoryNodes.getLength(); j++) {
                categories.add(categoryNodes.item(j).getTextContent().trim());
            }

            items.add(new RawRssItem(title, link, description, author, categories, guid, pubDate));
        }

        return new RawRssFeed(feedTitle, feedLink, feedDescription, feedLanguage, items);
    }

    private String getTextContent(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() > 0 && list.item(0) != null) {
            return list.item(0).getTextContent().trim();
        }
        return null;
    }
}
