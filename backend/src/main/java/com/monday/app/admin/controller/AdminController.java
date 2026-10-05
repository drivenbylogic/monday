package com.monday.app.admin.controller;

import com.monday.app.admin.service.AdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // --- Metrics & Radar ---

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        return ResponseEntity.ok(adminService.getMetrics());
    }

    @GetMapping("/renewal-radar")
    public ResponseEntity<Map<String, Object>> getRenewalRadar(
            @RequestParam(required = false, defaultValue = "90") int horizonDays) {
        return ResponseEntity.ok(adminService.getRenewalRadar(horizonDays));
    }

    // --- Obligations ---

    @GetMapping("/obligations")
    public ResponseEntity<Map<String, Object>> getObligations(
            @RequestParam(required = false, defaultValue = "all") String category,
            @RequestParam(required = false, defaultValue = "pending") String status,
            @RequestParam(required = false, defaultValue = "priority") String sortBy) {
        return ResponseEntity.ok(adminService.getObligations(category, status, sortBy));
    }

    @PostMapping("/obligations")
    public ResponseEntity<Map<String, Object>> createObligation(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createObligation(body));
    }

    @PostMapping("/obligations/{id}/settle")
    public ResponseEntity<Map<String, Object>> settleObligation(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(adminService.settleObligation(id, body));
    }

    @GetMapping("/obligations/{id}/tax-dossier")
    public ResponseEntity<Map<String, Object>> getTaxDossier(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.getTaxDossier(id));
    }

    @GetMapping("/obligations/{id}/lease-addendum")
    public ResponseEntity<Map<String, Object>> getLeaseAddendum(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.getLeaseAddendum(id));
    }

    // --- Subscriptions ---

    @GetMapping("/subscriptions")
    public ResponseEntity<Map<String, Object>> getSubscriptions() {
        return ResponseEntity.ok(adminService.getSubscriptions());
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<Map<String, Object>> createSubscription(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createSubscription(body));
    }

    @PatchMapping("/subscriptions/{id}")
    public ResponseEntity<Map<String, Object>> updateSubscription(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(adminService.updateSubscription(id, body));
    }

    // --- Vault ---

    @GetMapping("/vault")
    public ResponseEntity<Map<String, Object>> getVault() {
        return ResponseEntity.ok(adminService.getVault());
    }

    @PostMapping("/vault/sync-digilocker")
    public ResponseEntity<Map<String, Object>> syncDigiLocker() {
        return ResponseEntity.ok(adminService.syncDigiLocker());
    }

    @GetMapping("/vault/{id}/dossier")
    public ResponseEntity<Map<String, Object>> getVaultDossier(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.getVaultDossier(id));
    }

    @PostMapping("/vault/{id}/verify-invariants")
    public ResponseEntity<Map<String, Object>> verifyInvariants(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.verifyInvariants(id));
    }

    @GetMapping("/vault/{id}/export")
    public ResponseEntity<Map<String, Object>> exportVaultDocument(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.exportVaultDocument(id));
    }

    // --- Tasks & Archive ---

    @GetMapping("/tasks")
    public ResponseEntity<Map<String, Object>> getTasks() {
        return ResponseEntity.ok(adminService.getTasks());
    }

    @PatchMapping("/tasks/{taskId}")
    public ResponseEntity<Map<String, Object>> toggleTask(
            @PathVariable UUID taskId,
            @RequestBody Map<String, Object> body) {
        boolean completed = Boolean.TRUE.equals(body.get("completed"));
        return ResponseEntity.ok(adminService.toggleTask(taskId, completed));
    }

    @GetMapping("/physical-archive")
    public ResponseEntity<Map<String, Object>> getPhysicalArchive() {
        return ResponseEntity.ok(adminService.getPhysicalArchive());
    }
}
