package com.monday.app.administration.controller;

import com.monday.app.administration.dto.CreateObligationRequest;
import com.monday.app.administration.dto.ObligationDto;
import com.monday.app.administration.dto.UpdateObligationRequest;
import com.monday.app.administration.service.AdministrationObligationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/administration/obligations", "/api/administration/obligations"})
public class AdministrationObligationController {

    private final AdministrationObligationService service;

    public AdministrationObligationController(AdministrationObligationService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ObligationDto>> getObligations(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean archived) {
        return ResponseEntity.ok(service.getObligations(query, category, status, archived));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ObligationDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<ObligationDto> create(@Valid @RequestBody CreateObligationRequest req) {
        ObligationDto created = service.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ObligationDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateObligationRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @PostMapping("/{id}/advance")
    public ResponseEntity<ObligationDto> advanceRecurrence(@PathVariable UUID id) {
        return ResponseEntity.ok(service.advanceRecurrence(id));
    }

    @PostMapping("/{id}/reviewed")
    public ResponseEntity<ObligationDto> markReviewed(@PathVariable UUID id) {
        return ResponseEntity.ok(service.markReviewed(id));
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
