package com.monday.app.agenda.service;

import com.monday.app.agenda.dto.*;
import com.monday.app.event.repository.EventRepository;
import com.monday.app.project.entity.Task;
import com.monday.app.project.repository.MilestoneRepository;
import com.monday.app.project.repository.ProjectRepository;
import com.monday.app.project.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AgendaValidationServiceTest {

    private TaskRepository taskRepository;
    private ProjectRepository projectRepository;
    private MilestoneRepository milestoneRepository;
    private EventRepository eventRepository;

    private AgendaValidationService validationService;

    @BeforeEach
    void setUp() {
        taskRepository = mock(TaskRepository.class);
        projectRepository = mock(ProjectRepository.class);
        milestoneRepository = mock(MilestoneRepository.class);
        eventRepository = mock(EventRepository.class);

        validationService = new AgendaValidationService(
                taskRepository,
                projectRepository,
                milestoneRepository,
                eventRepository
        );
    }

    @Test
    void validateRecommendations_discardsFabricatedTaskIds() {
        UUID nonExistentId = UUID.randomUUID();
        when(taskRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        AgendaRecommendationDto rec = new AgendaRecommendationDto(
                "rec-1",
                "Hallucinated Task Execution",
                "AI hallucinated a task",
                "HIGH",
                "Reason",
                "Action",
                List.of(new SourceReferenceDto("TASK", nonExistentId, "Fake Task")),
                60,
                null,
                null,
                0.95,
                null
        );

        List<AgendaRecommendationDto> validated = validationService.validateRecommendations(
                List.of(rec),
                Collections.emptyList()
        );

        assertEquals(1, validated.size());
        // The invalid source reference was stripped!
        assertTrue(validated.get(0).sourceReferences().isEmpty());
    }

    @Test
    void validateRecommendations_rejectsAlreadyCompletedTaskRecommendations() {
        UUID completedTaskId = UUID.randomUUID();
        Task completedTask = new Task();
        completedTask.setId(completedTaskId);
        completedTask.setTitle("Old Done Task");
        completedTask.setStatus("COMPLETED");

        when(taskRepository.findById(completedTaskId)).thenReturn(Optional.of(completedTask));

        AgendaRecommendationDto rec = new AgendaRecommendationDto(
                "rec-1",
                "Work on Done Task",
                "Task is already completed",
                "HIGH",
                "Reason",
                "Action",
                List.of(new SourceReferenceDto("TASK", completedTaskId, "Old Done Task")),
                60,
                null,
                null,
                0.95,
                null
        );

        List<AgendaRecommendationDto> validated = validationService.validateRecommendations(
                List.of(rec),
                Collections.emptyList()
        );

        // Entire recommendation is discarded because the task was already completed
        assertTrue(validated.isEmpty());
    }

    @Test
    void validateRecommendations_clearsTimeWindowIfConflictingWithExistingEvents() {
        UUID taskId = UUID.randomUUID();
        Task pendingTask = new Task();
        pendingTask.setId(taskId);
        pendingTask.setTitle("Important Task");
        pendingTask.setStatus("PENDING");
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(pendingTask));

        Instant eventStart = Instant.parse("2026-10-26T10:00:00Z");
        Instant eventEnd = Instant.parse("2026-10-26T11:00:00Z");

        EventSummaryDto existingEvent = new EventSummaryDto(
                UUID.randomUUID(), "Meeting", "Desc", "WORK",
                eventStart, eventEnd, false, "Office", "SCHEDULED", false, null, null
        );

        // Suggest time that overlaps with existing event: 10:30 to 11:30
        Instant suggestedStart = Instant.parse("2026-10-26T10:30:00Z");
        Instant suggestedEnd = Instant.parse("2026-10-26T11:30:00Z");

        AgendaRecommendationDto rec = new AgendaRecommendationDto(
                "rec-1",
                "Important Task Execution",
                "Desc",
                "HIGH",
                "Reason",
                "Action",
                List.of(new SourceReferenceDto("TASK", taskId, "Important Task")),
                60,
                suggestedStart,
                suggestedEnd,
                0.95,
                null
        );

        List<AgendaRecommendationDto> validated = validationService.validateRecommendations(
                List.of(rec),
                List.of(existingEvent)
        );

        assertEquals(1, validated.size());
        // Recommendation is preserved, but conflicting time suggestion is cleared
        assertNull(validated.get(0).suggestedStartAt());
        assertNull(validated.get(0).suggestedEndAt());
        assertEquals("Important Task Execution", validated.get(0).title());
    }
}
