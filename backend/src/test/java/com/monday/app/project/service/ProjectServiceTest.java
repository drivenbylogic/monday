package com.monday.app.project.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.app.project.entity.Project;
import com.monday.app.project.repository.MilestoneRepository;
import com.monday.app.project.repository.ProjectRepository;
import com.monday.app.project.repository.TaskRepository;
import com.monday.app.shared.cache.CacheInvalidationService;
import com.monday.app.shared.cache.CacheNames;
import com.monday.app.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ProjectServiceTest {

    private ProjectRepository projectRepository;
    private MilestoneRepository milestoneRepository;
    private TaskRepository taskRepository;
    private NamedParameterJdbcTemplate jdbc;
    private ObjectMapper objectMapper;
    private CacheInvalidationService cacheInvalidationService;
    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        milestoneRepository = mock(MilestoneRepository.class);
        taskRepository = mock(TaskRepository.class);
        jdbc = mock(NamedParameterJdbcTemplate.class);
        objectMapper = new ObjectMapper();
        cacheInvalidationService = mock(CacheInvalidationService.class);
        projectService = new ProjectService(
                projectRepository,
                milestoneRepository,
                taskRepository,
                jdbc,
                objectMapper,
                cacheInvalidationService
        );
    }

    @Test
    void delete_existingProject_cascadesDeleteToTasksMilestonesAndProject() {
        UUID projectId = UUID.randomUUID();
        Project existing = new Project();
        existing.setId(projectId);
        existing.setName("Alpha Project");

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(existing));

        projectService.delete(projectId);

        // Verify taskRepository.deleteByProjectId is invoked
        verify(taskRepository).deleteByProjectId(projectId);

        // Verify milestoneRepository.deleteByProjectId is invoked
        verify(milestoneRepository).deleteByProjectId(projectId);

        // Verify projectRepository.deleteById is invoked
        verify(projectRepository).deleteById(projectId);

        // Verify cache invalidation
        verify(cacheInvalidationService).evictAllAfterCommit(
                CacheNames.PROJECT_METRICS,
                CacheNames.PROJECT_VELOCITY,
                CacheNames.HOME_DASHBOARD
        );
    }

    @Test
    void delete_nonExistentProject_throwsResourceNotFoundException() {
        UUID projectId = UUID.randomUUID();
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> projectService.delete(projectId));

        verify(taskRepository, never()).deleteByProjectId(any());
        verify(milestoneRepository, never()).deleteByProjectId(any());
        verify(projectRepository, never()).deleteById(any());
    }
}

