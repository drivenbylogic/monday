package com.monday.app.intelligence.service;

import com.monday.app.intelligence.dto.RawRssFeed;
import com.monday.app.intelligence.dto.RawRssItem;
import com.monday.app.intelligence.entity.*;
import com.monday.app.intelligence.ingestion.ArticleNormalizer;
import com.monday.app.intelligence.ingestion.FeedFetcher;
import com.monday.app.intelligence.ingestion.RssParser;
import com.monday.app.intelligence.repository.ArticleClassificationRepository;
import com.monday.app.intelligence.repository.IntelligenceRunRepository;
import com.monday.app.intelligence.repository.InterestCategoryRepository;
import com.monday.app.intelligence.repository.UserInterestCategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class IntelligencePipelineService {

    private static final Logger log = LoggerFactory.getLogger(IntelligencePipelineService.class);

    private final IntelligenceRunRepository runRepository;
    private final FeedManagementService feedService;
    private final FeedFetcher feedFetcher;
    private final RssParser rssParser;
    private final ArticleNormalizer articleNormalizer;
    private final ArticleDeduplicationService deduplicationService;
    private final ArticleClassifier articleClassifier;
    private final ArticleRankingService rankingService;
    private final DailySelectionService selectionService;
    private final IntelligenceSummaryService summaryService;
    private final InterestCategoryRepository categoryRepository;
    private final UserInterestCategoryRepository userInterestCategoryRepository;
    private final ArticleClassificationRepository classificationRepository;

    public IntelligencePipelineService(IntelligenceRunRepository runRepository,
                                       FeedManagementService feedService,
                                       FeedFetcher feedFetcher,
                                       RssParser rssParser,
                                       ArticleNormalizer articleNormalizer,
                                       ArticleDeduplicationService deduplicationService,
                                       ArticleClassifier articleClassifier,
                                       ArticleRankingService rankingService,
                                       DailySelectionService selectionService,
                                       IntelligenceSummaryService summaryService,
                                       InterestCategoryRepository categoryRepository,
                                       UserInterestCategoryRepository userInterestCategoryRepository,
                                       ArticleClassificationRepository classificationRepository) {
        this.runRepository = runRepository;
        this.feedService = feedService;
        this.feedFetcher = feedFetcher;
        this.rssParser = rssParser;
        this.articleNormalizer = articleNormalizer;
        this.deduplicationService = deduplicationService;
        this.articleClassifier = articleClassifier;
        this.rankingService = rankingService;
        this.selectionService = selectionService;
        this.summaryService = summaryService;
        this.categoryRepository = categoryRepository;
        this.userInterestCategoryRepository = userInterestCategoryRepository;
        this.classificationRepository = classificationRepository;
    }

    public void runDaily() {
        IntelligenceRun run = new IntelligenceRun();
        run.setRunDate(LocalDate.now());
        run.setStartedAt(OffsetDateTime.now());
        run.setStatus("RUNNING");
        run = runRepository.save(run);

        try {
            List<RssFeed> activeFeeds = feedService.getAllFeeds().stream()
                    .filter(f -> "ACTIVE".equals(f.getStatus()))
                    .toList();

            run.setFeedsAttempted(activeFeeds.size());
            
            List<Article> newArticlesThisRun = new ArrayList<>();

            for (RssFeed feed : activeFeeds) {
                try {
                    String xml = feedFetcher.fetch(feed.getFeedUrl());
                    RawRssFeed parsedFeed = rssParser.parse(xml);
                    
                    for (RawRssItem rawItem : parsedFeed.items()) {
                        run.setArticlesSeen(run.getArticlesSeen() + 1);
                        Article normalized = articleNormalizer.normalize(rawItem);
                        
                        // Simple check to prevent re-classifying deduplicated articles inside this run scope
                        Article resolved = deduplicationService.processAndDeduplicate(normalized, feed, rawItem.guid());
                        if (resolved.getCreatedAt().isAfter(run.getStartedAt().minusMinutes(5))) {
                            // Approximating 'new' for this run. Proper implementation would check if it was newly saved.
                            if (!newArticlesThisRun.contains(resolved)) {
                                newArticlesThisRun.add(resolved);
                                run.setArticlesNew(run.getArticlesNew() + 1);
                            }
                        } else {
                            run.setArticlesDeduplicated(run.getArticlesDeduplicated() + 1);
                        }
                    }
                    
                    feedService.updateFetchMetadata(feed.getId(), true, null, null);
                    run.setFeedsSucceeded(run.getFeedsSucceeded() + 1);
                } catch (Exception e) {
                    log.error("Failed to process feed {}", feed.getFeedUrl(), e);
                    feedService.updateFetchMetadata(feed.getId(), false, "FETCH_ERROR", e.getMessage());
                    run.setFeedsFailed(run.getFeedsFailed() + 1);
                }
            }

            // Classify new articles
            for (Article article : newArticlesThisRun) {
                articleClassifier.classifyAndPersist(article);
                run.setArticlesClassified(run.getArticlesClassified() + 1);
            }

            // Interest filtering & Ranking & Selection
            // For now, assuming user interest is just all categories to simulate for single user
            // In reality we would fetch subscriptions for the current user.
            List<InterestCategory> categories = categoryRepository.findAll();
            OffsetDateTime twentyFourHoursAgo = OffsetDateTime.now().minusHours(24);
            
            for (InterestCategory category : categories) {
                List<ArticleClassification> recentClassifications = classificationRepository
                        .findByCategoryIdAndClassifiedAtGreaterThanEqual(category.getId(), twentyFourHoursAgo);
                
                List<DailySelectionService.RankedArticle> rankedForCategory = new ArrayList<>();
                for (ArticleClassification classification : recentClassifications) {
                    double score = rankingService.calculateRankingScore(classification, 0.5); // Default source quality
                    rankedForCategory.add(new DailySelectionService.RankedArticle(classification, score));
                }
                
                List<DailyArticleSelection> selections = selectionService.selectTopArticles(category, rankedForCategory, run.getRunDate());
                run.setArticlesSelected(run.getArticlesSelected() + selections.size());
                
                for (DailyArticleSelection selection : selections) {
                    ArticleSummary summary = summaryService.generateSummary(selection);
                    if ("SUCCESS".equals(summary.getStatus())) {
                        run.setSummariesGenerated(run.getSummariesGenerated() + 1);
                    } else {
                        run.setSummariesFailed(run.getSummariesFailed() + 1);
                    }
                }
            }
            
            run.setStatus(run.getFeedsFailed() > 0 ? "PARTIAL" : "COMPLETED");
        } catch (Exception e) {
            log.error("Pipeline failed", e);
            run.setStatus("FAILED");
            run.setFailureMessage(e.getMessage());
        } finally {
            run.setCompletedAt(OffsetDateTime.now());
            runRepository.save(run);
        }
    }
}
