package com.monday.app.administration.controller;

import com.monday.app.administration.dto.CreateImportantDateRequest;
import com.monday.app.administration.dto.ImportantDateDto;
import com.monday.app.administration.dto.UpdateImportantDateRequest;
import com.monday.app.administration.service.AdministrationImportantDateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/administration/important-dates", "/api/administration/important-dates"})
public class AdministrationImportantDateController {

    private final AdministrationImportantDateService service;

    public AdministrationImportantDateController(AdministrationImportantDateService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ImportantDateDto>> getDates(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String dateType,
            @RequestParam(required = false) Boolean archived) {
        return ResponseEntity.ok(service.getDates(query, dateType, archived));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImportantDateDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<ImportantDateDto> create(@Valid @RequestBody CreateImportantDateRequest req) {
        ImportantDateDto created = service.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ImportantDateDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateImportantDateRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<Void> archive(@PathVariable UUID id) {
        service.archive(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/unarchive")
    public ResponseEntity<Void> unarchive(@PathVariable UUID id) {
        service.unarchive(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
