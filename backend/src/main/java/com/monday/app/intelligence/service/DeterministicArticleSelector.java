package com.monday.app.intelligence.service;

import com.monday.app.intelligence.entity.Article;
import com.monday.app.intelligence.entity.RssFeed;
import com.monday.app.intelligence.repository.ArticleSummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DeterministicArticleSelector {

    private static final Logger log = LoggerFactory.getLogger(DeterministicArticleSelector.class);

    private final ArticleSummaryRepository summaryRepository;

    @Value("${intelligence.cost.max-articles-per-node:5}")
    private int maxArticlesPerNode = 5;

    @Value("${intelligence.retention.days:7}")
    private int lookbackDays = 7;

    public DeterministicArticleSelector(ArticleSummaryRepository summaryRepository) {
        this.summaryRepository = summaryRepository;
    }

    /**
     * Selects up to node.getArticleLimit() eligible unique articles per node.
     * Enforces deterministic selection rules without fabricating any data.
     */
    public List<Article> selectEligibleArticlesForNode(RssFeed node, List<Article> candidateArticles) {
        if (candidateArticles == null || candidateArticles.isEmpty()) {
            return Collections.emptyList();
        }

        int limit = node.getArticleLimit() != null && node.getArticleLimit() > 0
                ? Math.min(node.getArticleLimit(), maxArticlesPerNode)
                : maxArticlesPerNode;

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime lookbackCutoff = now.minusDays(lookbackDays);

        List<Article> eligible = candidateArticles.stream()
                // Rule 0: Exclude deleted articles
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                // Rule 1: Exclude duplicates
                .filter(a -> a.getDuplicateOfId() == null && !"DEDUPLICATED".equalsIgnoreCase(a.getProcessingStatus()))
                // Rule 2: Exclude articles without sufficient usable content (< 30 characters)
                .filter(a -> a.getContentText() != null && a.getContentText().trim().length() >= 30)
                // Rule 3: Exclude articles outside the lookback window
                .filter(a -> {
                    OffsetDateTime effectiveDate = a.getPublishedAt() != null ? a.getPublishedAt() : a.getFirstSeenAt();
                    return effectiveDate != null && effectiveDate.isAfter(lookbackCutoff);
                })
                // Rule 4: Exclude articles that already have a successful summary within retention window
                .filter(a -> {
                    boolean alreadySummarized = summaryRepository.findFirstByArticleIdAndStatusOrderByCreatedAtDesc(a.getId(), "SUCCESS").isPresent();
                    return !alreadySummarized;
                })
                // Rule 5: Deterministic ordering: Prefer newer eligible articles, break ties deterministically
                .sorted((a1, a2) -> {
                    OffsetDateTime t1 = a1.getPublishedAt() != null ? a1.getPublishedAt() : a1.getFirstSeenAt();
                    OffsetDateTime t2 = a2.getPublishedAt() != null ? a2.getPublishedAt() : a2.getFirstSeenAt();
                    int cmp = t2.compareTo(t1); // descending (newer first)
                    if (cmp != 0) return cmp;
                    return a1.getCanonicalUrl().compareTo(a2.getCanonicalUrl()); // deterministic tie-break
                })
                .limit(limit)
                .collect(Collectors.toList());

        log.info("Selected {} eligible articles for node '{}' (configured limit: {})",
                eligible.size(), node.getName(), limit);

        return eligible;
    }
}
