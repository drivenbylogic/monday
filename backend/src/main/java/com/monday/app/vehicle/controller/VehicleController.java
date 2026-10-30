package com.monday.app.vehicle.controller;

import com.monday.app.vehicle.dto.*;
import com.monday.app.vehicle.entity.Vehicle;
import com.monday.app.vehicle.entity.VehicleFuelLog;
import com.monday.app.vehicle.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;
    private final VehicleOverviewService overviewService;
    private final VehicleDocumentService documentService;
    private final VehicleOdometerService odometerService;
    private final VehicleFuelService fuelService;
    private final VehicleServiceLedgerService serviceLedgerService;
    private final VehicleMaintenanceScheduleService scheduleService;
    private final VehicleAnalyticsService analyticsService;
    private final VehicleTimelineService timelineService;
    private final VehicleTaskService taskService;

    @Autowired
    public VehicleController(VehicleService vehicleService,
                             VehicleOverviewService overviewService,
                             VehicleDocumentService documentService,
                             VehicleOdometerService odometerService,
                             VehicleFuelService fuelService,
                             VehicleServiceLedgerService serviceLedgerService,
                             VehicleMaintenanceScheduleService scheduleService,
                             VehicleAnalyticsService analyticsService,
                             VehicleTimelineService timelineService,
                             VehicleTaskService taskService) {
        this.vehicleService = vehicleService;
        this.overviewService = overviewService;
        this.documentService = documentService;
        this.odometerService = odometerService;
        this.fuelService = fuelService;
        this.serviceLedgerService = serviceLedgerService;
        this.scheduleService = scheduleService;
        this.analyticsService = analyticsService;
        this.timelineService = timelineService;
        this.taskService = taskService;
    }

    // Overload for standalone unit testing
    public VehicleController(VehicleService vehicleService) {
        this(vehicleService, null, null, null, null, null, null, null, null, null);
    }

    // =========================================================================
    // 1. Fleet & Specifications
    // =========================================================================

    @GetMapping
    public ResponseEntity<Map<String, Object>> getFleet(@RequestParam(required = false, defaultValue = "active") String status) {
        return ResponseEntity.ok(vehicleService.getFleet(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getVehicleSpecs(@PathVariable UUID id) {
        return ResponseEntity.ok(vehicleService.getVehicleSpecs(id));
    }

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

    @PutMapping("/{vehicleId}/primary")
    public ResponseEntity<Map<String, Object>> setPrimaryVehicle(
            @PathVariable UUID vehicleId,
            @RequestBody Map<String, Object> body) {
        boolean isPrimary = Boolean.TRUE.equals(body.get("isPrimary"));
        return ResponseEntity.ok(vehicleService.setPrimaryVehicle(vehicleId, isPrimary));
    }

    // =========================================================================
    // 2. Section A — Garage Dashboard & Overview
    // =========================================================================

    @GetMapping("/{vehicleId}/overview")
    public ResponseEntity<VehicleOverviewDto> getOverview(@PathVariable UUID vehicleId) {
        return ResponseEntity.ok(overviewService.getOverview(vehicleId));
    }

    // =========================================================================
    // 3. Section B — Flexible Document Management
    // =========================================================================

    @GetMapping("/{vehicleId}/documents")
    public ResponseEntity<Map<String, Object>> getDocuments(
            @PathVariable UUID vehicleId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        if (documentService != null) {
            List<VehicleDocumentDto> docs = documentService.getDocuments(vehicleId, type, status, search);
            boolean allValid = docs.stream().noneMatch(VehicleDocumentDto::isExpired);
            return ResponseEntity.ok(Map.of(
                    "vehicleId", vehicleId.toString(),
                    "documents", docs,
                    "data", docs,
                    "totalCount", docs.size(),
                    "allValid", allValid
            ));
        }
        return ResponseEntity.ok(vehicleService.getDocuments(vehicleId));
    }

    @GetMapping("/{vehicleId}/documents/{docId}")
    public ResponseEntity<VehicleDocumentDto> getDocument(
            @PathVariable UUID vehicleId,
            @PathVariable UUID docId) {
        return ResponseEntity.ok(documentService.getDocument(docId));
    }

    @PostMapping("/{vehicleId}/documents")
    public ResponseEntity<VehicleDocumentDto> createDocument(
            @PathVariable UUID vehicleId,
            @RequestBody CreateDocumentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.createDocument(vehicleId, req));
    }

    @PutMapping("/{vehicleId}/documents/{docId}")
    public ResponseEntity<VehicleDocumentDto> updateDocument(
            @PathVariable UUID vehicleId,
            @PathVariable UUID docId,
            @RequestBody UpdateDocumentRequest req) {
        return ResponseEntity.ok(documentService.updateDocument(docId, req));
    }

    @PatchMapping("/{vehicleId}/documents/{docId}")
    public ResponseEntity<VehicleDocumentDto> patchDocument(
            @PathVariable UUID vehicleId,
            @PathVariable UUID docId,
            @RequestBody UpdateDocumentRequest req) {
        return ResponseEntity.ok(documentService.updateDocument(docId, req));
    }

    @DeleteMapping("/{vehicleId}/documents/{docId}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable UUID vehicleId,
            @PathVariable UUID docId) {
        documentService.deleteDocument(docId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{vehicleId}/documents/{docId}/renew")
    public ResponseEntity<VehicleDocumentDto> renewDocument(
            @PathVariable UUID vehicleId,
            @PathVariable UUID docId,
            @RequestBody RenewDocumentRequest req) {
        return ResponseEntity.ok(documentService.renewDocument(docId, req));
    }

    @GetMapping("/{vehicleId}/documents/{documentId}/download")
    public ResponseEntity<Map<String, Object>> downloadDocument(
            @PathVariable UUID vehicleId,
            @PathVariable String documentId) {
        return ResponseEntity.ok(vehicleService.downloadDocument(vehicleId, documentId));
    }

    @GetMapping("/{vehicleId}/insurance")
    public ResponseEntity<Map<String, Object>> getInsurance(@PathVariable UUID vehicleId) {
        return ResponseEntity.ok(vehicleService.getInsurance(vehicleId));
    }

    @PostMapping("/{vehicleId}/insurance/renew")
    public ResponseEntity<Map<String, Object>> renewInsurance(
            @PathVariable UUID vehicleId,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(vehicleService.renewInsurance(vehicleId, body));
    }

    // =========================================================================
    // 4. Section C — Vehicle Analytics & Odometer
    // =========================================================================

    @GetMapping("/{vehicleId}/analytics")
    public ResponseEntity<VehicleAnalyticsDto> getAnalytics(
            @PathVariable UUID vehicleId,
            @RequestParam(required = false, defaultValue = "ALL") String period) {
        return ResponseEntity.ok(analyticsService.getAnalytics(vehicleId, period));
    }

    @GetMapping("/{vehicleId}/odometer-readings")
    public ResponseEntity<List<OdometerReadingDto>> getOdometerReadings(@PathVariable UUID vehicleId) {
        return ResponseEntity.ok(odometerService.getReadings(vehicleId));
    }

    @PostMapping("/{vehicleId}/odometer-readings")
    public ResponseEntity<OdometerReadingDto> recordOdometerReading(
            @PathVariable UUID vehicleId,
            @RequestBody RecordOdometerRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(odometerService.recordReading(vehicleId, req));
    }

    @GetMapping(value = {"/{vehicleId}/fuel", "/{vehicleId}/fuel-logs"})
    public ResponseEntity<Map<String, Object>> getFuelLogs(
            @PathVariable UUID vehicleId,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        if (fuelService != null) {
            List<VehicleFuelLog> logs = fuelService.getFuelLogs(vehicleId, limit);
            return ResponseEntity.ok(Map.of(
                    "data", logs,
                    "totalCount", logs.size()
            ));
        }
        return ResponseEntity.ok(vehicleService.getFuelLogs(vehicleId, limit));
    }

    @PostMapping(value = {"/{vehicleId}/fuel", "/{vehicleId}/fuel-logs"})
    public ResponseEntity<Object> logFuel(
            @PathVariable UUID vehicleId,
            @RequestBody Map<String, Object> body) {
        if (fuelService != null) {
            VehicleFuelLog saved = fuelService.logFuel(vehicleId, body);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.logFuel(vehicleId, body));
    }

    @GetMapping("/{vehicleId}/analytics/outlay")
    public ResponseEntity<Map<String, Object>> getOperationalOutlay(
            @PathVariable UUID vehicleId,
            @RequestParam(required = false) String fiscalYear) {
        return ResponseEntity.ok(vehicleService.getOperationalOutlay(vehicleId, fiscalYear));
    }

    @GetMapping("/{vehicleId}/telemetry")
    public ResponseEntity<Map<String, Object>> getTelemetry(@PathVariable UUID vehicleId) {
        return ResponseEntity.ok(vehicleService.getVehicleTelemetry(vehicleId));
    }

    @PostMapping("/{vehicleId}/diagnostics/scan")
    public ResponseEntity<Map<String, Object>> scanDiagnostics(@PathVariable UUID vehicleId) {
        return ResponseEntity.ok(vehicleService.scanDiagnostics(vehicleId));
    }

    // =========================================================================
    // 5. Section D — Service Ledger & Maintenance Schedules
    // =========================================================================

    @GetMapping("/{vehicleId}/service-records")
    public ResponseEntity<List<ServiceRecordDto>> getServiceRecords(
            @PathVariable UUID vehicleId,
            @RequestParam(required = false, defaultValue = "all") String type,
            @RequestParam(required = false, defaultValue = "50") int limit) {
        return ResponseEntity.ok(serviceLedgerService.getServiceRecords(vehicleId, type, limit));
    }

    @GetMapping("/{vehicleId}/service-records/{recordId}")
    public ResponseEntity<ServiceRecordDto> getServiceRecord(
            @PathVariable UUID vehicleId,
            @PathVariable UUID recordId) {
        return ResponseEntity.ok(serviceLedgerService.getServiceRecord(recordId));
    }

    @PostMapping("/{vehicleId}/service-records")
    public ResponseEntity<ServiceRecordDto> createServiceRecord(
            @PathVariable UUID vehicleId,
            @RequestBody CreateServiceRecordRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceLedgerService.createServiceRecord(vehicleId, req));
    }

    @PutMapping("/{vehicleId}/service-records/{recordId}")
    public ResponseEntity<ServiceRecordDto> updateServiceRecord(
            @PathVariable UUID vehicleId,
            @PathVariable UUID recordId,
            @RequestBody UpdateServiceRecordRequest req) {
        return ResponseEntity.ok(serviceLedgerService.updateServiceRecord(recordId, req));
    }

    @DeleteMapping("/{vehicleId}/service-records/{recordId}")
    public ResponseEntity<Void> deleteServiceRecord(
            @PathVariable UUID vehicleId,
            @PathVariable UUID recordId) {
        serviceLedgerService.deleteServiceRecord(recordId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{vehicleId}/maintenance")
    public ResponseEntity<Map<String, Object>> getMaintenanceLedger(
            @PathVariable UUID vehicleId,
            @RequestParam(required = false, defaultValue = "all") String type,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        return ResponseEntity.ok(vehicleService.getMaintenanceLedger(vehicleId, type, limit));
    }

    @PostMapping("/{vehicleId}/maintenance")
    public ResponseEntity<Map<String, Object>> logMaintenance(
            @PathVariable UUID vehicleId,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.logMaintenance(vehicleId, body));
    }

    @GetMapping("/{vehicleId}/maintenance/{recordId}/scope")
    public ResponseEntity<Map<String, Object>> getServiceScope(
            @PathVariable UUID vehicleId,
            @PathVariable String recordId) {
        return ResponseEntity.ok(vehicleService.getServiceScope(vehicleId, recordId));
    }

    @GetMapping("/{vehicleId}/maintenance-schedules")
    public ResponseEntity<List<MaintenanceScheduleDto>> getMaintenanceSchedules(@PathVariable UUID vehicleId) {
        return ResponseEntity.ok(scheduleService.getSchedules(vehicleId));
    }

    @PostMapping("/{vehicleId}/maintenance-schedules")
    public ResponseEntity<MaintenanceScheduleDto> createMaintenanceSchedule(
            @PathVariable UUID vehicleId,
            @RequestBody CreateMaintenanceScheduleRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.createSchedule(vehicleId, req));
    }

    @PutMapping("/{vehicleId}/maintenance-schedules/{scheduleId}")
    public ResponseEntity<MaintenanceScheduleDto> updateMaintenanceSchedule(
            @PathVariable UUID vehicleId,
            @PathVariable UUID scheduleId,
            @RequestBody UpdateMaintenanceScheduleRequest req) {
        return ResponseEntity.ok(scheduleService.updateSchedule(scheduleId, req));
    }

    @DeleteMapping("/{vehicleId}/maintenance-schedules/{scheduleId}")
    public ResponseEntity<Void> deleteMaintenanceSchedule(
            @PathVariable UUID vehicleId,
            @PathVariable UUID scheduleId) {
        scheduleService.deleteSchedule(scheduleId);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // 6. Tasks & Obligations
    // =========================================================================

    @GetMapping("/{vehicleId}/tasks")
    public ResponseEntity<Map<String, Object>> getTasks(@PathVariable UUID vehicleId) {
        if (taskService != null) {
            List<VehicleTaskDto> tasks = taskService.getTasks(vehicleId);
            long pendingCount = tasks.stream().filter(t -> !Boolean.TRUE.equals(t.completed())).count();
            return ResponseEntity.ok(Map.of(
                    "vehicleId", vehicleId.toString(),
                    "tasks", tasks,
                    "data", tasks,
                    "pendingCount", pendingCount
            ));
        }
        return ResponseEntity.ok(vehicleService.getTasks(vehicleId));
    }

    @PatchMapping("/{vehicleId}/tasks/{taskId}")
    public ResponseEntity<Map<String, Object>> toggleTask(
            @PathVariable UUID vehicleId,
            @PathVariable String taskId,
            @RequestBody Map<String, Object> body) {
        boolean completed = Boolean.TRUE.equals(body.get("completed"));
        if (taskService != null) {
            try {
                VehicleTaskDto updated = taskService.toggleTask(vehicleId, UUID.fromString(taskId), completed);
                return ResponseEntity.ok(Map.of("id", taskId, "completed", updated.completed()));
            } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(vehicleService.toggleTask(vehicleId, taskId, completed));
    }

    @PostMapping("/{vehicleId}/tasks")
    public ResponseEntity<Object> createTask(
            @PathVariable UUID vehicleId,
            @RequestBody Map<String, Object> body) {
        if (taskService != null && body.containsKey("title")) {
            CreateVehicleTaskRequest req = new CreateVehicleTaskRequest(
                    (String) body.get("title"),
                    (String) body.get("metaText"),
                    (String) body.get("priority"),
                    (String) body.get("linkedModule")
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(vehicleId, req));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.createTask(vehicleId, body));
    }

    @DeleteMapping("/{vehicleId}/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable UUID vehicleId,
            @PathVariable UUID taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{vehicleId}/obligations")
    public ResponseEntity<List<VehicleObligationDto>> getObligations(@PathVariable UUID vehicleId) {
        return ResponseEntity.ok(taskService.getObligations(vehicleId));
    }

    @PostMapping("/{vehicleId}/obligations")
    public ResponseEntity<VehicleObligationDto> createObligation(
            @PathVariable UUID vehicleId,
            @RequestBody CreateVehicleObligationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createObligation(vehicleId, req));
    }

    @PutMapping("/{vehicleId}/obligations/{obligationId}")
    public ResponseEntity<VehicleObligationDto> updateObligation(
            @PathVariable UUID vehicleId,
            @PathVariable UUID obligationId,
            @RequestBody CreateVehicleObligationRequest req) {
        return ResponseEntity.ok(taskService.updateObligation(obligationId, req));
    }

    @DeleteMapping("/{vehicleId}/obligations/{obligationId}")
    public ResponseEntity<Void> deleteObligation(
            @PathVariable UUID vehicleId,
            @PathVariable UUID obligationId) {
        taskService.deleteObligation(obligationId);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // 7. Section E — Vehicle Timeline
    // =========================================================================

    @GetMapping("/{vehicleId}/timeline")
    public ResponseEntity<List<VehicleTimelineItemDto>> getTimeline(
            @PathVariable UUID vehicleId,
            @RequestParam(required = false, defaultValue = "50") int limit) {
        return ResponseEntity.ok(timelineService.getTimeline(vehicleId, limit));
    }
}
