package com.monday.app.career.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.app.career.entity.CareerPlan;
import com.monday.app.career.service.CareerService;
import com.monday.app.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CareerControllerTest {

    private MockMvc mockMvc;
    private CareerService careerService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        careerService = mock(CareerService.class);
        objectMapper = new ObjectMapper();

        mockMvc = MockMvcBuilders.standaloneSetup(new CareerController(careerService, objectMapper))
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetCurrentPlan_Found() throws Exception {
        CareerPlan plan = new CareerPlan();
        plan.setId("level-up");
        plan.setName("Level up");
        plan.setDescription("12-month transition plan");
        plan.setStartDate(LocalDate.of(2026, 11, 1));
        plan.setTargetDate(LocalDate.of(2027, 10, 31));
        plan.setPlanData("{\"id\":\"level-up\",\"name\":\"Level up\"}");

        when(careerService.getCurrentPlan()).thenReturn(Optional.of(plan));

        mockMvc.perform(get("/api/v1/career/plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("level-up"))
                .andExpect(jsonPath("$.name").value("Level up"));
    }

    @Test
    void testGetCurrentPlan_NotFound() throws Exception {
        when(careerService.getCurrentPlan()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/career/plan"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testImportPlan() throws Exception {
        CareerPlan plan = new CareerPlan();
        plan.setId("level-up");
        plan.setName("Level up");
        plan.setPlanData("{\"id\":\"level-up\",\"name\":\"Level up\"}");

        when(careerService.importPlan(any(JsonNode.class))).thenReturn(plan);

        mockMvc.perform(post("/api/v1/career/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"level-up\",\"name\":\"Level up\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("level-up"));
    }

    @Test
    void testToggleTask() throws Exception {
        CareerPlan plan = new CareerPlan();
        plan.setId("level-up");
        plan.setName("Level up");
        plan.setPlanData("{\"id\":\"level-up\",\"name\":\"Level up\"}");

        when(careerService.toggleTask(eq("task-0009"), any())).thenReturn(plan);

        mockMvc.perform(put("/api/v1/career/tasks/task-0009/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("level-up"));
    }

    @Test
    void testClearCareerData() throws Exception {
        mockMvc.perform(delete("/api/v1/career/plan"))
                .andExpect(status().isNoContent());
    }
}
