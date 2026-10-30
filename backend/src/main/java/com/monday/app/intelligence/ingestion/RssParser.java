package com.monday.app.intelligence.ingestion;

import com.monday.app.intelligence.dto.RawRssFeed;
import com.monday.app.intelligence.dto.RawRssItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@Component
public class RssParser {

    private static final Logger log = LoggerFactory.getLogger(RssParser.class);

    public RawRssFeed parse(String xmlContent) throws Exception {
        if (xmlContent == null || xmlContent.isBlank()) {
            throw new IllegalArgumentException("Feed XML content is empty");
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        // Robust XXE Prevention
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
        } catch (Exception e) {
            log.debug("XXE feature flags warning: {}", e.getMessage());
        }

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(xmlContent)));
        Element root = document.getDocumentElement();

        if (root == null) {
            throw new IllegalArgumentException("Invalid XML document: No root element");
        }

        String rootName = root.getLocalName() != null ? root.getLocalName() : root.getTagName();

        if ("feed".equalsIgnoreCase(rootName)) {
            return parseAtomFeed(root);
        } else {
            return parseRssFeed(root, document);
        }
    }

    private RawRssFeed parseRssFeed(Element root, Document document) {
        Element channel = (Element) document.getElementsByTagName("channel").item(0);
        if (channel == null) {
            throw new IllegalArgumentException("Invalid RSS format: Missing <channel>");
        }

        String feedTitle = getDirectChildText(channel, "title");
        String feedLink = getDirectChildText(channel, "link");
        String feedDescription = getDirectChildText(channel, "description");
        String feedLanguage = getDirectChildText(channel, "language");

        List<RawRssItem> items = new ArrayList<>();
        NodeList itemNodes = channel.getElementsByTagName("item");
        for (int i = 0; i < itemNodes.getLength(); i++) {
            Element itemElement = (Element) itemNodes.item(i);

            String title = getDirectChildText(itemElement, "title");
            String link = getDirectChildText(itemElement, "link");
            String description = getDirectChildText(itemElement, "description");
            if (description == null || description.isBlank()) {
                description = getDirectChildText(itemElement, "content:encoded");
            }
            if (description == null || description.isBlank()) {
                description = getDirectChildText(itemElement, "encoded");
            }

            String author = getDirectChildText(itemElement, "author");
            if (author == null || author.isBlank()) {
                author = getDirectChildText(itemElement, "dc:creator");
            }
            if (author == null || author.isBlank()) {
                author = getDirectChildText(itemElement, "creator");
            }

            String guid = getDirectChildText(itemElement, "guid");
            if (guid == null || guid.isBlank()) {
                guid = link;
            }

            String pubDate = getDirectChildText(itemElement, "pubDate");
            if (pubDate == null || pubDate.isBlank()) {
                pubDate = getDirectChildText(itemElement, "dc:date");
            }

            List<String> categories = new ArrayList<>();
            NodeList categoryNodes = itemElement.getElementsByTagName("category");
            for (int j = 0; j < categoryNodes.getLength(); j++) {
                String cat = categoryNodes.item(j).getTextContent();
                if (cat != null && !cat.isBlank()) {
                    categories.add(cat.trim());
                }
            }

            items.add(new RawRssItem(title, link, description, author, categories, guid, pubDate));
        }

        return new RawRssFeed(feedTitle, feedLink, feedDescription, feedLanguage, items);
    }

    private RawRssFeed parseAtomFeed(Element feedElement) {
        String feedTitle = getDirectChildText(feedElement, "title");
        String feedDescription = getDirectChildText(feedElement, "subtitle");
        String feedLink = getAtomLink(feedElement);
        String feedLanguage = feedElement.getAttribute("xml:lang");

        List<RawRssItem> items = new ArrayList<>();
        NodeList entryNodes = feedElement.getElementsByTagName("entry");
        for (int i = 0; i < entryNodes.getLength(); i++) {
            Element entryElement = (Element) entryNodes.item(i);

            String title = getDirectChildText(entryElement, "title");
            String link = getAtomLink(entryElement);

            String description = getDirectChildText(entryElement, "content");
            if (description == null || description.isBlank()) {
                description = getDirectChildText(entryElement, "summary");
            }

            String author = null;
            NodeList authorNodes = entryElement.getElementsByTagName("author");
            if (authorNodes.getLength() > 0 && authorNodes.item(0) instanceof Element aElem) {
                author = getDirectChildText(aElem, "name");
            }

            String guid = getDirectChildText(entryElement, "id");
            if (guid == null || guid.isBlank()) {
                guid = link;
            }

            String pubDate = getDirectChildText(entryElement, "published");
            if (pubDate == null || pubDate.isBlank()) {
                pubDate = getDirectChildText(entryElement, "updated");
            }

            List<String> categories = new ArrayList<>();
            NodeList categoryNodes = entryElement.getElementsByTagName("category");
            for (int j = 0; j < categoryNodes.getLength(); j++) {
                if (categoryNodes.item(j) instanceof Element cElem) {
                    String term = cElem.getAttribute("term");
                    if (term != null && !term.isBlank()) {
                        categories.add(term.trim());
                    } else if (cElem.getTextContent() != null && !cElem.getTextContent().isBlank()) {
                        categories.add(cElem.getTextContent().trim());
                    }
                }
            }

            items.add(new RawRssItem(title, link, description, author, categories, guid, pubDate));
        }

        return new RawRssFeed(feedTitle, feedLink, feedDescription, feedLanguage, items);
    }

    private String getAtomLink(Element parent) {
        NodeList links = parent.getElementsByTagName("link");
        String fallbackLink = null;
        for (int i = 0; i < links.getLength(); i++) {
            if (links.item(i) instanceof Element lElem) {
                String href = lElem.getAttribute("href");
                String rel = lElem.getAttribute("rel");
                if (href != null && !href.isBlank()) {
                    if ("alternate".equalsIgnoreCase(rel) || rel.isBlank()) {
                        return href.trim();
                    }
                    if (fallbackLink == null) {
                        fallbackLink = href.trim();
                    }
                }
            }
        }
        return fallbackLink;
    }

    private String getDirectChildText(Element parent, String tagName) {
        NodeList list = parent.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String name = node.getNodeName();
                String localName = node.getLocalName();
                if (tagName.equalsIgnoreCase(name) || tagName.equalsIgnoreCase(localName) ||
                    (localName != null && tagName.endsWith(":" + localName))) {
                    return node.getTextContent() != null ? node.getTextContent().trim() : null;
                }
            }
        }
        return null;
    }
}
