package com.monday.app.intelligence.service;

import com.monday.app.intelligence.entity.IndustrySource;
import com.monday.app.intelligence.entity.IndustrySummary;
import com.monday.app.intelligence.repository.IndustrySourceRepository;
import com.monday.app.intelligence.repository.IndustrySummaryRepository;
import com.monday.app.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class IntelligenceService {
    private static final Logger log = LoggerFactory.getLogger(IntelligenceService.class);
    
    private final IndustrySourceRepository sourceRepository;
    private final IndustrySummaryRepository summaryRepository;

    public IntelligenceService(IndustrySourceRepository sourceRepository, IndustrySummaryRepository summaryRepository) {
        this.sourceRepository = sourceRepository;
        this.summaryRepository = summaryRepository;
    }

    @Transactional
    public IndustrySource createSource(IndustrySource source) {
        if (source.getSourceType() == null) source.setSourceType("RSS");
        return sourceRepository.save(source);
    }

    @Transactional
    public IndustrySource updateSource(UUID id, IndustrySource updated) {
        IndustrySource existing = sourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IndustrySource", id));
        existing.setName(updated.getName());
        existing.setFeedUrl(updated.getFeedUrl());
        existing.setSourceType(updated.getSourceType());
        existing.setActive(updated.isActive());
        return sourceRepository.update(existing);
    }

    public List<IndustrySource> getAllActiveSources() {
        return sourceRepository.findAllActive();
    }

    public IndustrySummary getSummaryByDate(LocalDate date) {
        return summaryRepository.findByDate(date).orElse(null);
    }

    public List<IndustrySummary> getRecentSummaries(int days) {
        return summaryRepository.findRecent(days);
    }

    @Transactional
    public IndustrySummary saveSummary(IndustrySummary summary) {
        return summaryRepository.save(summary);
    }

    /**
     * Nightly job: clean up intelligence summaries older than 7 days.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldSummaries() {
        LocalDate cutoffDate = LocalDate.now().minusDays(7);
        summaryRepository.deleteOlderThan(cutoffDate);
        log.info("Cleaned up industry summaries older than {}", cutoffDate);
    }
}
