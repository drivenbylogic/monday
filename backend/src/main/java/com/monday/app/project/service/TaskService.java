package com.monday.app.project.service;

import com.monday.app.project.entity.Task;
import com.monday.app.project.repository.MilestoneRepository;
import com.monday.app.project.repository.ProjectRepository;
import com.monday.app.project.repository.TaskRepository;
import com.monday.app.shared.exception.BusinessRuleException;
import com.monday.app.shared.exception.ResourceNotFoundException;
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

    public TaskService(TaskRepository taskRepository,
                       ProjectRepository projectRepository,
                       MilestoneRepository milestoneRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.milestoneRepository = milestoneRepository;
    }

    @Transactional
    public Task create(Task task) {
        validateReferences(task);

        if (task.getStatus() == null) {
            task.setStatus("PENDING");
        }
        if (task.getPriority() == null) {
            task.setPriority("MEDIUM");
        }
        return taskRepository.save(task);
    }

    @Transactional
    public Task update(UUID id, Task updated) {
        Task existing = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));

        validateReferences(updated);

        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setProjectId(updated.getProjectId());
        existing.setMilestoneId(updated.getMilestoneId());
        existing.setEventId(updated.getEventId());
        existing.setStatus(updated.getStatus());
        existing.setPriority(updated.getPriority());
        existing.setDueAt(updated.getDueAt());
        existing.setEstimatedMinutes(updated.getEstimatedMinutes());

        // Status transitions
        if ("IN_PROGRESS".equals(updated.getStatus()) && existing.getStartedAt() == null) {
            existing.setStartedAt(Instant.now());
        }
        if ("COMPLETED".equals(updated.getStatus()) && existing.getCompletedAt() == null) {
            existing.setCompletedAt(Instant.now());
        } else if (!"COMPLETED".equals(updated.getStatus())) {
            existing.setCompletedAt(null);
        }

        return taskRepository.update(existing);
    }

    public Task getById(UUID id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }

    public List<Task> getAll() {
        return taskRepository.findAll();
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

        return taskRepository.save(occurrence);
    }

    public List<Task> getOccurrences(UUID templateTaskId) {
        return taskRepository.findByParentTaskId(templateTaskId);
    }

    @Transactional
    public void delete(UUID id) {
        taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
        taskRepository.deleteById(id);
    }

    private void validateReferences(Task task) {
        if (task.getProjectId() != null) {
            projectRepository.findById(task.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project", task.getProjectId()));
        }
        if (task.getMilestoneId() != null) {
            var milestone = milestoneRepository.findById(task.getMilestoneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Milestone", task.getMilestoneId()));
            // If both project and milestone are set, milestone must belong to the project
            if (task.getProjectId() != null && !milestone.getProjectId().equals(task.getProjectId())) {
                throw new BusinessRuleException("MILESTONE_PROJECT_MISMATCH",
                        "Milestone does not belong to the specified project");
            }
        }
    }
}
