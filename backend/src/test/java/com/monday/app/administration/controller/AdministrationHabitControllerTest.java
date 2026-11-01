package com.monday.app.administration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.monday.app.administration.dto.*;
import com.monday.app.administration.service.*;
import com.monday.app.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdministrationHabitControllerTest {

    private MockMvc mockMvc;
    private AdministrationHabitService habitService;
    private AdministrationHabitExecutionService executionService;
    private AdministrationHabitAnalyticsService analyticsService;
    private AdministrationHabitInsightService insightService;
    private AdministrationHabitReminderService reminderService;
    private AdministrationHabitGroupService groupService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        habitService = mock(AdministrationHabitService.class);
        executionService = mock(AdministrationHabitExecutionService.class);
        analyticsService = mock(AdministrationHabitAnalyticsService.class);
        insightService = mock(AdministrationHabitInsightService.class);
        reminderService = mock(AdministrationHabitReminderService.class);
        groupService = mock(AdministrationHabitGroupService.class);

        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        AdministrationHabitController habitController = new AdministrationHabitController(
                habitService, executionService, analyticsService, insightService, reminderService
        );
        AdministrationHabitGroupController groupController = new AdministrationHabitGroupController(groupService);

        mockMvc = MockMvcBuilders.standaloneSetup(habitController, groupController)
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetHabits() throws Exception {
        UUID id = UUID.randomUUID();
        when(habitService.getHabits(null, null, null, null)).thenReturn(List.of(
                new HabitDto(
                        id, null, null, null, "Meditation", "Breathing", "Mindfulness",
                        "DAILY", "{}", BigDecimal.valueOf(10), BigDecimal.valueOf(2),
                        "minutes", "HIGH", LocalDate.of(2026, 10, 1), null, "ACTIVE",
                        1, 10, List.of(), 5, 10, 95.0, null, null
                )
        ));

        mockMvc.perform(get("/api/v1/administration/habits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].name").value("Meditation"))
                .andExpect(jsonPath("$[0].category").value("Mindfulness"));
    }

    @Test
    void testCreateHabit() throws Exception {
        UUID id = UUID.randomUUID();
        CreateHabitRequest req = new CreateHabitRequest(
                "Reading",
                "Read books",
                "Learning",
                "DAILY",
                "{}",
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(2),
                "pages",
                "MEDIUM",
                LocalDate.of(2026, 10, 1),
                null,
                null,
                1,
                15,
                null
        );

        when(habitService.create(any(CreateHabitRequest.class))).thenReturn(
                new HabitDto(
                        id, null, null, null, "Reading", "Read books", "Learning",
                        "DAILY", "{}", BigDecimal.valueOf(10), BigDecimal.valueOf(2),
                        "pages", "MEDIUM", LocalDate.of(2026, 10, 1), null, "ACTIVE",
                        1, 15, List.of(), 0, 0, 0.0, null, null
                )
        );

        mockMvc.perform(post("/api/v1/administration/habits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Reading"));
    }

    @Test
    void testGetOverview() throws Exception {
        TodayHabitViewDto today = new TodayHabitViewDto(
                LocalDate.of(2026, 11, 1), 4, 3, 0, 1, 0, 0, 75.0,
                List.of(), List.of(), List.of()
        );
        WeekHabitViewDto week = new WeekHabitViewDto(
                LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 1),
                28, 24, 1, 2, 1, 88.0,
                List.of(), List.of(), List.of()
        );

        when(executionService.getTodayView(any())).thenReturn(today);
        when(executionService.getWeekView(any())).thenReturn(week);
        when(habitService.getHabits(eq("ACTIVE"), any(), any(), any())).thenReturn(List.of());
        when(insightService.generateInsights()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/administration/habits/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.today.completionPercentage").value(75.0))
                .andExpect(jsonPath("$.week.completionPercentage").value(88.0));
    }

    @Test
    void testCompleteOccurrence() throws Exception {
        UUID id = UUID.randomUUID();
        when(executionService.completeOccurrence(eq(id), any())).thenReturn(
                new HabitOccurrenceDto(
                        id, UUID.randomUUID(), "Meditation", "Mindfulness",
                        LocalDate.of(2026, 11, 1), BigDecimal.valueOf(10), BigDecimal.valueOf(2),
                        "minutes", "COMPLETED", BigDecimal.valueOf(10), null, null, null,
                        "HIGH", 10, null, null
                )
        );

        mockMvc.perform(post("/api/v1/administration/habits/occurrences/" + id + "/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actualValue\": 10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.actualValue").value(10));
    }

    @Test
    void testGetCalendar() throws Exception {
        when(analyticsService.getCalendarDays(any(), any())).thenReturn(List.of(
                new CalendarDayDto(
                        LocalDate.of(2026, 11, 1), 4, 4, 0, 0, 0, "4/4", 100.0, "HIGH"
                )
        ));

        mockMvc.perform(get("/api/v1/administration/habits/calendar")
                        .param("year", "2026")
                        .param("month", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ratio").value("4/4"))
                .andExpect(jsonPath("$[0].statusLevel").value("HIGH"));
    }

    @Test
    void testGetWeeklyReport() throws Exception {
        when(analyticsService.getWeeklyReport(any())).thenReturn(
                new HabitWeeklyReportDto(
                        LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 1),
                        25, 22, 1, 1, 1, 88.0, 80.0, 8.0,
                        "Morning Meditation", "Strength Workout", 4,
                        java.util.Map.of("minutes", BigDecimal.valueOf(120)),
                        List.of()
                )
        );

        mockMvc.perform(get("/api/v1/administration/habits/reports/weekly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionPercentage").value(88.0))
                .andExpect(jsonPath("$.percentageDelta").value(8.0))
                .andExpect(jsonPath("$.bestPerformingHabit").value("Morning Meditation"));
    }

    @Test
    void testHabitGroupsEndpoints() throws Exception {
        UUID groupId = UUID.randomUUID();
        when(groupService.getGroups()).thenReturn(List.of(
                new HabitGroupDto(
                        groupId, null, "Morning Routine", "Focus & Health", 1, 2, List.of(), null, null
                )
        ));

        mockMvc.perform(get("/api/v1/administration/habit-groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Morning Routine"));
    }
}

