package com.monday.app.project.service;

import com.monday.app.project.entity.Milestone;
import com.monday.app.project.repository.MilestoneRepository;
import com.monday.app.project.repository.ProjectRepository;
import com.monday.app.shared.cache.CacheInvalidationService;
import com.monday.app.shared.cache.CacheNames;
import com.monday.app.shared.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final ProjectRepository projectRepository;
    private final CacheInvalidationService cacheInvalidationService;

    public MilestoneService(MilestoneRepository milestoneRepository,
                            ProjectRepository projectRepository) {
        this(milestoneRepository, projectRepository, null);
    }

    @Autowired
    public MilestoneService(MilestoneRepository milestoneRepository,
                            ProjectRepository projectRepository,
                            @org.springframework.lang.Nullable CacheInvalidationService cacheInvalidationService) {
        this.milestoneRepository = milestoneRepository;
        this.projectRepository = projectRepository;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    private void evictMetrics() {
        if (cacheInvalidationService != null) {
            cacheInvalidationService.evictAllAfterCommit(
                    CacheNames.PROJECT_METRICS,
                    CacheNames.HOME_DASHBOARD
            );
        }
    }

    @Transactional
    public Milestone create(Milestone milestone) {
        // Validate project exists
        projectRepository.findById(milestone.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", milestone.getProjectId()));

        if (milestone.getStatus() == null) {
            milestone.setStatus("PENDING");
        }
        if (milestone.getPriority() == null) {
            milestone.setPriority("MEDIUM");
        }
        Milestone saved = milestoneRepository.save(milestone);
        evictMetrics();
        return saved;
    }

    @Transactional
    public Milestone update(UUID id, Milestone updated) {
        Milestone existing = milestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", id));

        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setStatus(updated.getStatus());
        existing.setPriority(updated.getPriority());
        existing.setStartAt(updated.getStartAt());
        existing.setDueAt(updated.getDueAt());

        if ("COMPLETED".equals(updated.getStatus()) && existing.getCompletedAt() == null) {
            existing.setCompletedAt(Instant.now());
        } else if (!"COMPLETED".equals(updated.getStatus())) {
            existing.setCompletedAt(null);
        }

        Milestone saved = milestoneRepository.update(existing);
        evictMetrics();
        return saved;
    }

    public Milestone getById(UUID id) {
        return milestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", id));
    }

    public List<Milestone> getByProjectId(UUID projectId) {
        return milestoneRepository.findByProjectId(projectId);
    }

    @Transactional
    public void delete(UUID id) {
        milestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", id));
        milestoneRepository.deleteById(id);
        evictMetrics();
    }
}
