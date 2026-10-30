package com.monday.app.administration.controller;

import com.monday.app.administration.dto.AppointmentDto;
import com.monday.app.administration.dto.CreateAppointmentRequest;
import com.monday.app.administration.dto.UpdateAppointmentRequest;
import com.monday.app.administration.service.AdministrationAppointmentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/administration/appointments", "/api/administration/appointments"})
public class AdministrationAppointmentController {

    private final AdministrationAppointmentService service;

    public AdministrationAppointmentController(AdministrationAppointmentService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<AppointmentDto>> getAppointments(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(service.getAppointments(query, eventType, status, from, to));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<AppointmentDto> create(@Valid @RequestBody CreateAppointmentRequest req) {
        AppointmentDto created = service.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AppointmentDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAppointmentRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AppointmentDto> updateStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String status = body.getOrDefault("status", "SCHEDULED");
        return ResponseEntity.ok(service.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
