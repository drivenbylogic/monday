package com.monday.app.intelligence.service;

import com.monday.app.intelligence.dto.DashboardStatsDto;
import com.monday.app.intelligence.entity.Article;
import com.monday.app.intelligence.entity.IntelligenceRun;
import com.monday.app.intelligence.entity.RssFeed;
import com.monday.app.intelligence.repository.ArticleRepository;
import com.monday.app.intelligence.repository.ArticleSourceRepository;
import com.monday.app.intelligence.repository.IntelligenceRunRepository;
import com.monday.app.intelligence.repository.RssFeedRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DashboardStatsService {

    private final IntelligenceRunRepository runRepository;
    private final RssFeedRepository feedRepository;
    private final ArticleRepository articleRepository;
    private final ArticleSourceRepository articleSourceRepository;

    public DashboardStatsService(IntelligenceRunRepository runRepository,
                                 RssFeedRepository feedRepository,
                                 ArticleRepository articleRepository,
                                 ArticleSourceRepository articleSourceRepository) {
        this.runRepository = runRepository;
        this.feedRepository = feedRepository;
        this.articleRepository = articleRepository;
        this.articleSourceRepository = articleSourceRepository;
    }

    public DashboardStatsDto getDashboardStatistics(String period) {
        OffsetDateTime now = OffsetDateTime.now();
        int days = "today".equalsIgnoreCase(period) ? 1 : 7;
        OffsetDateTime startCutoff = now.minusDays(days);

        List<IntelligenceRun> runs = runRepository.findByStartedAtBetweenOrderByStartedAtDesc(startCutoff, now);
        List<RssFeed> allFeeds = feedRepository.findAll();
        List<Article> recentArticles = articleRepository.findRecentArticles(startCutoff);

        int activeNodes = (int) allFeeds.stream().filter(f -> Boolean.TRUE.equals(f.getIsEnabled())).count();

        long articlesDiscovered = 0;
        long articlesUnique = 0;
        long summariesGenerated = 0;
        long articlesDeferred = 0;
        long articlesFailed = 0;
        long totalTokens = 0;
        BigDecimal totalCost = BigDecimal.ZERO;
        OffsetDateTime lastSuccessfulGeneration = null;

        // Group runs by date string for activity chart
        Map<String, int[]> dailyActivity = new TreeMap<>(); // date -> [disc, dedup, summ, fail]
        List<DashboardStatsDto.AiUsagePoint> aiUsagePoints = new ArrayList<>();

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("MMM dd");

        for (IntelligenceRun run : runs) {
            articlesDiscovered += run.getArticlesSeen();
            articlesUnique += run.getArticlesNew();
            summariesGenerated += run.getSummariesGenerated();
            articlesDeferred += run.getSummariesDeferred();
            articlesFailed += run.getSummariesFailed();
            totalTokens += run.getTokensUsed();
            if (run.getEstimatedCost() != null) {
                totalCost = totalCost.add(run.getEstimatedCost());
            }

            if (lastSuccessfulGeneration == null &&
                ("COMPLETED".equals(run.getStatus()) || "PARTIAL".equals(run.getStatus()) || "BUDGET_LIMITED".equals(run.getStatus()))) {
                lastSuccessfulGeneration = run.getCompletedAt() != null ? run.getCompletedAt() : run.getStartedAt();
            }

            String dateKey = run.getRunDate() != null ? run.getRunDate().format(dateFmt) : run.getStartedAt().format(dateFmt);
            int[] counts = dailyActivity.computeIfAbsent(dateKey, k -> new int[4]);
            counts[0] += run.getArticlesSeen();
            counts[1] += run.getArticlesDeduplicated();
            counts[2] += run.getSummariesGenerated();
            counts[3] += run.getSummariesFailed();

            aiUsagePoints.add(new DashboardStatsDto.AiUsagePoint(
                    dateKey,
                    run.getId().toString().substring(0, 8),
                    run.getTokensUsed(),
                    run.getEstimatedCost() != null ? run.getEstimatedCost() : BigDecimal.ZERO
            ));
        }

        // If no runs in period, provide fallback counts from recent articles directly
        if (articlesDiscovered == 0 && !recentArticles.isEmpty()) {
            articlesDiscovered = recentArticles.size();
            articlesUnique = recentArticles.stream().filter(a -> a.getDuplicateOfId() == null).count();
            summariesGenerated = recentArticles.stream().filter(a -> "SUMMARIZED".equals(a.getProcessingStatus())).count();
            articlesDeferred = recentArticles.stream().filter(a -> "DEFERRED".equals(a.getProcessingStatus())).count();
            articlesFailed = recentArticles.stream().filter(a -> "FAILED".equals(a.getProcessingStatus())).count();
        }

        List<DashboardStatsDto.ProcessingActivityPoint> processingActivity = dailyActivity.entrySet().stream()
                .map(e -> new DashboardStatsDto.ProcessingActivityPoint(
                        e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2], e.getValue()[3]
                ))
                .collect(Collectors.toList());

        // Area distribution
        Map<String, Long> areaCounts = recentArticles.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getAreaOfInterest() != null ? a.getAreaOfInterest() : "TECHNOLOGY",
                        Collectors.counting()
                ));

        long totalArticlesInAreas = recentArticles.size();
        List<DashboardStatsDto.AreaDistributionPoint> areaDistribution = areaCounts.entrySet().stream()
                .map(e -> {
                    double pct = totalArticlesInAreas > 0
                            ? BigDecimal.valueOf((e.getValue() * 100.0) / totalArticlesInAreas)
                                .setScale(1, RoundingMode.HALF_UP).doubleValue()
                            : 0.0;
                    String friendlyName = formatAreaName(e.getKey());
                    return new DashboardStatsDto.AreaDistributionPoint(e.getKey(), friendlyName, e.getValue(), pct);
                })
                .sorted((a, b) -> Long.compare(b.count(), a.count()))
                .collect(Collectors.toList());

        // Source performance
        List<DashboardStatsDto.SourcePerformancePoint> sourcePerformance = new ArrayList<>();
        for (RssFeed feed : allFeeds) {
            var sources = articleSourceRepository.findByIdFeedId(feed.getId());
            long discovered = sources.size();
            long summarized = sources.stream()
                    .filter(s -> s.getArticle() != null && "SUMMARIZED".equals(s.getArticle().getProcessingStatus()))
                    .count();
            double rate = discovered > 0
                    ? BigDecimal.valueOf((summarized * 100.0) / discovered).setScale(1, RoundingMode.HALF_UP).doubleValue()
                    : 100.0;

            sourcePerformance.add(new DashboardStatsDto.SourcePerformancePoint(
                    feed.getId(),
                    feed.getName(),
                    discovered,
                    summarized,
                    rate
            ));
        }

        // Processing Outcomes
        long totalOutcomes = summariesGenerated + (articlesDiscovered - articlesUnique) + articlesDeferred + articlesFailed;
        DashboardStatsDto.ProcessingOutcomesDto outcomes = new DashboardStatsDto.ProcessingOutcomesDto(
                summariesGenerated,
                Math.max(0, articlesDiscovered - articlesUnique),
                articlesDeferred,
                articlesFailed,
                Math.max(1, totalOutcomes)
        );

        return new DashboardStatsDto(
                activeNodes,
                articlesDiscovered,
                articlesUnique,
                summariesGenerated,
                articlesDeferred,
                articlesFailed,
                lastSuccessfulGeneration,
                totalTokens,
                totalCost.setScale(4, RoundingMode.HALF_UP),
                days == 1 ? "Today" : "Last 7 Days",
                processingActivity,
                areaDistribution,
                sourcePerformance,
                aiUsagePoints,
                outcomes
        );
    }

    private String formatAreaName(String code) {
        if (code == null) return "General Tech";
        return Arrays.stream(code.split("_"))
                .map(w -> w.isEmpty() ? "" : Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
