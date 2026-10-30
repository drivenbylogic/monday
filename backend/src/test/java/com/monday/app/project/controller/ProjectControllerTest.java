package com.monday.app.project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.app.project.dto.DeepWorkAnalyticsResponse;
import com.monday.app.project.dto.DeepWorkSessionRequest;
import com.monday.app.project.dto.DeepWorkSessionResponse;
import com.monday.app.project.service.ProjectService;
import com.monday.app.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProjectControllerTest {

    private MockMvc mockMvc;
    private ProjectService projectService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        projectService = mock(ProjectService.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(new ProjectController(projectService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void logDeepWorkSession_validRequest_returnsCreatedSession() throws Exception {
        DeepWorkSessionResponse response = new DeepWorkSessionResponse(
                "session-8491", 3.0, 34.5, 40.0, "LOGGED"
        );

        when(projectService.logDeepWorkSession(any(DeepWorkSessionRequest.class))).thenReturn(response);

        String payload = """
                {
                    "projectId": "proj-monday-arch",
                    "durationMinutes": 180,
                    "startedAt": "2026-10-14T05:00:00Z",
                    "endedAt": "2026-10-14T08:00:00Z",
                    "focusScore": 95,
                    "notes": "Refactored cross-process IPC telemetry channel & benchmarked zero-copy deserialization",
                    "tasksCompleted": ["task-ipc-01", "task-ipc-02"]
                }
                """;

        mockMvc.perform(post("/api/v1/projects/deep-work/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("session-8491"))
                .andExpect(jsonPath("$.loggedHours").value(3.0))
                .andExpect(jsonPath("$.weeklyTotalHours").value(34.5))
                .andExpect(jsonPath("$.targetWeeklyHours").value(40.0))
                .andExpect(jsonPath("$.status").value("LOGGED"));

        verify(projectService).logDeepWorkSession(any(DeepWorkSessionRequest.class));
    }

    @Test
    void logDeepWorkSession_zeroDuration_returnsValidationError() throws Exception {
        String payload = """
                {
                    "projectId": "proj-monday-arch",
                    "durationMinutes": 0,
                    "focusScore": 90
                }
                """;

        mockMvc.perform(post("/api/v1/projects/deep-work/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(true))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void getDeepWorkAnalytics_returnsAnalyticsSummary() throws Exception {
        Map<String, Double> dailyAverages = Map.of(
                "monday", 6.5,
                "tuesday", 5.0,
                "wednesday", 7.0,
                "thursday", 5.5,
                "friday", 6.0,
                "saturday", 3.0,
                "sunday", 1.5
        );

        DeepWorkAnalyticsResponse analytics = new DeepWorkAnalyticsResponse(
                34.5, 38.0, -9.2, dailyAverages, "Karthick Ravichandran"
        );

        when(projectService.getDeepWorkAnalytics(eq(2))).thenReturn(analytics);

        mockMvc.perform(get("/api/v1/projects/deep-work/analytics?weeks=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentWeekHours").value(34.5))
                .andExpect(jsonPath("$.pastWeekHours").value(38.0))
                .andExpect(jsonPath("$.percentageChange").value(-9.2))
                .andExpect(jsonPath("$.leadArchitect").value("Karthick Ravichandran"))
                .andExpect(jsonPath("$.dailyAverages.monday").value(6.5))
                .andExpect(jsonPath("$.dailyAverages.wednesday").value(7.0));

        verify(projectService).getDeepWorkAnalytics(2);
    }

    @Test
    void createProject_withArrayTags_returnsCreatedProject() throws Exception {
        com.monday.app.project.entity.Project saved = new com.monday.app.project.entity.Project();
        saved.setId(java.util.UUID.randomUUID());
        saved.setName("Sovereign Engine");
        saved.setCategory("WORK");
        saved.setStatus("PLANNING");
        saved.setPriority("HIGH");
        saved.setTags("[\"Distributed\",\"WASM\"]");

        when(projectService.create(any(com.monday.app.project.entity.Project.class))).thenReturn(saved);

        String payload = """
                {
                    "name": "Sovereign Engine",
                    "description": "Core engine prototype",
                    "category": "WORK",
                    "status": "PLANNING",
                    "priority": "HIGH",
                    "tags": ["Distributed", "WASM"]
                }
                """;

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Sovereign Engine"))
                .andExpect(jsonPath("$.tags").value("[\"Distributed\",\"WASM\"]"));

        verify(projectService).create(any(com.monday.app.project.entity.Project.class));
    }
}
