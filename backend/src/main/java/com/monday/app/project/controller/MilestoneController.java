package com.monday.app.project.controller;

import com.monday.app.project.entity.Milestone;
import com.monday.app.project.service.MilestoneService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/milestones")
public class MilestoneController {

    private final MilestoneService milestoneService;

    public MilestoneController(MilestoneService milestoneService) {
        this.milestoneService = milestoneService;
    }

    @PostMapping
    public ResponseEntity<Milestone> createMilestone(@Valid @RequestBody Milestone milestone) {
        Milestone created = milestoneService.create(milestone);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<Milestone>> getMilestonesByProject(@RequestParam UUID projectId) {
        return ResponseEntity.ok(milestoneService.getByProjectId(projectId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Milestone> getMilestoneById(@PathVariable UUID id) {
        return ResponseEntity.ok(milestoneService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Milestone> updateMilestone(@PathVariable UUID id, @Valid @RequestBody Milestone milestone) {
        return ResponseEntity.ok(milestoneService.update(id, milestone));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMilestone(@PathVariable UUID id) {
        milestoneService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
