package com.monday.app.project.controller;

import com.monday.app.project.entity.Project;
import com.monday.app.project.entity.Milestone;
import com.monday.app.project.entity.Task;
import com.monday.app.project.dto.ProjectBasicResponse;
import com.monday.app.project.dto.ProjectMetricsResponse;
import com.monday.app.project.dto.ProjectDetailedResponse;
import com.monday.app.project.dto.VelocityMetricsResponse;
import com.monday.app.project.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<Project> createProject(@Valid @RequestBody Project project) {
        Project created = projectService.create(project);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<ProjectBasicResponse>> getAllProjects() {
        return ResponseEntity.ok(projectService.getBasicProjects());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProjectById(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Project> updateProject(@PathVariable UUID id, @Valid @RequestBody Project project) {
        return ResponseEntity.ok(projectService.update(id, project));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/metrics")
    public ResponseEntity<ProjectMetricsResponse> getMetrics() {
        return ResponseEntity.ok(projectService.getProjectMetrics());
    }

    @GetMapping("/velocity")
    public ResponseEntity<VelocityMetricsResponse> getVelocity() {
        return ResponseEntity.ok(projectService.getVelocityMetrics());
    }

    @GetMapping("/{id}/detailed")
    public ResponseEntity<ProjectDetailedResponse> getDetailedProject(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.getDetailedProject(id));
    }

    @GetMapping("/milestones/impending")
    public ResponseEntity<List<Milestone>> getImpendingMilestones() {
        return ResponseEntity.ok(projectService.getImpendingMilestones());
    }

    @GetMapping("/milestones/{id}/tasks")
    public ResponseEntity<List<Task>> getTasksForMilestone(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.getTasksByMilestoneId(id));
    }

    @GetMapping("/tasks/execution-queue")
    public ResponseEntity<List<Task>> getExecutionQueue() {
        return ResponseEntity.ok(projectService.getExecutionQueue());
    }
}
