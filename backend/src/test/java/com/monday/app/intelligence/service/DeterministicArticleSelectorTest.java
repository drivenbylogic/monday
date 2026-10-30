package com.monday.app.intelligence.service;

import com.monday.app.intelligence.entity.Article;
import com.monday.app.intelligence.entity.ArticleSummary;
import com.monday.app.intelligence.entity.RssFeed;
import com.monday.app.intelligence.repository.ArticleSummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeterministicArticleSelectorTest {

    private ArticleSummaryRepository summaryRepository;
    private DeterministicArticleSelector selector;
    private RssFeed node;

    @BeforeEach
    void setUp() {
        summaryRepository = mock(ArticleSummaryRepository.class);
        selector = new DeterministicArticleSelector(summaryRepository);

        node = new RssFeed();
        node.setId(UUID.randomUUID());
        node.setName("AI Research Feed");
        node.setArticleLimit(3); // limit to 3 for test
    }

    @Test
    void selectEligibleArticlesForNode_capsAtLimitAndExcludesDuplicates() {
        OffsetDateTime now = OffsetDateTime.now();
        List<Article> candidates = new ArrayList<>();

        for (int i = 1; i <= 6; i++) {
            Article a = new Article();
            a.setId(UUID.randomUUID());
            a.setTitle("AI Article " + i);
            a.setCanonicalUrl("https://ai.org/article-" + i);
            a.setContentText("Substantial article content describing breakthroughs in model intelligence " + i);
            a.setPublishedAt(now.minusHours(i));
            a.setFirstSeenAt(now.minusHours(i));
            candidates.add(a);
        }

        // Mark article 2 as duplicate
        candidates.get(1).setDuplicateOfId(candidates.get(0).getId());

        when(summaryRepository.findFirstByArticleIdAndStatusOrderByCreatedAtDesc(any(), eq("SUCCESS")))
                .thenReturn(Optional.empty());

        List<Article> selected = selector.selectEligibleArticlesForNode(node, candidates);

        assertNotNull(selected);
        assertEquals(3, selected.size(), "Should be capped at node limit of 3");
        assertFalse(selected.contains(candidates.get(1)), "Duplicate article must be excluded");
        // Verify newest first
        assertEquals("AI Article 1", selected.get(0).getTitle());
    }

    @Test
    void selectEligibleArticlesForNode_excludesAlreadySummarizedArticles() {
        OffsetDateTime now = OffsetDateTime.now();
        Article a1 = new Article();
        a1.setId(UUID.randomUUID());
        a1.setTitle("Already Summarized");
        a1.setCanonicalUrl("https://ai.org/1");
        a1.setContentText("Substantial body text for article 1 with details.");
        a1.setPublishedAt(now.minusHours(2));

        Article a2 = new Article();
        a2.setId(UUID.randomUUID());
        a2.setTitle("Fresh Article Needs Summary");
        a2.setCanonicalUrl("https://ai.org/2");
        a2.setContentText("Substantial body text for article 2 with details.");
        a2.setPublishedAt(now.minusHours(3));

        ArticleSummary existingSummary = new ArticleSummary();
        existingSummary.setId(UUID.randomUUID());
        existingSummary.setStatus("SUCCESS");

        when(summaryRepository.findFirstByArticleIdAndStatusOrderByCreatedAtDesc(a1.getId(), "SUCCESS"))
                .thenReturn(Optional.of(existingSummary));
        when(summaryRepository.findFirstByArticleIdAndStatusOrderByCreatedAtDesc(a2.getId(), "SUCCESS"))
                .thenReturn(Optional.empty());

        List<Article> selected = selector.selectEligibleArticlesForNode(node, List.of(a1, a2));

        assertEquals(1, selected.size());
        assertEquals("Fresh Article Needs Summary", selected.get(0).getTitle());
    }

    @Test
    void selectEligibleArticlesForNode_excludesArticlesOlderThanLookback() {
        OffsetDateTime now = OffsetDateTime.now();
        Article stale = new Article();
        stale.setId(UUID.randomUUID());
        stale.setTitle("Very Old Article");
        stale.setCanonicalUrl("https://ai.org/old");
        stale.setContentText("Stale content from 20 days ago.");
        stale.setPublishedAt(now.minusDays(20));

        when(summaryRepository.findFirstByArticleIdAndStatusOrderByCreatedAtDesc(any(), eq("SUCCESS")))
                .thenReturn(Optional.empty());

        List<Article> selected = selector.selectEligibleArticlesForNode(node, List.of(stale));
        assertTrue(selected.isEmpty(), "Articles outside lookback window must be excluded");
    }
}
