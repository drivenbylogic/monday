package com.monday.app.intelligence.ingestion;

import com.monday.app.intelligence.dto.RawRssItem;
import com.monday.app.intelligence.entity.Article;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

@Component
public class ArticleNormalizer {

    public Article normalize(RawRssItem rawItem) {
        Article article = new Article();
        article.setTitle(normalizeText(rawItem.title()));
        article.setOriginalUrl(rawItem.link());
        article.setCanonicalUrl(normalizeUrl(rawItem.link()));
        article.setDescription(stripHtml(rawItem.description()));
        article.setContentText(stripHtml(rawItem.description())); // Assuming description holds content for now
        article.setAuthor(normalizeText(rawItem.author()));
        article.setPublishedAt(parseDate(rawItem.pubDate()));
        article.setContentHash(generateFingerprint(article.getTitle(), article.getContentText()));
        return article;
    }

    private String normalizeText(String text) {
        if (text == null) return "";
        return text.trim().replaceAll("\\s+", " ");
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", "").replaceAll("&[a-zA-Z0-9#]+;", " ").trim().replaceAll("\\s+", " ");
    }

    private String normalizeUrl(String url) {
        if (url == null) return "";
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase() : "http";
            String host = uri.getHost() != null ? uri.getHost().toLowerCase() : "";
            int port = uri.getPort();
            if ((scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443)) {
                port = -1;
            }
            String path = uri.getPath();
            if (path == null || path.isEmpty()) {
                path = "/";
            } else if (path.endsWith("/") && path.length() > 1) {
                path = path.substring(0, path.length() - 1);
            }
            // Strip utm_ parameters
            String query = uri.getQuery();
            if (query != null) {
                String[] params = query.split("&");
                StringBuilder newQuery = new StringBuilder();
                for (String param : params) {
                    if (!param.toLowerCase().startsWith("utm_")) {
                        if (!newQuery.isEmpty()) {
                            newQuery.append("&");
                        }
                        newQuery.append(param);
                    }
                }
                query = newQuery.isEmpty() ? null : newQuery.toString();
            }
            URI normalized = new URI(scheme, uri.getUserInfo(), host, port, path, query, null);
            return normalized.toString();
        } catch (URISyntaxException e) {
            return url;
        }
    }

    private OffsetDateTime parseDate(String pubDate) {
        if (pubDate == null || pubDate.isBlank()) return OffsetDateTime.now();
        try {
            return OffsetDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME);
        } catch (Exception e) {
            return OffsetDateTime.now();
        }
    }

    private String generateFingerprint(String title, String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = (title != null ? title : "") + "|" + (content != null ? content : "");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
