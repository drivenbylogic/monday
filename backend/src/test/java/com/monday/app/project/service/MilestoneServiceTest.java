package com.monday.app.project.service;

import com.monday.app.project.entity.Milestone;
import com.monday.app.project.entity.Project;
import com.monday.app.project.repository.MilestoneRepository;
import com.monday.app.project.repository.ProjectRepository;
import com.monday.app.project.repository.TaskRepository;
import com.monday.app.shared.cache.CacheInvalidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class MilestoneServiceTest {

    private MilestoneRepository milestoneRepository;
    private ProjectRepository projectRepository;
    private TaskRepository taskRepository;
    private CacheInvalidationService cacheInvalidationService;
    private MilestoneService milestoneService;

    @BeforeEach
    void setUp() {
        milestoneRepository = mock(MilestoneRepository.class);
        projectRepository = mock(ProjectRepository.class);
        taskRepository = mock(TaskRepository.class);
        cacheInvalidationService = mock(CacheInvalidationService.class);
        milestoneService = new MilestoneService(milestoneRepository, projectRepository, taskRepository, cacheInvalidationService);
    }

    @Test
    void delete_deletesMilestoneAndAllItsAssociatedTasks() {
        UUID milestoneId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        Milestone milestone = new Milestone();
        milestone.setId(milestoneId);
        milestone.setProjectId(projectId);
        milestone.setName("Alpha Milestone");

        when(milestoneRepository.findById(milestoneId)).thenReturn(Optional.of(milestone));

        milestoneService.delete(milestoneId);

        // Verify taskRepository.deleteByMilestoneId was called
        verify(taskRepository, times(1)).deleteByMilestoneId(milestoneId);
        // Verify milestone was deleted
        verify(milestoneRepository, times(1)).deleteById(milestoneId);
    }
}

