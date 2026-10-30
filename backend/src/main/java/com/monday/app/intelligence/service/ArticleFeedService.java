package com.monday.app.intelligence.service;

import com.monday.app.intelligence.dto.ArticleFeedItemDto;
import com.monday.app.intelligence.entity.Article;
import com.monday.app.intelligence.entity.ArticleSource;
import com.monday.app.intelligence.entity.ArticleSummary;
import com.monday.app.intelligence.repository.ArticleRepository;
import com.monday.app.intelligence.repository.ArticleSourceRepository;
import com.monday.app.intelligence.repository.ArticleSummaryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ArticleFeedService {

    private final ArticleRepository articleRepository;
    private final ArticleSummaryRepository summaryRepository;
    private final ArticleSourceRepository articleSourceRepository;

    public ArticleFeedService(ArticleRepository articleRepository,
                              ArticleSummaryRepository summaryRepository,
                              ArticleSourceRepository articleSourceRepository) {
        this.articleRepository = articleRepository;
        this.summaryRepository = summaryRepository;
        this.articleSourceRepository = articleSourceRepository;
    }

    @Transactional(readOnly = true)
    public Page<ArticleFeedItemDto> getArticles(String area, UUID feedId, Boolean bookmarked,
                                               String search, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)));

        String cleanArea = (area != null && !area.isBlank() && !"all".equalsIgnoreCase(area)) ? area.trim() : null;
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<Article> articlesPage = articleRepository.searchArticles(cleanArea, feedId, bookmarked, cleanSearch, pageable);

        List<UUID> articleIds = articlesPage.getContent().stream().map(Article::getId).collect(Collectors.toList());

        // Batch load summaries
        Map<UUID, ArticleSummary> summariesMap = new HashMap<>();
        if (!articleIds.isEmpty()) {
            for (UUID id : articleIds) {
                summaryRepository.findFirstByArticleIdAndStatusOrderByCreatedAtDesc(id, "SUCCESS")
                        .ifPresent(s -> summariesMap.put(id, s));
            }
        }

        // Batch load sources
        Map<UUID, ArticleSource> sourcesMap = new HashMap<>();
        if (!articleIds.isEmpty()) {
            List<ArticleSource> sources = articleSourceRepository.findByIdArticleIdIn(articleIds);
            for (ArticleSource src : sources) {
                sourcesMap.putIfAbsent(src.getArticle().getId(), src);
            }
        }

        List<ArticleFeedItemDto> dtoList = articlesPage.getContent().stream()
                .map(article -> {
                    ArticleSummary summary = summariesMap.get(article.getId());
                    ArticleSource source = sourcesMap.get(article.getId());

                    String sourceName = source != null && source.getFeed() != null
                            ? source.getFeed().getName()
                            : (article.getAuthor() != null ? article.getAuthor() : "Curated Source");
                    UUID nodeFeedId = source != null && source.getFeed() != null ? source.getFeed().getId() : null;

                    String summaryText = summary != null && summary.getSummary() != null
                            ? summary.getSummary()
                            : (article.getDescription() != null ? article.getDescription() : "Summary awaiting generation.");

                    return new ArticleFeedItemDto(
                            article.getId(),
                            article.getTitle(),
                            summaryText,
                            article.getOriginalUrl(),
                            article.getCanonicalUrl(),
                            article.getAuthor(),
                            sourceName,
                            nodeFeedId,
                            article.getAreaOfInterest(),
                            article.getPublishedAt(),
                            article.getFirstSeenAt(),
                            summary != null ? summary.getGeneratedAt() : null,
                            summary != null ? summary.getModel() : "gpt-4o-mini",
                            summary != null ? summary.getWordCount() : null,
                            summary != null ? summary.getTokensUsed() : null,
                            summary != null ? summary.getEstimatedCost() : null,
                            Boolean.TRUE.equals(article.getBookmarked()),
                            Boolean.TRUE.equals(article.getIsRead()),
                            article.getProcessingStatus()
                    );
                })
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, articlesPage.getTotalElements());
    }

    public ArticleFeedItemDto toggleBookmark(UUID articleId, boolean bookmarked) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("Article not found: " + articleId));

        article.setBookmarked(bookmarked);
        article.setBookmarkedAt(bookmarked ? OffsetDateTime.now() : null);
        article.setUpdatedAt(OffsetDateTime.now());
        Article saved = articleRepository.save(article);

        Optional<ArticleSummary> summary = summaryRepository.findFirstByArticleIdAndStatusOrderByCreatedAtDesc(saved.getId(), "SUCCESS");
        List<ArticleSource> sources = articleSourceRepository.findByIdArticleId(saved.getId());
        String sourceName = !sources.isEmpty() && sources.get(0).getFeed() != null
                ? sources.get(0).getFeed().getName()
                : saved.getAuthor();
        UUID feedId = !sources.isEmpty() && sources.get(0).getFeed() != null ? sources.get(0).getFeed().getId() : null;

        return new ArticleFeedItemDto(
                saved.getId(),
                saved.getTitle(),
                summary.map(ArticleSummary::getSummary).orElse(saved.getDescription()),
                saved.getOriginalUrl(),
                saved.getCanonicalUrl(),
                saved.getAuthor(),
                sourceName,
                feedId,
                saved.getAreaOfInterest(),
                saved.getPublishedAt(),
                saved.getFirstSeenAt(),
                summary.map(ArticleSummary::getGeneratedAt).orElse(null),
                summary.map(ArticleSummary::getModel).orElse(null),
                summary.map(ArticleSummary::getWordCount).orElse(null),
                summary.map(ArticleSummary::getTokensUsed).orElse(null),
                summary.map(ArticleSummary::getEstimatedCost).orElse(null),
                saved.getBookmarked(),
                saved.getIsRead(),
                saved.getProcessingStatus()
        );
    }

    public ArticleFeedItemDto toggleRead(UUID articleId, boolean isRead) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("Article not found: " + articleId));

        article.setIsRead(isRead);
        article.setReadAt(isRead ? OffsetDateTime.now() : null);
        article.setUpdatedAt(OffsetDateTime.now());
        Article saved = articleRepository.save(article);

        Optional<ArticleSummary> summary = summaryRepository.findFirstByArticleIdAndStatusOrderByCreatedAtDesc(saved.getId(), "SUCCESS");
        List<ArticleSource> sources = articleSourceRepository.findByIdArticleId(saved.getId());
        String sourceName = !sources.isEmpty() && sources.get(0).getFeed() != null
                ? sources.get(0).getFeed().getName()
                : saved.getAuthor();
        UUID feedId = !sources.isEmpty() && sources.get(0).getFeed() != null ? sources.get(0).getFeed().getId() : null;

        return new ArticleFeedItemDto(
                saved.getId(),
                saved.getTitle(),
                summary.map(ArticleSummary::getSummary).orElse(saved.getDescription()),
                saved.getOriginalUrl(),
                saved.getCanonicalUrl(),
                saved.getAuthor(),
                sourceName,
                feedId,
                saved.getAreaOfInterest(),
                saved.getPublishedAt(),
                saved.getFirstSeenAt(),
                summary.map(ArticleSummary::getGeneratedAt).orElse(null),
                summary.map(ArticleSummary::getModel).orElse(null),
                summary.map(ArticleSummary::getWordCount).orElse(null),
                summary.map(ArticleSummary::getTokensUsed).orElse(null),
                summary.map(ArticleSummary::getEstimatedCost).orElse(null),
                saved.getBookmarked(),
                saved.getIsRead(),
                saved.getProcessingStatus()
        );
    }

    public void deleteArticle(UUID articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("Article not found: " + articleId));

        article.setIsDeleted(true);
        article.setDeletedAt(OffsetDateTime.now());
        article.setUpdatedAt(OffsetDateTime.now());
        articleRepository.save(article);
    }
}

