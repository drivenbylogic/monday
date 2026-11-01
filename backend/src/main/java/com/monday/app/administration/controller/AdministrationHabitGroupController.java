package com.monday.app.administration.controller;

import com.monday.app.administration.dto.*;
import com.monday.app.administration.service.AdministrationHabitGroupService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/administration/habit-groups", "/api/administration/habit-groups", "/api/v1/habit-groups", "/api/habit-groups"})
public class AdministrationHabitGroupController {

    private final AdministrationHabitGroupService groupService;

    public AdministrationHabitGroupController(AdministrationHabitGroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public ResponseEntity<List<HabitGroupDto>> getGroups() {
        return ResponseEntity.ok(groupService.getGroups());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HabitGroupDto> getGroupById(@PathVariable UUID id) {
        return ResponseEntity.ok(groupService.getGroupById(id));
    }

    @PostMapping
    public ResponseEntity<HabitGroupDto> createGroup(@Valid @RequestBody CreateHabitGroupRequest req) {
        HabitGroupDto created = groupService.createGroup(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HabitGroupDto> updateGroup(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateHabitGroupRequest req
    ) {
        return ResponseEntity.ok(groupService.updateGroup(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable UUID id) {
        groupService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<List<HabitOccurrenceDto>> executeRoutine(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(groupService.executeGroupRoutine(id, date));
    }
}

