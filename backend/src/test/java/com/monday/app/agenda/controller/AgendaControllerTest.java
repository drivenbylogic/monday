package com.monday.app.agenda.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.monday.app.agenda.dto.*;
import com.monday.app.agenda.service.AgendaEngineService;
import com.monday.app.agenda.service.AgendaGenerationService;
import com.monday.app.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
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

class AgendaControllerTest {

    private MockMvc mockMvc;
    private AgendaEngineService agendaEngineService;
    private AgendaGenerationService agendaGenerationService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        agendaEngineService = mock(AgendaEngineService.class);
        agendaGenerationService = mock(AgendaGenerationService.class);
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders.standaloneSetup(new AgendaController(agendaEngineService, agendaGenerationService))
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getTodayAgenda_returnsGeneratedAgendaResponse() throws Exception {
        LocalDate date = LocalDate.of(2026, 10, 26);
        TodayAgendaResponse response = new TodayAgendaResponse(
                date,
                "GENERATED",
                Instant.now(),
                "## Daily Briefing\nFocus on API completion.",
                List.of(new AgendaRecommendationDto("rec-1", "Finish API", "Desc", "HIGH", "Milestone deadline", "Write tests", Collections.emptyList(), 60, null, null, 0.95, null)),
                Collections.emptyList(),
                Collections.emptyList(),
                new FinancialHeadsUpDto("Financial Status", "Balanced", BigDecimal.ZERO, "LOW"),
                new ExecutionInsightDto("Velocity", "Good progress", "100%"),
                false,
                null
        );

        when(agendaGenerationService.getAgenda(any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/agenda/today?date=2026-10-26")
                        .header("X-Timezone", "UTC")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-10-26"))
                .andExpect(jsonPath("$.status").value("GENERATED"))
                .andExpect(jsonPath("$.recommendations[0].title").value("Finish API"));
    }

    @Test
    void generateAgenda_triggersRegeneration() throws Exception {
        LocalDate date = LocalDate.of(2026, 10, 26);
        GenerateAgendaRequest request = new GenerateAgendaRequest(date, "UTC", true);

        TodayAgendaResponse response = new TodayAgendaResponse(
                date,
                "GENERATED",
                Instant.now(),
                "Refreshed briefing",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                null,
                false,
                null
        );

        when(agendaGenerationService.generateAgenda(eq(date), any(), eq(true))).thenReturn(response);

        mockMvc.perform(post("/api/v1/agenda/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("GENERATED"))
                .andExpect(jsonPath("$.summaryMarkdown").value("Refreshed briefing"));
    }

    @Test
    void submitFeedback_persistsAcceptedFeedback() throws Exception {
        LocalDate date = LocalDate.of(2026, 10, 26);
        AgendaFeedbackRequest request = new AgendaFeedbackRequest(date, "rec-1", "ACCEPTED");

        TodayAgendaResponse response = new TodayAgendaResponse(
                date,
                "GENERATED",
                Instant.now(),
                "Briefing",
                List.of(new AgendaRecommendationDto("rec-1", "Finish API", "Desc", "HIGH", "Reason", "Action", Collections.emptyList(), 60, null, null, 0.95, "ACCEPTED")),
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                null,
                false,
                null
        );

        when(agendaGenerationService.applyFeedback(eq(date), eq("rec-1"), eq("ACCEPTED"))).thenReturn(response);

        mockMvc.perform(post("/api/v1/agenda/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations[0].feedbackStatus").value("ACCEPTED"));
    }
}
