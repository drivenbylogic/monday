package com.monday.app.project.controller;

import com.monday.app.project.entity.Task;
import com.monday.app.project.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@Valid @RequestBody Task task) {
        Task created = taskService.create(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<Task>> getTasks(
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) UUID milestoneId,
            @RequestParam(required = false) UUID eventId,
            @RequestParam(required = false) String status) {
        
        if (projectId != null) {
            return ResponseEntity.ok(taskService.getByProjectId(projectId));
        } else if (milestoneId != null) {
            return ResponseEntity.ok(taskService.getByMilestoneId(milestoneId));
        } else if (eventId != null) {
            return ResponseEntity.ok(taskService.getByEventId(eventId));
        } else if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(taskService.getByStatus(status));
        }
        
        return ResponseEntity.ok(taskService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable UUID id) {
        return ResponseEntity.ok(taskService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable UUID id, @Valid @RequestBody Task task) {
        return ResponseEntity.ok(taskService.update(id, task));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable UUID id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/occurrences")
    public ResponseEntity<Task> generateOccurrence(@PathVariable UUID id, @RequestParam Instant dueAt) {
        Task occurrence = taskService.generateOccurrence(id, dueAt);
        return ResponseEntity.status(HttpStatus.CREATED).body(occurrence);
    }

    @GetMapping("/{id}/occurrences")
    public ResponseEntity<List<Task>> getOccurrences(@PathVariable UUID id) {
        return ResponseEntity.ok(taskService.getOccurrences(id));
    }
}
