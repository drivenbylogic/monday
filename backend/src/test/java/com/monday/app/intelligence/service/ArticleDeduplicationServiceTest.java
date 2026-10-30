package com.monday.app.intelligence.service;

import com.monday.app.intelligence.entity.Article;
import com.monday.app.intelligence.entity.RssFeed;
import com.monday.app.intelligence.repository.ArticleRepository;
import com.monday.app.intelligence.repository.ArticleSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ArticleDeduplicationServiceTest {

    private ArticleRepository articleRepository;
    private ArticleSourceRepository articleSourceRepository;
    private ArticleDeduplicationService deduplicationService;
    private RssFeed sampleFeed;

    @BeforeEach
    void setUp() {
        articleRepository = mock(ArticleRepository.class);
        articleSourceRepository = mock(ArticleSourceRepository.class);
        deduplicationService = new ArticleDeduplicationService(articleRepository, articleSourceRepository);

        sampleFeed = new RssFeed();
        sampleFeed.setId(UUID.randomUUID());
        sampleFeed.setName("Tech Feed");
        sampleFeed.setFeedUrl("https://tech.com/rss");
    }

    @Test
    void calculateTitleSimilarity_detectsSimilarTitles() {
        String t1 = "Anthropic announces Claude 3.7 with Hybrid Reasoning capabilities";
        String t2 = "Anthropic announces Claude 3.7 with Hybrid Reasoning in benchmark";

        double similarity = deduplicationService.calculateTitleSimilarity(t1, t2);
        assertTrue(similarity >= 0.70, "Similarity score was " + similarity);

        double exactSim = deduplicationService.calculateTitleSimilarity(t1, t1);
        assertEquals(1.0, exactSim, 0.001);

        double differentSim = deduplicationService.calculateTitleSimilarity(
                "Anthropic announces Claude 3.7",
                "Ferrari unveils new solid state hybrid supercar"
        );
        assertEquals(0.0, differentSim, 0.01);
    }

    @Test
    void deduplicateAndPersist_handlesExactUrlMatch() {
        Article existing = new Article();
        existing.setId(UUID.randomUUID());
        existing.setCanonicalUrl("https://tech.org/posts/claude-37");
        existing.setContentHash("hash123");
        existing.setTitle("Claude 3.7 Announced");

        when(articleRepository.findByCanonicalUrl("https://tech.org/posts/claude-37"))
                .thenReturn(Optional.of(existing));

        Article candidate = new Article();
        candidate.setCanonicalUrl("https://tech.org/posts/claude-37");
        candidate.setContentHash("differentHash");
        candidate.setTitle("Claude 3.7 Announced");

        var result = deduplicationService.deduplicateAndPersist(candidate, sampleFeed, "guid-1", List.of());

        assertNotNull(result);
        assertFalse(result.isNew());
        assertTrue(result.isDuplicate());
        assertEquals("EXACT_URL_MATCH", result.duplicateReason());
        assertEquals(existing.getId(), result.article().getId());
        verify(articleRepository, never()).save(candidate);
    }

    @Test
    void deduplicateAndPersist_handlesExactHashMatch() {
        Article existing = new Article();
        existing.setId(UUID.randomUUID());
        existing.setCanonicalUrl("https://syndicateA.com/story");
        existing.setContentHash("same-hash-456");
        existing.setTitle("Tech Story");

        when(articleRepository.findByCanonicalUrl(any())).thenReturn(Optional.empty());
        when(articleRepository.findByContentHash("same-hash-456")).thenReturn(Optional.of(existing));

        Article candidate = new Article();
        candidate.setCanonicalUrl("https://syndicateB.com/story");
        candidate.setContentHash("same-hash-456");
        candidate.setTitle("Tech Story");

        var result = deduplicationService.deduplicateAndPersist(candidate, sampleFeed, "guid-2", List.of());

        assertNotNull(result);
        assertFalse(result.isNew());
        assertTrue(result.isDuplicate());
        assertEquals("EXACT_HASH_MATCH", result.duplicateReason());
    }

    @Test
    void deduplicateAndPersist_savesNewUniqueArticle() {
        when(articleRepository.findByCanonicalUrl(any())).thenReturn(Optional.empty());
        when(articleRepository.findByContentHash(any())).thenReturn(Optional.empty());
        when(articleRepository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));

        Article candidate = new Article();
        candidate.setCanonicalUrl("https://newsite.com/original-article");
        candidate.setContentHash("unique-hash-789");
        candidate.setTitle("Unique Quantum Computing Advance");
        candidate.setPublishedAt(OffsetDateTime.now());

        var result = deduplicationService.deduplicateAndPersist(candidate, sampleFeed, "guid-3", List.of());

        assertNotNull(result);
        assertTrue(result.isNew());
        assertFalse(result.isDuplicate());
        assertEquals("ELIGIBLE", result.article().getProcessingStatus());
        verify(articleRepository).save(candidate);
    }
}
