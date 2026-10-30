package com.monday.app.agenda.controller;

import com.monday.app.agenda.dto.*;
import com.monday.app.agenda.service.HomeDashboardService;
import com.monday.app.shared.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HomeDashboardControllerTest {

    private MockMvc mockMvc;
    private HomeDashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = mock(HomeDashboardService.class);
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(new HomeDashboardController(dashboardService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getDashboard_returnsCompleteDashboardSnapshot() throws Exception {
        LocalDate date = LocalDate.of(2026, 10, 26);
        HomeDashboardResponse mockResponse = new HomeDashboardResponse(
                date,
                new ScheduleDashboardDto(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), 0, 0, 0),
                new TaskOverviewDto(2, 1, 1, 1, 90, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList()),
                new ProjectOverviewDto(3, Collections.emptyList(), Collections.emptyList(), new DeepWorkSummaryDto(120, 2.0, 5.0, 1, 100)),
                new FinanceObligationsDto(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), BigDecimal.ZERO, Collections.emptyList(), new CreditUtilizationSummaryDto(BigDecimal.ZERO, BigDecimal.ZERO, null, false), Collections.emptyList()),
                new BudgetSpendingDto(BigDecimal.valueOf(75000), BigDecimal.valueOf(32000), BigDecimal.valueOf(43000), 42.6, Collections.emptyList(), Collections.emptyList()),
                new WorkCapacityDto(480, 0, 480, 90, false),
                Instant.now()
        );

        when(dashboardService.getDashboard(any(), any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/home/dashboard?date=2026-10-26")
                        .header("X-Timezone", "Asia/Kolkata")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-10-26"))
                .andExpect(jsonPath("$.tasks.dueTodayCount").value(2))
                .andExpect(jsonPath("$.tasks.overdueCount").value(1))
                .andExpect(jsonPath("$.capacity.availableWorkMinutes").value(480));
    }
}
