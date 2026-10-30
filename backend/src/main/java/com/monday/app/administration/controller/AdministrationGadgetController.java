package com.monday.app.administration.controller;

import com.monday.app.administration.dto.*;
import com.monday.app.administration.service.AdministrationGadgetService;
import com.monday.app.administration.service.AdministrationMaintenanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/administration/gadgets", "/api/administration/gadgets"})
public class AdministrationGadgetController {

    private final AdministrationGadgetService gadgetService;
    private final AdministrationMaintenanceService maintenanceService;

    public AdministrationGadgetController(AdministrationGadgetService gadgetService,
                                          AdministrationMaintenanceService maintenanceService) {
        this.gadgetService = gadgetService;
        this.maintenanceService = maintenanceService;
    }

    // --- Gadget Endpoints ---

    @GetMapping
    public ResponseEntity<List<GadgetDto>> getGadgets(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean archived) {
        return ResponseEntity.ok(gadgetService.getGadgets(query, category, archived));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GadgetDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(gadgetService.getById(id));
    }

    @PostMapping
    public ResponseEntity<GadgetDto> create(@Valid @RequestBody CreateGadgetRequest req) {
        GadgetDto created = gadgetService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GadgetDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGadgetRequest req) {
        return ResponseEntity.ok(gadgetService.update(id, req));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<Void> archive(@PathVariable UUID id) {
        gadgetService.archive(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/unarchive")
    public ResponseEntity<Void> unarchive(@PathVariable UUID id) {
        gadgetService.unarchive(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        gadgetService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // --- Gadget Maintenance Nested Endpoints ---

    @GetMapping("/{id}/maintenance")
    public ResponseEntity<List<MaintenanceDto>> getMaintenance(@PathVariable UUID id) {
        return ResponseEntity.ok(maintenanceService.getMaintenanceByGadget(id));
    }

    @PostMapping("/{id}/maintenance")
    public ResponseEntity<MaintenanceDto> addMaintenance(
            @PathVariable UUID id,
            @Valid @RequestBody CreateMaintenanceRequest req) {
        // Enforce path gadget ID
        CreateMaintenanceRequest effective = new CreateMaintenanceRequest(
                id, req.title(), req.description(), req.maintenanceType(),
                req.dueDate(), req.recurrenceRule(), req.cost(),
                req.serviceProvider(), req.serviceNotes()
        );
        MaintenanceDto created = maintenanceService.create(effective);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping({"/maintenance/{maintId}", "/{id}/maintenance/{maintId}"})
    public ResponseEntity<MaintenanceDto> getMaintenanceById(
            @PathVariable(required = false) UUID id,
            @PathVariable UUID maintId) {
        return ResponseEntity.ok(maintenanceService.getById(maintId));
    }

    @PutMapping({"/maintenance/{maintId}", "/{id}/maintenance/{maintId}"})
    public ResponseEntity<MaintenanceDto> updateMaintenance(
            @PathVariable(required = false) UUID id,
            @PathVariable UUID maintId,
            @Valid @RequestBody UpdateMaintenanceRequest req) {
        return ResponseEntity.ok(maintenanceService.update(maintId, req));
    }

    @PostMapping({"/maintenance/{maintId}/complete", "/{id}/maintenance/{maintId}/complete"})
    public ResponseEntity<MaintenanceDto> completeMaintenance(
            @PathVariable(required = false) UUID id,
            @PathVariable UUID maintId,
            @Valid @RequestBody CompleteMaintenanceRequest req) {
        return ResponseEntity.ok(maintenanceService.complete(maintId, req));
    }

    @PostMapping({"/maintenance/{maintId}/cancel", "/{id}/maintenance/{maintId}/cancel"})
    public ResponseEntity<MaintenanceDto> cancelMaintenance(
            @PathVariable(required = false) UUID id,
            @PathVariable UUID maintId) {
        return ResponseEntity.ok(maintenanceService.cancel(maintId));
    }

    @DeleteMapping({"/maintenance/{maintId}", "/{id}/maintenance/{maintId}"})
    public ResponseEntity<Void> deleteMaintenance(
            @PathVariable(required = false) UUID id,
            @PathVariable UUID maintId) {
        maintenanceService.delete(maintId);
        return ResponseEntity.noContent().build();
    }
}
