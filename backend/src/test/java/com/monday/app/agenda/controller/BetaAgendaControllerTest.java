package com.monday.app.agenda.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.monday.app.agenda.dto.*;
import com.monday.app.agenda.service.AgendaEngineService;
import com.monday.app.agenda.service.AgendaGenerationService;
import com.monday.app.agenda.service.BetaBriefGenerationService;
import com.monday.app.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BetaAgendaControllerTest {

    private MockMvc mockMvc;
    private AgendaEngineService agendaEngineService;
    private AgendaGenerationService agendaGenerationService;
    private BetaBriefGenerationService betaBriefGenerationService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        agendaEngineService = mock(AgendaEngineService.class);
        agendaGenerationService = mock(AgendaGenerationService.class);
        betaBriefGenerationService = mock(BetaBriefGenerationService.class);

        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        AgendaController controller = new AgendaController(
                agendaEngineService,
                agendaGenerationService,
                betaBriefGenerationService
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getBetaDailyBrief_returnsBriefResponse() throws Exception {
        LocalDate date = LocalDate.of(2026, 11, 10);
        BetaDailyBriefDto briefing = new BetaDailyBriefDto(
                new BetaDailyBriefDto.TodayAtAGlanceSection("All Systems Operational", "Day is balanced", "NORMAL", "Execution"),
                List.of(new BetaDailyBriefDto.PriorityItem("pri-1", "Deploy Beta Brief", "Enables revamp", "HIGH", "TASK", "t-1", "Deploy")),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(new BetaDailyBriefDto.NextActionItem(1, "Deploy", "Task", "EXECUTION"))
        );

        BetaDailyBriefResponse response = new BetaDailyBriefResponse(
                date,
                "GENERATED",
                Instant.now(),
                "AI",
                briefing,
                null,
                false,
                null
        );

        when(betaBriefGenerationService.getBrief(any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/agenda/beta-brief?date=2026-11-10")
                        .header("X-Timezone", "UTC")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("GENERATED"))
                .andExpect(jsonPath("$.generationMode").value("AI"))
                .andExpect(jsonPath("$.briefing.todayAtAGlance.headline").value("All Systems Operational"))
                .andExpect(jsonPath("$.briefing.priorities[0].title").value("Deploy Beta Brief"));
    }

    @Test
    void generateBetaDailyBrief_triggersGeneration() throws Exception {
        LocalDate date = LocalDate.of(2026, 11, 10);
        BetaDailyBriefDto briefing = new BetaDailyBriefDto(
                new BetaDailyBriefDto.TodayAtAGlanceSection("Regenerated Brief", "Fresh synthesis", "ATTENTION", "Focus"),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );

        BetaDailyBriefResponse response = new BetaDailyBriefResponse(
                date,
                "GENERATED",
                Instant.now(),
                "DETERMINISTIC",
                briefing,
                null,
                false,
                null
        );

        when(betaBriefGenerationService.generateBrief(eq(date), any(), eq(true))).thenReturn(response);

        mockMvc.perform(post("/api/v1/agenda/generate-beta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GenerateBetaBriefRequest(date, "UTC", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generationMode").value("DETERMINISTIC"))
                .andExpect(jsonPath("$.briefing.todayAtAGlance.headline").value("Regenerated Brief"));
    }

    @Test
    void getBetaSnapshot_returnsInputPayload() throws Exception {
        LocalDate date = LocalDate.of(2026, 11, 10);
        DailyBriefSnapshotDto snapshot = new DailyBriefSnapshotDto(
                date, Instant.now(), "UTC",
                new DailyBriefSnapshotDto.ModulesSnapshot(
                        new DailyBriefSnapshotDto.CalendarModule("AVAILABLE", Collections.emptyList(), Collections.emptyList()),
                        new DailyBriefSnapshotDto.ProjectsModule("AVAILABLE", Collections.emptyList(), Collections.emptyList(), Collections.emptyList()),
                        new DailyBriefSnapshotDto.TasksModule("AVAILABLE", Collections.emptyList(), Collections.emptyList(), Collections.emptyList()),
                        new DailyBriefSnapshotDto.FinancesModule("AVAILABLE", Collections.emptyMap(), Collections.emptyList(), Collections.emptyList()),
                        new DailyBriefSnapshotDto.VehiclesModule("AVAILABLE", Collections.emptyList(), Collections.emptyList(), Collections.emptyList()),
                        new DailyBriefSnapshotDto.IntelligenceModule("AVAILABLE", Collections.emptyList())
                )
        );

        when(betaBriefGenerationService.getSnapshot(any(), any())).thenReturn(snapshot);

        mockMvc.perform(get("/api/v1/agenda/beta-snapshot?date=2026-11-10")
                        .header("X-Timezone", "UTC")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brief_date").value("2026-11-10"))
                .andExpect(jsonPath("$.modules.calendar.availability").value("AVAILABLE"))
                .andExpect(jsonPath("$.modules.projects.availability").value("AVAILABLE"));
    }
}

