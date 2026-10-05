package com.monday.app.vehicle.controller;

import com.monday.app.vehicle.entity.Vehicle;
import com.monday.app.vehicle.entity.VehicleMaintenanceSchedule;
import com.monday.app.vehicle.entity.VehicleObligation;
import com.monday.app.vehicle.entity.VehicleServiceRecord;
import com.monday.app.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    // --- Fleet & Specifications ---
    
    @GetMapping
    public ResponseEntity<java.util.Map<String, Object>> getFleet(@RequestParam(required = false, defaultValue = "active") String status) {
        return ResponseEntity.ok(vehicleService.getFleet(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<java.util.Map<String, Object>> getVehicleSpecs(@PathVariable UUID id) {
        return ResponseEntity.ok(vehicleService.getVehicleSpecs(id));
    }

    // --- Telemetry & Diagnostics ---

    @GetMapping("/{vehicleId}/telemetry")
    public ResponseEntity<java.util.Map<String, Object>> getTelemetry(@PathVariable UUID vehicleId) {
        return ResponseEntity.ok(vehicleService.getVehicleTelemetry(vehicleId));
    }

    @PostMapping("/{vehicleId}/diagnostics/scan")
    public ResponseEntity<java.util.Map<String, Object>> scanDiagnostics(@PathVariable UUID vehicleId) {
        return ResponseEntity.ok(vehicleService.scanDiagnostics(vehicleId));
    }

    // --- Maintenance & Service ---

    @GetMapping("/{vehicleId}/maintenance")
    public ResponseEntity<java.util.Map<String, Object>> getMaintenanceLedger(
            @PathVariable UUID vehicleId,
            @RequestParam(required = false, defaultValue = "all") String type,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        return ResponseEntity.ok(vehicleService.getMaintenanceLedger(vehicleId, type, limit));
    }

    @PostMapping("/{vehicleId}/maintenance")
    public ResponseEntity<java.util.Map<String, Object>> logMaintenance(
            @PathVariable UUID vehicleId,
            @RequestBody java.util.Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.logMaintenance(vehicleId, body));
    }

    @GetMapping("/{vehicleId}/maintenance/{recordId}/scope")
    public ResponseEntity<java.util.Map<String, Object>> getServiceScope(
            @PathVariable UUID vehicleId,
            @PathVariable String recordId) {
        return ResponseEntity.ok(vehicleService.getServiceScope(vehicleId, recordId));
    }

    // --- Fuel & Outlay ---

    @GetMapping("/{vehicleId}/analytics/outlay")
    public ResponseEntity<java.util.Map<String, Object>> getOperationalOutlay(
            @PathVariable UUID vehicleId,
            @RequestParam(required = false) String fiscalYear) {
        return ResponseEntity.ok(vehicleService.getOperationalOutlay(vehicleId, fiscalYear));
    }

    @GetMapping("/{vehicleId}/fuel-logs")
    public ResponseEntity<java.util.Map<String, Object>> getFuelLogs(
            @PathVariable UUID vehicleId,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        return ResponseEntity.ok(vehicleService.getFuelLogs(vehicleId, limit));
    }

    @PostMapping("/{vehicleId}/fuel-logs")
    public ResponseEntity<java.util.Map<String, Object>> logFuel(
            @PathVariable UUID vehicleId,
            @RequestBody java.util.Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.logFuel(vehicleId, body));
    }

    // --- Insurance & Documents ---

    @GetMapping("/{vehicleId}/insurance")
    public ResponseEntity<java.util.Map<String, Object>> getInsurance(@PathVariable UUID vehicleId) {
        return ResponseEntity.ok(vehicleService.getInsurance(vehicleId));
    }

    @PostMapping("/{vehicleId}/insurance/renew")
    public ResponseEntity<java.util.Map<String, Object>> renewInsurance(
            @PathVariable UUID vehicleId,
            @RequestBody java.util.Map<String, Object> body) {
        return ResponseEntity.ok(vehicleService.renewInsurance(vehicleId, body));
    }

    @GetMapping("/{vehicleId}/documents")
    public ResponseEntity<java.util.Map<String, Object>> getDocuments(@PathVariable UUID vehicleId) {
        return ResponseEntity.ok(vehicleService.getDocuments(vehicleId));
    }

    @GetMapping("/{vehicleId}/documents/{documentId}/download")
    public ResponseEntity<java.util.Map<String, Object>> downloadDocument(
            @PathVariable UUID vehicleId,
            @PathVariable String documentId) {
        return ResponseEntity.ok(vehicleService.downloadDocument(vehicleId, documentId));
    }

    // --- Tasks ---

    @GetMapping("/{vehicleId}/tasks")
    public ResponseEntity<java.util.Map<String, Object>> getTasks(@PathVariable UUID vehicleId) {
        return ResponseEntity.ok(vehicleService.getTasks(vehicleId));
    }

    @PatchMapping("/{vehicleId}/tasks/{taskId}")
    public ResponseEntity<java.util.Map<String, Object>> toggleTask(
            @PathVariable UUID vehicleId,
            @PathVariable String taskId,
            @RequestBody java.util.Map<String, Object> body) {
        boolean completed = Boolean.TRUE.equals(body.get("completed"));
        return ResponseEntity.ok(vehicleService.toggleTask(vehicleId, taskId, completed));
    }

    @PostMapping("/{vehicleId}/tasks")
    public ResponseEntity<java.util.Map<String, Object>> createTask(
            @PathVariable UUID vehicleId,
            @RequestBody java.util.Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.createTask(vehicleId, body));
    }

    // --- Core CRUD (Leftovers) ---
    
    @PostMapping
    public ResponseEntity<Vehicle> createVehicle(@Valid @RequestBody Vehicle vehicle) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.createVehicle(vehicle));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Vehicle> updateVehicle(@PathVariable UUID id, @Valid @RequestBody Vehicle vehicle) {
        return ResponseEntity.ok(vehicleService.updateVehicle(id, vehicle));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable UUID id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }
}
