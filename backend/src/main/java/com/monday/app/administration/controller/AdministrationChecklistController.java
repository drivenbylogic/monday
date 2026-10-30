package com.monday.app.administration.controller;

import com.monday.app.administration.dto.*;
import com.monday.app.administration.service.AdministrationChecklistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/administration/checklists", "/api/administration/checklists"})
public class AdministrationChecklistController {

    private final AdministrationChecklistService service;

    public AdministrationChecklistController(AdministrationChecklistService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ChecklistDto>> getChecklists(
            @RequestParam(required = false) String recordType,
            @RequestParam(required = false) String parentRecordType,
            @RequestParam(required = false) UUID recordId,
            @RequestParam(required = false) UUID parentRecordId) {
        String type = recordType != null ? recordType : parentRecordType;
        UUID id = recordId != null ? recordId : parentRecordId;
        if (type != null && id != null) {
            return ResponseEntity.ok(service.getChecklistsForRecord(type, id));
        } else if (type != null) {
            return ResponseEntity.ok(service.getChecklistsForRecordType(type));
        }
        return ResponseEntity.ok(service.getAllChecklists());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChecklistDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getChecklistById(id));
    }

    @PostMapping
    public ResponseEntity<ChecklistDto> create(@Valid @RequestBody CreateChecklistRequest req) {
        ChecklistDto created = service.createChecklist(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChecklistDto> update(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String title = body.get("title");
        String description = body.get("description");
        String status = body.get("status");
        return ResponseEntity.ok(service.updateChecklist(id, title, description, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteChecklist(id);
        return ResponseEntity.noContent().build();
    }

    // --- Items ---

    @PostMapping("/{id}/items")
    public ResponseEntity<ChecklistItemDto> addItem(
            @PathVariable UUID id,
            @Valid @RequestBody CreateChecklistItemRequest req) {
        ChecklistItemDto created = service.addItem(id, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping({"/items/{itemId}", "/{id}/items/{itemId}"})
    public ResponseEntity<ChecklistItemDto> updateItem(
            @PathVariable(required = false) UUID id,
            @PathVariable UUID itemId,
            @RequestBody UpdateChecklistItemRequest req) {
        return ResponseEntity.ok(service.updateItem(id, itemId, req));
    }

    @RequestMapping(value = {"/items/{itemId}/toggle", "/{id}/items/{itemId}/toggle"}, method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<ChecklistItemDto> toggleItem(
            @PathVariable(required = false) UUID id,
            @PathVariable UUID itemId) {
        return ResponseEntity.ok(service.toggleItemCompleted(id, itemId));
    }

    @DeleteMapping({"/items/{itemId}", "/{id}/items/{itemId}"})
    public ResponseEntity<Void> deleteItem(
            @PathVariable(required = false) UUID id,
            @PathVariable UUID itemId) {
        service.deleteItem(id, itemId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/items/reorder")
    public ResponseEntity<ChecklistDto> reorderItems(
            @PathVariable UUID id,
            @Valid @RequestBody ReorderChecklistItemsRequest req) {
        return ResponseEntity.ok(service.reorderItems(id, req));
    }
}
