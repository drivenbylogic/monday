package com.monday.app.intelligence.scheduler;

import com.monday.app.intelligence.service.IntelligencePipelineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "intelligence.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class IntelligenceScheduler {

    private static final Logger log = LoggerFactory.getLogger(IntelligenceScheduler.class);
    
    private final IntelligencePipelineService pipelineService;

    public IntelligenceScheduler(IntelligencePipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    // Run at 2:00 AM every day
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduleDailyIntelligenceRun() {
        log.info("Starting scheduled daily intelligence run");
        pipelineService.runDaily();
        log.info("Finished scheduled daily intelligence run");
    }
}
