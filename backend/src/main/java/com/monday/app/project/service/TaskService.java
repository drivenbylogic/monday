package com.monday.app.project.service;

import com.monday.app.project.entity.Task;
import com.monday.app.project.repository.MilestoneRepository;
import com.monday.app.project.repository.ProjectRepository;
import com.monday.app.project.repository.TaskRepository;
import com.monday.app.shared.cache.CacheInvalidationService;
import com.monday.app.shared.cache.CacheNames;
import com.monday.app.shared.exception.BusinessRuleException;
import com.monday.app.shared.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final MilestoneRepository milestoneRepository;
    private final CacheInvalidationService cacheInvalidationService;

    public TaskService(TaskRepository taskRepository,
                       ProjectRepository projectRepository,
                       MilestoneRepository milestoneRepository) {
        this(taskRepository, projectRepository, milestoneRepository, null);
    }

    @Autowired
    public TaskService(TaskRepository taskRepository,
                       ProjectRepository projectRepository,
                       MilestoneRepository milestoneRepository,
                       @org.springframework.lang.Nullable CacheInvalidationService cacheInvalidationService) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.milestoneRepository = milestoneRepository;
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
    public Task create(Task task) {
        if (task.getTitle() == null || task.getTitle().isBlank()) {
            throw new BusinessRuleException("TASK_TITLE_REQUIRED", "Task title cannot be null or empty");
        }
        validateReferences(task);

        if (task.getStatus() == null || task.getStatus().isBlank()) {
            task.setStatus("PENDING");
        } else {
            task.setStatus(task.getStatus().trim().toUpperCase());
        }
        if (task.getPriority() == null || task.getPriority().isBlank()) {
            task.setPriority("MEDIUM");
        } else {
            task.setPriority(task.getPriority().trim().toUpperCase());
        }
        Task saved = taskRepository.save(task);
        evictMetrics();
        return saved;
    }

    @Transactional
    public Task update(UUID id, Task updated) {
        Task existing = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));

        if (updated.getTitle() != null && !updated.getTitle().isBlank()) {
            existing.setTitle(updated.getTitle().trim());
        }
        if (updated.getDescription() != null) {
            existing.setDescription(updated.getDescription());
        }

        // Validate and apply references
        if (updated.getMilestoneId() != null) {
            var milestone = milestoneRepository.findById(updated.getMilestoneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Milestone", updated.getMilestoneId()));
            existing.setMilestoneId(updated.getMilestoneId());
            if (updated.getProjectId() != null) {
                if (!milestone.getProjectId().equals(updated.getProjectId())) {
                    throw new BusinessRuleException("MILESTONE_PROJECT_MISMATCH",
                            "Milestone does not belong to the specified project");
                }
                existing.setProjectId(updated.getProjectId());
            } else {
                existing.setProjectId(milestone.getProjectId());
            }
        } else if (updated.getProjectId() != null) {
            projectRepository.findById(updated.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project", updated.getProjectId()));
            existing.setProjectId(updated.getProjectId());
            if (existing.getMilestoneId() != null) {
                var milestone = milestoneRepository.findById(existing.getMilestoneId()).orElse(null);
                if (milestone != null && !milestone.getProjectId().equals(updated.getProjectId())) {
                    existing.setMilestoneId(null);
                }
            }
        }

        if (updated.getEventId() != null) {
            existing.setEventId(updated.getEventId());
        }
        if (updated.getParentTaskId() != null) {
            existing.setParentTaskId(updated.getParentTaskId());
        }
        if (updated.getPriority() != null && !updated.getPriority().isBlank()) {
            existing.setPriority(updated.getPriority().trim().toUpperCase());
        }
        if (updated.getDueAt() != null) {
            existing.setDueAt(updated.getDueAt());
        }
        if (updated.getEstimatedMinutes() != null) {
            existing.setEstimatedMinutes(updated.getEstimatedMinutes());
        }
        if (updated.getRecurring() != null) {
            existing.setRecurring(updated.getRecurring());
        }
        if (updated.getRecurrencePattern() != null) {
            existing.setRecurrencePattern(updated.getRecurrencePattern());
        }

        // Status transitions
        if (updated.getStatus() != null && !updated.getStatus().isBlank()) {
            String newStatus = updated.getStatus().trim().toUpperCase();
            existing.setStatus(newStatus);
            if ("IN_PROGRESS".equals(newStatus) && existing.getStartedAt() == null) {
                existing.setStartedAt(Instant.now());
            }
            if ("COMPLETED".equals(newStatus)) {
                if (existing.getCompletedAt() == null) {
                    existing.setCompletedAt(Instant.now());
                }
            } else {
                existing.setCompletedAt(null);
            }
        }

        if (updated.getStartedAt() != null) {
            existing.setStartedAt(updated.getStartedAt());
        }
        if (updated.getCompletedAt() != null) {
            existing.setCompletedAt(updated.getCompletedAt());
        }

        Task saved = taskRepository.update(existing);
        evictMetrics();
        return saved;
    }

    public Task getById(UUID id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }

    public List<Task> getAll() {
        return taskRepository.findAll();
    }

    public List<Task> getPaged(int page, int size) {
        return taskRepository.findPaged(page, size);
    }

    public int countAll() {
        return taskRepository.countAll();
    }

    public List<Task> getByProjectId(UUID projectId) {
        return taskRepository.findByProjectId(projectId);
    }

    public List<Task> getByMilestoneId(UUID milestoneId) {
        return taskRepository.findByMilestoneId(milestoneId);
    }

    public List<Task> getByEventId(UUID eventId) {
        return taskRepository.findByEventId(eventId);
    }

    public List<Task> getByStatus(String status) {
        return taskRepository.findByStatus(status);
    }

    public List<Task> getDueByDate(Instant dueBy) {
        return taskRepository.findDueByDate(dueBy);
    }

    public List<Task> getOverdue() {
        return taskRepository.findOverdue();
    }

    /**
     * Generate a single occurrence from a recurring template.
     */
    @Transactional
    public Task generateOccurrence(UUID templateTaskId, Instant dueAt) {
        Task template = taskRepository.findById(templateTaskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", templateTaskId));

        if (!template.isRecurring()) {
            throw new BusinessRuleException("NOT_RECURRING", "Task is not a recurring template");
        }

        Task occurrence = new Task();
        occurrence.setTitle(template.getTitle());
        occurrence.setDescription(template.getDescription());
        occurrence.setProjectId(template.getProjectId());
        occurrence.setMilestoneId(template.getMilestoneId());
        occurrence.setEventId(template.getEventId());
        occurrence.setParentTaskId(template.getId());
        occurrence.setStatus("PENDING");
        occurrence.setPriority(template.getPriority());
        occurrence.setDueAt(dueAt);
        occurrence.setEstimatedMinutes(template.getEstimatedMinutes());
        occurrence.setRecurring(false);

        Task saved = taskRepository.save(occurrence);
        evictMetrics();
        return saved;
    }

    public List<Task> getOccurrences(UUID templateTaskId) {
        return taskRepository.findByParentTaskId(templateTaskId);
    }

    @Transactional
    public void delete(UUID id) {
        taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
        taskRepository.deleteById(id);
        evictMetrics();
    }

    private void validateReferences(Task task) {
        if (task.getMilestoneId() != null) {
            var milestone = milestoneRepository.findById(task.getMilestoneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Milestone", task.getMilestoneId()));
            // If project is not explicitly provided, infer it directly from the milestone
            if (task.getProjectId() == null) {
                task.setProjectId(milestone.getProjectId());
            } else if (!milestone.getProjectId().equals(task.getProjectId())) {
                throw new BusinessRuleException("MILESTONE_PROJECT_MISMATCH",
                        "Milestone does not belong to the specified project");
            }
        }
        if (task.getProjectId() != null) {
            projectRepository.findById(task.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project", task.getProjectId()));
        }
    }
}
