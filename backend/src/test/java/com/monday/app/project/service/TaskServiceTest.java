package com.monday.app.project.service;

import com.monday.app.project.entity.Milestone;
import com.monday.app.project.entity.Project;
import com.monday.app.project.entity.Task;
import com.monday.app.project.repository.MilestoneRepository;
import com.monday.app.project.repository.ProjectRepository;
import com.monday.app.project.repository.TaskRepository;
import com.monday.app.shared.cache.CacheInvalidationService;
import com.monday.app.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TaskServiceTest {

    private TaskRepository taskRepository;
    private ProjectRepository projectRepository;
    private MilestoneRepository milestoneRepository;
    private CacheInvalidationService cacheInvalidationService;
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskRepository = mock(TaskRepository.class);
        projectRepository = mock(ProjectRepository.class);
        milestoneRepository = mock(MilestoneRepository.class);
        cacheInvalidationService = mock(CacheInvalidationService.class);
        taskService = new TaskService(taskRepository, projectRepository, milestoneRepository, cacheInvalidationService);
    }

    @Test
    void update_partialUpdateWithOnlyStatusCompleted_preservesTitleAndOtherFields() {
        UUID taskId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID milestoneId = UUID.randomUUID();

        Task existing = new Task();
        existing.setId(taskId);
        existing.setTitle("Initial Task Title");
        existing.setDescription("Task Description");
        existing.setProjectId(projectId);
        existing.setMilestoneId(milestoneId);
        existing.setStatus("PENDING");
        existing.setPriority("HIGH");
        existing.setEstimatedMinutes(45);
        existing.setCreatedAt(Instant.now());
        existing.setUpdatedAt(Instant.now());

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existing));
        when(taskRepository.update(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Call update with only status set to COMPLETED (as happens from frontend toggle)
        Task partialUpdate = new Task();
        partialUpdate.setStatus("COMPLETED");

        Task result = taskService.update(taskId, partialUpdate);

        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        assertEquals("Initial Task Title", result.getTitle(), "Title must NOT be overwritten with null");
        assertEquals("Task Description", result.getDescription(), "Description must NOT be overwritten with null");
        assertEquals(projectId, result.getProjectId(), "ProjectId must NOT be overwritten with null");
        assertEquals(milestoneId, result.getMilestoneId(), "MilestoneId must NOT be overwritten with null");
        assertEquals("HIGH", result.getPriority(), "Priority must NOT be overwritten with null");
        assertEquals(45, result.getEstimatedMinutes(), "EstimatedMinutes must NOT be overwritten with null");
        assertNotNull(result.getCompletedAt(), "completedAt should be automatically set when transitioning to COMPLETED");

        verify(taskRepository).update(existing);
    }

    @Test
    void update_transitionFromCompletedToPending_clearsCompletedAt() {
        UUID taskId = UUID.randomUUID();

        Task existing = new Task();
        existing.setId(taskId);
        existing.setTitle("Completed Task");
        existing.setStatus("COMPLETED");
        existing.setCompletedAt(Instant.now().minusSeconds(3600));

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existing));
        when(taskRepository.update(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task partialUpdate = new Task();
        partialUpdate.setStatus("PENDING");

        Task result = taskService.update(taskId, partialUpdate);

        assertEquals("PENDING", result.getStatus());
        assertEquals("Completed Task", result.getTitle());
        assertNull(result.getCompletedAt(), "completedAt should be reset to null when leaving COMPLETED");
    }

    @Test
    void update_partialUpdateTitle_doesNotChangeStatusOrCompletedAt() {
        UUID taskId = UUID.randomUUID();
        Instant completedAt = Instant.now().minusSeconds(1000);

        Task existing = new Task();
        existing.setId(taskId);
        existing.setTitle("Old Title");
        existing.setStatus("COMPLETED");
        existing.setCompletedAt(completedAt);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existing));
        when(taskRepository.update(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task partialUpdate = new Task();
        partialUpdate.setTitle("Updated Title");

        Task result = taskService.update(taskId, partialUpdate);

        assertEquals("Updated Title", result.getTitle());
        assertEquals("COMPLETED", result.getStatus());
        assertEquals(completedAt, result.getCompletedAt(), "completedAt must be preserved");
    }

    @Test
    void create_missingTitle_throwsBusinessRuleException() {
        Task task = new Task();
        task.setTitle("");

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> taskService.create(task));
        assertEquals("TASK_TITLE_REQUIRED", ex.getCode());
    }

    @Test
    void update_milestoneProjectMismatch_throwsBusinessRuleException() {
        UUID taskId = UUID.randomUUID();
        UUID projectA = UUID.randomUUID();
        UUID projectB = UUID.randomUUID();
        UUID milestoneId = UUID.randomUUID();

        Milestone milestone = new Milestone();
        milestone.setId(milestoneId);
        milestone.setProjectId(projectA);

        Task existing = new Task();
        existing.setId(taskId);
        existing.setTitle("Test Task");

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existing));
        when(milestoneRepository.findById(milestoneId)).thenReturn(Optional.of(milestone));

        Task updated = new Task();
        updated.setMilestoneId(milestoneId);
        updated.setProjectId(projectB);

        assertThrows(BusinessRuleException.class, () -> taskService.update(taskId, updated));
    }
}
