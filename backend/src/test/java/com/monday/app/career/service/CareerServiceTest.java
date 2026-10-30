package com.monday.app.career.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.app.career.entity.CareerPlan;
import com.monday.app.career.repository.CareerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CareerServiceTest {

    private CareerRepository careerRepository;
    private ObjectMapper objectMapper;
    private CareerService careerService;

    @BeforeEach
    void setUp() {
        careerRepository = mock(CareerRepository.class);
        objectMapper = new ObjectMapper();
        careerService = new CareerService(careerRepository, objectMapper);
    }

    @Test
    void testImportPlan_Successful() throws Exception {
        String jsonStr = """
            {
                "id": "level-up",
                "name": "Level up",
                "description": "12-month career transition plan",
                "startDate": "2026-11-01",
                "targetDate": "2027-10-31",
                "color": "violet",
                "viewStyle": "board",
                "milestones": [
                    {
                        "id": "m1",
                        "name": "M1 · Baseline",
                        "dueDate": "2026-11-08",
                        "tasks": [
                            {
                                "id": "task-1",
                                "title": "Assess current skills",
                                "isCompleted": false
                            }
                        ]
                    }
                ]
            }
            """;
        JsonNode rootNode = objectMapper.readTree(jsonStr);

        CareerPlan result = careerService.importPlan(rootNode);

        assertNotNull(result);
        assertEquals("level-up", result.getId());
        assertEquals("Level up", result.getName());
        assertEquals(LocalDate.of(2026, 11, 1), result.getStartDate());
        assertEquals(LocalDate.of(2027, 10, 31), result.getTargetDate());
        assertEquals("violet", result.getColor());
        assertEquals("board", result.getViewStyle());

        verify(careerRepository, times(1)).save(any(CareerPlan.class));
        verify(careerRepository, times(1)).syncToCoreTables(any(CareerPlan.class), eq(rootNode));
    }

    @Test
    void testToggleTask_Successful() {
        CareerPlan dummy = new CareerPlan();
        dummy.setId("level-up");
        when(careerRepository.updateTaskCompletion("task-1", true)).thenReturn(Optional.of(dummy));

        CareerPlan updated = careerService.toggleTask("task-1", true);

        assertNotNull(updated);
        assertEquals("level-up", updated.getId());
        verify(careerRepository, times(1)).updateTaskCompletion("task-1", true);
    }

    @Test
    void testClearCareerData() {
        careerService.clearCareerData();

        verify(careerRepository, times(1)).deleteAll();
        verify(careerRepository, times(1)).deleteCoreCareerData();
    }

    @Test
    void testLoadSamplePlan() {
        JsonNode sample = careerService.loadSamplePlan();
        assertNotNull(sample);
        assertEquals("level-up", sample.path("id").asText());
        assertEquals("Level up", sample.path("name").asText());
        assertTrue(sample.path("milestones").isArray());
        assertEquals(9, sample.path("milestones").size());
    }
}
