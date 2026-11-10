package com.monday.app.project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.app.project.entity.Task;
import com.monday.app.project.service.TaskService;
import com.monday.app.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TaskControllerTest {

    private MockMvc mockMvc;
    private TaskService taskService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        taskService = mock(TaskService.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(new TaskController(taskService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void putUpdateTask_withOnlyStatus_returnsOk() throws Exception {
        UUID taskId = UUID.randomUUID();
        Task updatedTask = new Task();
        updatedTask.setId(taskId);
        updatedTask.setTitle("Important Task");
        updatedTask.setStatus("COMPLETED");

        when(taskService.update(eq(taskId), any(Task.class))).thenReturn(updatedTask);

        String payload = """
                {
                    "status": "COMPLETED"
                }
                """;

        mockMvc.perform(put("/api/v1/tasks/" + taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.title").value("Important Task"));

        verify(taskService).update(eq(taskId), any(Task.class));
    }

    @Test
    void patchTask_withOnlyStatus_returnsOk() throws Exception {
        UUID taskId = UUID.randomUUID();
        Task updatedTask = new Task();
        updatedTask.setId(taskId);
        updatedTask.setTitle("Important Task");
        updatedTask.setStatus("COMPLETED");

        when(taskService.update(eq(taskId), any(Task.class))).thenReturn(updatedTask);

        String payload = """
                {
                    "status": "COMPLETED"
                }
                """;

        mockMvc.perform(patch("/api/v1/tasks/" + taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(taskService).update(eq(taskId), any(Task.class));
    }

    @Test
    void deleteTask_validId_returnsNoContent() throws Exception {
        UUID taskId = UUID.randomUUID();
        doNothing().when(taskService).delete(taskId);

        mockMvc.perform(delete("/api/v1/tasks/" + taskId))
                .andExpect(status().isNoContent());

        verify(taskService).delete(taskId);
    }
}

