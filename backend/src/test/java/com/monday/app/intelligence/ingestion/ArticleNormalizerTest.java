package com.monday.app.intelligence.ingestion;

import com.monday.app.intelligence.dto.RawRssItem;
import com.monday.app.intelligence.entity.Article;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArticleNormalizerTest {

    private ArticleNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new ArticleNormalizer();
    }

    @Test
    void cleanHtml_stripsScriptsStylesAndTagsAndDecodesEntities() {
        String raw = "<div><script>alert('pwn');</script><p>Breaking &amp; exclusive: Apple announces new M5 chip &copy; 2026.</p></div>";
        String cleaned = normalizer.cleanHtml(raw);
        assertFalse(cleaned.contains("alert"));
        assertFalse(cleaned.contains("<p>"));
        assertTrue(cleaned.contains("Breaking & exclusive"));
        assertTrue(cleaned.contains("Apple announces new M5 chip"));
    }

    @Test
    void normalizeUrl_stripsTrackingParamsAndFragments() {
        String dirtyUrl = "https://example.com/posts/agent-ai/?utm_source=twitter&utm_medium=social&article_id=42&ref=newsletter#comments";
        String normalized = normalizer.normalizeUrl(dirtyUrl);

        assertFalse(normalized.contains("utm_source"));
        assertFalse(normalized.contains("utm_medium"));
        assertFalse(normalized.contains("ref=newsletter"));
        assertFalse(normalized.contains("#comments"));
        assertTrue(normalized.contains("article_id=42"));
        assertTrue(normalized.startsWith("https://example.com/posts/agent-ai"));
    }

    @Test
    void normalize_createsWellFormedArticle() {
        RawRssItem raw = new RawRssItem(
                "  Next-Gen AI Hardware Architectures  ",
                "https://tech.org/posts/ai-hw?utm_source=rss",
                "<p>Detailed exploration of wafer-scale engine benchmarks and interconnect bandwidth.</p>",
                "Dr. Hardware",
                List.of("Hardware", "AI"),
                "guid-123",
                "2026-10-28T10:00:00Z"
        );

        Article article = normalizer.normalize(raw, "ARTIFICIAL_INTELLIGENCE");
        assertNotNull(article);
        assertEquals("Next-Gen AI Hardware Architectures", article.getTitle());
        assertEquals("https://tech.org/posts/ai-hw", article.getCanonicalUrl());
        assertEquals("Dr. Hardware", article.getAuthor());
        assertEquals("ARTIFICIAL_INTELLIGENCE", article.getAreaOfInterest());
        assertNotNull(article.getContentHash());
        assertEquals(64, article.getContentHash().length());
    }

    @Test
    void normalize_discardsEmptyOrUnusableEntries() {
        RawRssItem empty = new RawRssItem("", "https://example.com", "", null, List.of(), null, null);
        assertNull(normalizer.normalize(empty, "TECH"));
    }
}
