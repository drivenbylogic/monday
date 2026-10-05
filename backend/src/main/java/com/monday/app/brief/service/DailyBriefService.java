package com.monday.app.brief.service;

import com.monday.app.brief.entity.DailyBrief;
import com.monday.app.brief.repository.DailyBriefRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class DailyBriefService {
    private static final Logger log = LoggerFactory.getLogger(DailyBriefService.class);
    private final DailyBriefRepository briefRepository;

    public DailyBriefService(DailyBriefRepository briefRepository) {
        this.briefRepository = briefRepository;
    }

    public DailyBrief getBriefByDate(LocalDate date) {
        return briefRepository.findByDate(date).orElse(null);
    }

    public List<DailyBrief> getRecentBriefs(int days) {
        return briefRepository.findRecent(days);
    }

    @Transactional
    public DailyBrief saveBrief(DailyBrief brief) {
        return brief.getId() == null ? briefRepository.save(brief) : briefRepository.update(brief);
    }

    /**
     * Nightly job: clean up daily briefs older than 7 days.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldBriefs() {
        LocalDate cutoffDate = LocalDate.now().minusDays(7);
        briefRepository.deleteOlderThan(cutoffDate);
        log.info("Cleaned up daily briefs older than {}", cutoffDate);
    }
}
