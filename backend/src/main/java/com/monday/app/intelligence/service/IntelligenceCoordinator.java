package com.monday.app.intelligence.service;

import com.monday.app.intelligence.dto.*;
import com.monday.app.intelligence.entity.*;
import com.monday.app.intelligence.ingestion.ArticleNormalizer;
import com.monday.app.intelligence.ingestion.FeedFetcher;
import com.monday.app.intelligence.ingestion.RssParser;
import com.monday.app.intelligence.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class IntelligenceCoordinator {

    private static final Logger log = LoggerFactory.getLogger(IntelligenceCoordinator.class);

    private final IntelligenceRunRepository runRepository;
    private final RssFeedRepository feedRepository;
    private final ArticleRepository articleRepository;
    private final ArticleSummaryRepository summaryRepository;
    private final ArticleSourceRepository articleSourceRepository;
    private final FeedManagementService feedService;
    private final FeedFetcher feedFetcher;
    private final RssParser rssParser;
    private final ArticleNormalizer articleNormalizer;
    private final ArticleDeduplicationService deduplicationService;
    private final DeterministicArticleSelector articleSelector;
    private final IntelligenceSummaryService summaryService;

    @Value("${intelligence.cooldown-hours:6}")
    private long cooldownHours = 6;

    @Value("${intelligence.retention.days:7}")
    private int retentionDays = 7;

    // Atomic in-memory concurrency guard across threads/requests
    private final AtomicBoolean isExecutionActive = new AtomicBoolean(false);

    public IntelligenceCoordinator(IntelligenceRunRepository runRepository,
                                   RssFeedRepository feedRepository,
                                   ArticleRepository articleRepository,
                                   ArticleSummaryRepository summaryRepository,
                                   ArticleSourceRepository articleSourceRepository,
                                   FeedManagementService feedService,
                                   FeedFetcher feedFetcher,
                                   RssParser rssParser,
                                   ArticleNormalizer articleNormalizer,
                                   ArticleDeduplicationService deduplicationService,
                                   DeterministicArticleSelector articleSelector,
                                   IntelligenceSummaryService summaryService) {
        this.runRepository = runRepository;
        this.feedRepository = feedRepository;
        this.articleRepository = articleRepository;
        this.summaryRepository = summaryRepository;
        this.articleSourceRepository = articleSourceRepository;
        this.feedService = feedService;
        this.feedFetcher = feedFetcher;
        this.rssParser = rssParser;
        this.articleNormalizer = articleNormalizer;
        this.deduplicationService = deduplicationService;
        this.articleSelector = articleSelector;
        this.summaryService = summaryService;
    }

    public record ExecutionOutcome(
            boolean runExecuted,
            String status,
            String message,
            IntelligenceRun run,
            long cooldownSecondsRemaining,
            OffsetDateTime nextAllowedAt
    ) {}

    /**
     * Checks the 6-hour generation cooldown status.
     */
    public IntelligenceStatusDto getStatus() {
        Optional<IntelligenceRun> lastRunOpt = runRepository.findFirstByOrderByStartedAtDesc();
        OffsetDateTime now = OffsetDateTime.now();
        boolean running = isExecutionActive.get();

        OffsetDateTime lastGeneratedAt = null;
        OffsetDateTime nextAllowedAt = null;
        long cooldownSecondsRemaining = 0;
        boolean canGenerate = true;
        String lastRunStatus = "IDLE";
        Long durationMs = 0L;
        Integer tokensUsed = 0;
        BigDecimal estimatedCost = BigDecimal.ZERO;

        if (lastRunOpt.isPresent()) {
            IntelligenceRun lastRun = lastRunOpt.get();
            lastGeneratedAt = lastRun.getStartedAt();
            lastRunStatus = lastRun.getStatus();
            durationMs = lastRun.getDurationMs();
            tokensUsed = lastRun.getTokensUsed();
            estimatedCost = lastRun.getEstimatedCost();

            Duration elapsed = Duration.between(lastGeneratedAt, now);
            long cooldownTotalSeconds = cooldownHours * 3600;

            if (elapsed.getSeconds() < cooldownTotalSeconds) {
                cooldownSecondsRemaining = cooldownTotalSeconds - elapsed.getSeconds();
                nextAllowedAt = lastGeneratedAt.plusSeconds(cooldownTotalSeconds);
                canGenerate = false;
            } else {
                nextAllowedAt = now;
                canGenerate = !running;
            }
        }

        if (running) {
            canGenerate = false;
        }

        int activeNodes = feedRepository.findByIsEnabledTrue().size();
        long totalArticles = articleRepository.count();

        return new IntelligenceStatusDto(
                canGenerate,
                running,
                lastGeneratedAt,
                nextAllowedAt,
                Math.max(0, cooldownSecondsRemaining),
                activeNodes,
                totalArticles,
                retentionDays,
                lastRunStatus,
                durationMs,
                tokensUsed,
                estimatedCost
        );
    }

    /**
     * Triggers a manual generation run from the UI.
     * Enforces the 6-hour cooldown: if last generation occurred < 6 hours ago,
     * it blocks the generation and returns cooldown status.
     */
    public ExecutionOutcome triggerManualGeneration() {
        IntelligenceStatusDto status = getStatus();
        if (!status.canGenerate()) {
            String reason = status.isRunning()
                    ? "Pipeline is currently executing another run."
                    : "Cooldown active: manual regeneration is only allowed every 6 hours.";
            log.info("Manual generation request rejected. {}", reason);
            return new ExecutionOutcome(
                    false,
                    status.isRunning() ? "RUNNING" : "COOLDOWN_ACTIVE",
                    reason,
                    null,
                    status.cooldownSecondsRemaining(),
                    status.nextAllowedAt()
            );
        }

        return executePipeline("MANUAL");
    }

    /**
     * Executes the daily scheduled generation run (scheduled at 6:00 AM IST).
     */
    public ExecutionOutcome triggerScheduledGeneration() {
        log.info("Starting scheduled intelligence generation pipeline run");
        return executePipeline("SCHEDULED");
    }

    /**
     * Orchestrates the 5-stage intelligence pipeline atomically.
     */
    public synchronized ExecutionOutcome executePipeline(String triggerType) {
        if (!isExecutionActive.compareAndSet(false, true)) {
            log.warn("Attempted to start pipeline while an execution is already running");
            IntelligenceStatusDto status = getStatus();
            return new ExecutionOutcome(false, "ALREADY_RUNNING", "Another pipeline run is currently active",
                    null, status.cooldownSecondsRemaining(), status.nextAllowedAt());
        }

        OffsetDateTime startedAt = OffsetDateTime.now();
        IntelligenceRun run = new IntelligenceRun();
        run.setRunDate(LocalDate.now());
        run.setStartedAt(startedAt);
        run.setStatus("RUNNING");
        run.setTriggerType(triggerType);
        run = runRepository.save(run);

        int cumulativeTokens = 0;
        BigDecimal cumulativeCost = BigDecimal.ZERO;
        int cumulativeSummaries = 0;
        boolean budgetLimited = false;

        try {
            List<RssFeed> enabledFeeds = feedRepository.findByIsEnabledTrue();
            run.setFeedsAttempted(enabledFeeds.size());

            OffsetDateTime poolCutoff = startedAt.minusDays(retentionDays);
            List<Article> recentPool = new ArrayList<>(articleRepository.findRecentArticles(poolCutoff));

            Map<RssFeed, List<Article>> selectedPerNode = new LinkedHashMap<>();

            // Stage 1 & 2 & 3 & 4: Fetch, Clean, Deduplicate
            for (RssFeed feed : enabledFeeds) {
                try {
                    feed.setLastAttemptedAt(OffsetDateTime.now());

                    FeedFetcher.FetchResult fetchResult = feedFetcher.fetchWithCache(
                            feed.getFeedUrl(), feed.getEtag(), feed.getLastModifiedHeader()
                    );

                    if (fetchResult.notModified()) {
                        log.info("Feed {} unchanged (304 Not Modified)", feed.getName());
                        feedService.updateFetchMetadata(
                                feed.getId(), true, "NOT_MODIFIED", null, null,
                                fetchResult.etag(), fetchResult.lastModified()
                        );
                        run.setFeedsSucceeded(run.getFeedsSucceeded() + 1);
                        continue;
                    }

                    RawRssFeed rawFeed = rssParser.parse(fetchResult.body());
                    List<Article> nodeCandidates = new ArrayList<>();

                    if (rawFeed.items() != null) {
                        for (RawRssItem rawItem : rawFeed.items()) {
                            run.setArticlesSeen(run.getArticlesSeen() + 1);

                            Article normalized = articleNormalizer.normalize(rawItem, feed.getAreaOfInterest());
                            if (normalized == null) continue;

                            ArticleDeduplicationService.DeduplicationResult dedupResult =
                                    deduplicationService.deduplicateAndPersist(normalized, feed, rawItem.guid(), recentPool);

                            if (dedupResult != null) {
                                if (dedupResult.isNew()) {
                                    run.setArticlesNew(run.getArticlesNew() + 1);
                                    recentPool.add(dedupResult.article());
                                } else if (dedupResult.isDuplicate()) {
                                    run.setArticlesDeduplicated(run.getArticlesDeduplicated() + 1);
                                }
                                nodeCandidates.add(dedupResult.article());
                            }
                        }
                    }

                    feedService.updateFetchMetadata(
                            feed.getId(), true, "SUCCESS", null, null,
                            fetchResult.etag(), fetchResult.lastModified()
                    );
                    run.setFeedsSucceeded(run.getFeedsSucceeded() + 1);

                    // Stage 5: Deterministic Selection for this node
                    List<Article> selected = articleSelector.selectEligibleArticlesForNode(feed, nodeCandidates);
                    selectedPerNode.put(feed, selected);
                    run.setArticlesSelected(run.getArticlesSelected() + selected.size());

                } catch (Exception e) {
                    log.error("Failed to process feed node '{}' ({}): {}", feed.getName(), feed.getFeedUrl(), e.getMessage());
                    feedService.updateFetchMetadata(
                            feed.getId(), false, "FETCH_ERROR", "ERROR", e.getMessage(), null, null
                    );
                    run.setFeedsFailed(run.getFeedsFailed() + 1);
                }
            }

            // Stage 6: AI Summarization with strict cost budget
            for (Map.Entry<RssFeed, List<Article>> entry : selectedPerNode.entrySet()) {
                for (Article article : entry.getValue()) {
                    // Check if already summarized within this run by a previous feed
                    Optional<ArticleSummary> existingSummary = summaryService.findExistingValidSummary(article);
                    if (existingSummary.isPresent()) {
                        run.setSummariesSkipped(run.getSummariesSkipped() + 1);
                        continue;
                    }

                    IntelligenceSummaryService.SummarizationResult summaryResult =
                            summaryService.summarizeArticle(article, cumulativeTokens, cumulativeSummaries);

                    if (summaryResult.budgetExceeded()) {
                        budgetLimited = true;
                        run.setSummariesDeferred(run.getSummariesDeferred() + 1);
                    } else if (summaryResult.summary() != null) {
                        if ("SUCCESS".equalsIgnoreCase(summaryResult.summary().getStatus())) {
                            if (summaryResult.isNewSummary()) {
                                run.setSummariesGenerated(run.getSummariesGenerated() + 1);
                                cumulativeSummaries++;
                                cumulativeTokens += summaryResult.tokensUsed();
                                cumulativeCost = cumulativeCost.add(summaryResult.estimatedCost());
                            } else {
                                run.setSummariesSkipped(run.getSummariesSkipped() + 1);
                            }
                        } else {
                            run.setSummariesFailed(run.getSummariesFailed() + 1);
                        }
                    }
                }
            }

            // Determine final run status
            if (budgetLimited) {
                run.setStatus("BUDGET_LIMITED");
            } else if (run.getFeedsFailed() > 0) {
                run.setStatus("PARTIAL");
            } else {
                run.setStatus("COMPLETED");
            }

        } catch (Exception e) {
            log.error("Intelligence pipeline encountered a fatal error", e);
            run.setStatus("FAILED");
            run.setFailureMessage(e.getMessage());
        } finally {
            OffsetDateTime completedAt = OffsetDateTime.now();
            run.setCompletedAt(completedAt);
            run.setDurationMs(Duration.between(startedAt, completedAt).toMillis());
            run.setTokensUsed(cumulativeTokens);
            run.setEstimatedCost(cumulativeCost);
            run = runRepository.save(run);
            isExecutionActive.set(false);
        }

        log.info("Intelligence pipeline finished with status {} in {} ms. Discovered: {}, Selected: {}, Summarized: {}, Cost: ${}",
                run.getStatus(), run.getDurationMs(), run.getArticlesSeen(), run.getArticlesSelected(),
                run.getSummariesGenerated(), run.getEstimatedCost());

        IntelligenceStatusDto newStatus = getStatus();
        return new ExecutionOutcome(
                true,
                run.getStatus(),
                "Pipeline completed with status " + run.getStatus(),
                run,
                newStatus.cooldownSecondsRemaining(),
                newStatus.nextAllowedAt()
        );
    }
}
