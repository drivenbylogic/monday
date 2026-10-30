package com.monday.app.intelligence.service;

import com.monday.app.intelligence.entity.Article;
import com.monday.app.intelligence.entity.ArticleSource;
import com.monday.app.intelligence.entity.ArticleSourceId;
import com.monday.app.intelligence.entity.RssFeed;
import com.monday.app.intelligence.repository.ArticleRepository;
import com.monday.app.intelligence.repository.ArticleSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ArticleDeduplicationService {

    private static final Logger log = LoggerFactory.getLogger(ArticleDeduplicationService.class);

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are", "as", "at",
            "be", "because", "been", "before", "being", "below", "between", "both", "but", "by",
            "could", "did", "do", "does", "doing", "down", "during",
            "each", "few", "for", "from", "further",
            "had", "has", "have", "having", "he", "her", "here", "hers", "herself", "him", "himself", "his", "how",
            "i", "if", "in", "into", "is", "it", "its", "itself",
            "just", "me", "more", "most", "my", "myself",
            "no", "nor", "not", "now", "of", "off", "on", "once", "only", "or", "other", "our", "ours", "ourselves", "out", "over", "own",
            "same", "she", "should", "so", "some", "such",
            "than", "that", "the", "their", "theirs", "them", "themselves", "then", "there", "these", "they", "this", "those", "through", "to", "too",
            "under", "until", "up", "very", "was", "we", "were", "what", "when", "where", "which", "while", "who", "whom", "why", "with", "would",
            "you", "your", "yours", "yourself", "yourselves"
    );

    private final ArticleRepository articleRepository;
    private final ArticleSourceRepository articleSourceRepository;

    @Value("${intelligence.dedup.similarity-threshold:0.82}")
    private double similarityThreshold = 0.82;

    public ArticleDeduplicationService(ArticleRepository articleRepository, ArticleSourceRepository articleSourceRepository) {
        this.articleRepository = articleRepository;
        this.articleSourceRepository = articleSourceRepository;
    }

    public record DeduplicationResult(
            Article article,
            boolean isNew,
            boolean isDuplicate,
            String duplicateReason
    ) {}

    /**
     * Executes two-stage deduplication:
     * 1. Exact match on canonical URL or content hash.
     * 2. Deterministic title similarity match against recent articles.
     */
    public DeduplicationResult deduplicateAndPersist(Article candidate, RssFeed feed, String externalGuid, List<Article> recentArticlesPool) {
        if (candidate == null) return null;

        // Stage 1: Exact Match (Canonical URL or Content Hash)
        Optional<Article> exactUrlMatch = articleRepository.findByCanonicalUrl(candidate.getCanonicalUrl());
        if (exactUrlMatch.isPresent()) {
            Article existing = exactUrlMatch.get();
            recordSourceAttribution(existing, feed, externalGuid, candidate.getPublishedAt());
            existing.setLastSeenAt(OffsetDateTime.now());
            articleRepository.save(existing);
            return new DeduplicationResult(existing, false, true, "EXACT_URL_MATCH");
        }

        Optional<Article> exactHashMatch = articleRepository.findByContentHash(candidate.getContentHash());
        if (exactHashMatch.isPresent()) {
            Article existing = exactHashMatch.get();
            recordSourceAttribution(existing, feed, externalGuid, candidate.getPublishedAt());
            existing.setLastSeenAt(OffsetDateTime.now());
            articleRepository.save(existing);
            return new DeduplicationResult(existing, false, true, "EXACT_HASH_MATCH");
        }

        // Stage 2: Second-stage deterministic title-similarity matching against recent pool
        if (recentArticlesPool != null) {
            for (Article existing : recentArticlesPool) {
                if (existing.getId() == null || existing.getId().equals(candidate.getId())) continue;

                // Time proximity check: within 72 hours
                if (existing.getPublishedAt() != null && candidate.getPublishedAt() != null) {
                    long hours = Math.abs(Duration.between(existing.getPublishedAt(), candidate.getPublishedAt()).toHours());
                    if (hours > 72) continue;
                }

                double similarity = calculateTitleSimilarity(candidate.getTitle(), existing.getTitle());
                if (similarity >= similarityThreshold) {
                    log.info("Title similarity duplicate detected (score: {}): '{}' matches '{}'",
                            similarity, candidate.getTitle(), existing.getTitle());

                    // Mark candidate as duplicate of existing, but still persist candidate record for lineage
                    candidate.setDuplicateOfId(existing.getId());
                    candidate.setProcessingStatus("DEDUPLICATED");
                    Article savedCandidate = articleRepository.save(candidate);

                    // Preserve source attribution on candidate and on canonical existing
                    recordSourceAttribution(savedCandidate, feed, externalGuid, candidate.getPublishedAt());
                    recordSourceAttribution(existing, feed, externalGuid, candidate.getPublishedAt());

                    return new DeduplicationResult(existing, false, true, "TITLE_SIMILARITY_MATCH (" + String.format("%.2f", similarity) + ")");
                }
            }
        }

        // Article is unique and newly discovered
        candidate.setProcessingStatus("ELIGIBLE");
        Article saved = articleRepository.save(candidate);
        recordSourceAttribution(saved, feed, externalGuid, candidate.getPublishedAt());

        return new DeduplicationResult(saved, true, false, null);
    }

    public Article processAndDeduplicate(Article normalizedArticle, RssFeed feed, String externalGuid) {
        DeduplicationResult result = deduplicateAndPersist(normalizedArticle, feed, externalGuid, null);
        return result != null ? result.article() : null;
    }

    public double calculateTitleSimilarity(String titleA, String titleB) {
        if (titleA == null || titleB == null) return 0.0;
        if (titleA.equalsIgnoreCase(titleB)) return 1.0;

        Set<String> tokensA = tokenizeAndCleanTitle(titleA);
        Set<String> tokensB = tokenizeAndCleanTitle(titleB);

        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(tokensA);
        intersection.retainAll(tokensB);

        Set<String> union = new HashSet<>(tokensA);
        union.addAll(tokensB);

        return (double) intersection.size() / (double) union.size();
    }

    private Set<String> tokenizeAndCleanTitle(String title) {
        String cleaned = title.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
        String[] words = cleaned.split("\\s+");
        return Arrays.stream(words)
                .map(String::trim)
                .filter(w -> w.length() > 2 && !STOP_WORDS.contains(w))
                .collect(Collectors.toSet());
    }

    private void recordSourceAttribution(Article article, RssFeed feed, String externalGuid, OffsetDateTime publishedAt) {
        if (article == null || feed == null) return;
        ArticleSourceId sourceId = new ArticleSourceId(article.getId(), feed.getId());
        if (!articleSourceRepository.existsById(sourceId)) {
            ArticleSource source = new ArticleSource();
            source.setId(sourceId);
            source.setArticle(article);
            source.setFeed(feed);
            source.setExternalGuid(externalGuid);
            source.setSourcePublishedAt(publishedAt);
            source.setFirstSeenAt(OffsetDateTime.now());
            articleSourceRepository.save(source);
        }
    }
}
