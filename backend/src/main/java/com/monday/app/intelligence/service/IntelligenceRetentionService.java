package com.monday.app.intelligence.service;

import com.monday.app.intelligence.repository.ArticleRepository;
import com.monday.app.intelligence.repository.ArticleSummaryRepository;
import com.monday.app.intelligence.repository.DailyArticleSelectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Service
public class IntelligenceRetentionService {

    private static final Logger log = LoggerFactory.getLogger(IntelligenceRetentionService.class);

    private final ArticleRepository articleRepository;
    private final ArticleSummaryRepository summaryRepository;
    private final DailyArticleSelectionRepository selectionRepository;

    @Value("${intelligence.retention.days:7}")
    private int retentionDays = 7;

    public IntelligenceRetentionService(ArticleRepository articleRepository,
                                        ArticleSummaryRepository summaryRepository,
                                        DailyArticleSelectionRepository selectionRepository) {
        this.articleRepository = articleRepository;
        this.summaryRepository = summaryRepository;
        this.selectionRepository = selectionRepository;
    }

    /**
     * Daily scheduled retention cleanup at 3:00 AM IST.
     * Cleans up article records and summaries older than 7 days.
     */
    @Scheduled(cron = "${intelligence.retention.cron:0 0 3 * * ?}", zone = "${intelligence.scheduler.zone:Asia/Kolkata}")
    @Transactional
    public void cleanupOldData() {
        log.info("Starting intelligence retention cleanup. Retaining last {} days.", retentionDays);
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(retentionDays);
        LocalDate cutoffDate = LocalDate.now().minusDays(retentionDays);

        try {
            // Delete daily selections before cutoff date
            selectionRepository.deleteBySelectionDateBefore(cutoffDate);

            // Delete summaries older than cutoff
            int deletedSummaries = summaryRepository.deleteSummariesOlderThan(cutoff);

            // Delete expired articles (foreign keys cascade delete article_sources)
            int deletedArticles = articleRepository.deleteArticlesOlderThan(cutoff);

            log.info("Retention cleanup completed successfully. Deleted {} articles and {} summaries older than {}.",
                    deletedArticles, deletedSummaries, cutoff);
        } catch (Exception e) {
            log.error("Failed to execute intelligence retention cleanup", e);
        }
    }
}
