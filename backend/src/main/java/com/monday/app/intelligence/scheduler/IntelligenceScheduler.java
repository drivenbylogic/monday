package com.monday.app.intelligence.scheduler;

import com.monday.app.intelligence.service.IntelligenceCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "intelligence.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class IntelligenceScheduler {

    private static final Logger log = LoggerFactory.getLogger(IntelligenceScheduler.class);

    private final IntelligenceCoordinator coordinator;

    public IntelligenceScheduler(IntelligenceCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    /**
     * Runs daily at 6:00 AM IST (Indian Standard Time).
     */
    @Scheduled(cron = "${intelligence.scheduler.cron:0 0 6 * * ?}", zone = "${intelligence.scheduler.zone:Asia/Kolkata}")
    public void scheduleDailyIntelligenceRun() {
        log.info("Triggering scheduled 6:00 AM IST daily intelligence run");
        coordinator.triggerScheduledGeneration();
        log.info("Finished scheduled 6:00 AM IST daily intelligence run");
    }
}
