package com.monday.app.project.service;

import com.monday.app.project.entity.Milestone;
import com.monday.app.project.repository.MilestoneRepository;
import com.monday.app.project.repository.ProjectRepository;
import com.monday.app.project.repository.TaskRepository;
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
    private final TaskRepository taskRepository;
    private final CacheInvalidationService cacheInvalidationService;

    public MilestoneService(MilestoneRepository milestoneRepository,
                            ProjectRepository projectRepository,
                            TaskRepository taskRepository) {
        this(milestoneRepository, projectRepository, taskRepository, null);
    }

    @Autowired
    public MilestoneService(MilestoneRepository milestoneRepository,
                            ProjectRepository projectRepository,
                            TaskRepository taskRepository,
                            @org.springframework.lang.Nullable CacheInvalidationService cacheInvalidationService) {
        this.milestoneRepository = milestoneRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    private void evictMetrics() {
        if (cacheInvalidationService != null) {
            cacheInvalidationService.evictAllAfterCommit(
                    CacheNames.PROJECT_METRICS,
                    CacheNames.PROJECT_VELOCITY,
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

        if (updated.getName() != null && !updated.getName().isBlank()) {
            existing.setName(updated.getName().trim());
        }
        if (updated.getDescription() != null) {
            existing.setDescription(updated.getDescription());
        }
        if (updated.getPriority() != null && !updated.getPriority().isBlank()) {
            existing.setPriority(updated.getPriority().trim().toUpperCase());
        }
        if (updated.getStartAt() != null) {
            existing.setStartAt(updated.getStartAt());
        }
        if (updated.getDueAt() != null) {
            existing.setDueAt(updated.getDueAt());
        }

        if (updated.getStatus() != null && !updated.getStatus().isBlank()) {
            String newStatus = updated.getStatus().trim().toUpperCase();
            existing.setStatus(newStatus);
            if ("COMPLETED".equals(newStatus)) {
                if (existing.getCompletedAt() == null) {
                    existing.setCompletedAt(Instant.now());
                }
            } else {
                existing.setCompletedAt(null);
            }
        }

        if (updated.getCompletedAt() != null) {
            existing.setCompletedAt(updated.getCompletedAt());
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
        if (taskRepository != null) {
            taskRepository.deleteByMilestoneId(id);
        }
        milestoneRepository.deleteById(id);
        evictMetrics();
    }
}
