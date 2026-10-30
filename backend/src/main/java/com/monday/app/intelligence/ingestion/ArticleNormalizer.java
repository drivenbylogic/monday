package com.monday.app.intelligence.ingestion;

import com.monday.app.intelligence.dto.RawRssItem;
import com.monday.app.intelligence.entity.Article;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class ArticleNormalizer {

    private static final int MAX_TEXT_LENGTH = 8000;
    private static final int MIN_TEXT_LENGTH = 20;

    private static final Pattern SCRIPT_STYLE_PATTERN = Pattern.compile(
            "<(script|style)[^>]*>.*?</\\1>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private static final Set<String> TRACKING_PARAMS = Set.of(
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
            "utm_cid", "utm_reader", "fbclid", "gclid", "mc_cid", "mc_eid",
            "_ga", "ref", "source", "ref_src"
    );

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.RFC_1123_DATE_TIME,
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ISO_INSTANT,
            DateTimeFormatter.ISO_DATE_TIME
    );

    @Value("${intelligence.cost.max-input-chars:8000}")
    private int maxInputChars = 8000;

    public Article normalize(RawRssItem rawItem, String areaOfInterest) {
        if (rawItem == null) return null;

        String title = normalizeText(rawItem.title());
        if (title.isBlank()) return null;

        String originalUrl = rawItem.link() != null ? rawItem.link().trim() : "";
        String canonicalUrl = normalizeUrl(originalUrl);
        if (canonicalUrl.isBlank()) return null;

        String rawContent = rawItem.description();
        String cleanedContent = cleanHtml(rawContent);

        // Discard empty or unusable entries
        if (cleanedContent.length() < MIN_TEXT_LENGTH && title.length() < MIN_TEXT_LENGTH) {
            return null;
        }

        if (cleanedContent.length() > maxInputChars) {
            cleanedContent = cleanedContent.substring(0, maxInputChars);
        }

        String author = normalizeText(rawItem.author());
        OffsetDateTime publishedAt = parseDate(rawItem.pubDate());
        OffsetDateTime now = OffsetDateTime.now();

        String contentHash = generateFingerprint(title, cleanedContent);

        Article article = new Article();
        article.setTitle(title);
        article.setOriginalUrl(originalUrl);
        article.setCanonicalUrl(canonicalUrl);
        article.setDescription(cleanedContent.length() > 500 ? cleanedContent.substring(0, 500) + "..." : cleanedContent);
        article.setContentText(cleanedContent);
        article.setAuthor(author.isBlank() ? null : author);
        article.setPublishedAt(publishedAt);
        article.setContentHash(contentHash);
        article.setAreaOfInterest(areaOfInterest != null ? areaOfInterest : "TECHNOLOGY");
        article.setProcessingStatus("DISCOVERED");
        article.setFirstSeenAt(now);
        article.setLastSeenAt(now);
        article.setCreatedAt(now);
        article.setUpdatedAt(now);

        return article;
    }

    public String cleanHtml(String html) {
        if (html == null) return "";
        // 1. Remove script and style tags and their contents
        String noScripts = SCRIPT_STYLE_PATTERN.matcher(html).replaceAll(" ");
        // 2. Remove all HTML tags
        String noTags = HTML_TAG_PATTERN.matcher(noScripts).replaceAll(" ");
        // 3. Decode entities
        String decoded = decodeHtmlEntities(noTags);
        // 4. Normalize Unicode
        String normalized = Normalizer.normalize(decoded, Normalizer.Form.NFC);
        // 5. Collapse whitespace
        return WHITESPACE_PATTERN.matcher(normalized).replaceAll(" ").trim();
    }

    public String normalizeUrl(String url) {
        if (url == null || url.isBlank()) return "";
        try {
            URI uri = new URI(url.trim());
            String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase() : "https";
            String host = uri.getHost() != null ? uri.getHost().toLowerCase() : "";
            if (host.isEmpty()) return url;

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

            // Strip tracking query parameters but preserve functional query parameters
            String query = uri.getRawQuery();
            String newQuery = null;
            if (query != null && !query.isBlank()) {
                String[] params = query.split("&");
                List<String> retained = new ArrayList<>();
                for (String param : params) {
                    if (param.isBlank()) continue;
                    int eqIdx = param.indexOf('=');
                    String key = eqIdx > 0 ? param.substring(0, eqIdx).toLowerCase() : param.toLowerCase();
                    if (!TRACKING_PARAMS.contains(key) && !key.startsWith("utm_")) {
                        retained.add(param);
                    }
                }
                if (!retained.isEmpty()) {
                    Collections.sort(retained);
                    newQuery = String.join("&", retained);
                }
            }

            // Discard fragment (#)
            URI normalized = new URI(scheme, null, host, port, path, newQuery, null);
            return normalized.toASCIIString();
        } catch (URISyntaxException e) {
            return url.trim();
        }
    }

    public String normalizeText(String text) {
        if (text == null) return "";
        String cleaned = HTML_TAG_PATTERN.matcher(text).replaceAll(" ");
        String decoded = decodeHtmlEntities(cleaned);
        return WHITESPACE_PATTERN.matcher(decoded).replaceAll(" ").trim();
    }

    public OffsetDateTime parseDate(String pubDate) {
        if (pubDate == null || pubDate.isBlank()) {
            return OffsetDateTime.now();
        }

        String trimmed = pubDate.trim();
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return OffsetDateTime.parse(trimmed, formatter);
            } catch (Exception ignored) {}
        }

        // Try standard Instant parse
        try {
            return OffsetDateTime.ofInstant(Instant.parse(trimmed), ZoneOffset.UTC);
        } catch (Exception ignored) {}

        return OffsetDateTime.now();
    }

    public String generateFingerprint(String title, String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = (title != null ? title.toLowerCase() : "") + "|" +
                           (content != null ? content.toLowerCase() : "");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private String decodeHtmlEntities(String input) {
        if (input == null || !input.contains("&")) return input != null ? input : "";
        return input.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&ndash;", "–")
                .replace("&mdash;", "—")
                .replace("&hellip;", "…")
                .replace("&#8217;", "’")
                .replace("&#8216;", "‘")
                .replace("&#8220;", "“")
                .replace("&#8221;", "”");
    }
}
