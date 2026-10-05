package com.monday.app.intelligence.service;

import com.monday.app.intelligence.repository.ArticleSummaryRepository;
import com.monday.app.intelligence.repository.DailyArticleSelectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class IntelligenceRetentionService {

    private static final Logger log = LoggerFactory.getLogger(IntelligenceRetentionService.class);

    private final DailyArticleSelectionRepository selectionRepository;
    private final ArticleSummaryRepository summaryRepository;

    @Value("${intelligence.retention.days:7}")
    private int retentionDays;

    public IntelligenceRetentionService(DailyArticleSelectionRepository selectionRepository, 
                                        ArticleSummaryRepository summaryRepository) {
        this.selectionRepository = selectionRepository;
        this.summaryRepository = summaryRepository;
    }

    @Scheduled(cron = "0 30 2 * * ?") // Run at 2:30 AM
    @Transactional
    public void cleanupOldData() {
        log.info("Starting intelligence retention cleanup. Retaining last {} days.", retentionDays);
        LocalDate cutoffDate = LocalDate.now().minusDays(retentionDays);
        
        // Since JPA doesn't easily do cascading deletes for orphans across multiple detached entities efficiently in bulk,
        // we could just fetch and delete. For a small personal app, this is fine.
        // A better approach is JPQL bulk delete, but let's assume we can fetch them for now.
        
        // For actual cleanup we need a custom query to delete selections older than cutoffDate.
        // Let's rely on standard Spring Data JPQL method which we will add: deleteBySelectionDateBefore(cutoffDate)
        
        try {
            // Note: I will add the deleteBySelectionDateBefore to the repository 
            selectionRepository.deleteBySelectionDateBefore(cutoffDate);
            log.info("Retention cleanup completed successfully.");
        } catch (Exception e) {
            log.error("Failed to clean up old intelligence data", e);
        }
    }
}
