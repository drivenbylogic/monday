package com.monday.app.intelligence.service;

import org.springframework.stereotype.Service;

@Service
public class IntelligencePipelineService {

    private final IntelligenceCoordinator coordinator;

    public IntelligencePipelineService(IntelligenceCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    public void runDaily() {
        coordinator.triggerScheduledGeneration();
    }
}
