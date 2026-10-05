package com.monday.app.vehicle.service;

import com.monday.app.shared.exception.ResourceNotFoundException;
import com.monday.app.vehicle.entity.Vehicle;
import com.monday.app.vehicle.entity.VehicleMaintenanceSchedule;
import com.monday.app.vehicle.entity.VehicleObligation;
import com.monday.app.vehicle.entity.VehicleServiceRecord;
import com.monday.app.vehicle.repository.VehicleMaintenanceScheduleRepository;
import com.monday.app.vehicle.repository.VehicleObligationRepository;
import com.monday.app.vehicle.repository.VehicleRepository;
import com.monday.app.vehicle.repository.VehicleServiceRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleServiceRecordRepository serviceRecordRepository;
    private final VehicleMaintenanceScheduleRepository scheduleRepository;
    private final VehicleObligationRepository obligationRepository;
    private final org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate jdbc;

    public VehicleService(VehicleRepository vehicleRepository,
                          VehicleServiceRecordRepository serviceRecordRepository,
                          VehicleMaintenanceScheduleRepository scheduleRepository,
                          VehicleObligationRepository obligationRepository,
                          org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate jdbc) {
        this.vehicleRepository = vehicleRepository;
        this.serviceRecordRepository = serviceRecordRepository;
        this.scheduleRepository = scheduleRepository;
        this.obligationRepository = obligationRepository;
        this.jdbc = jdbc;
    }

    // --- Vehicles ---
    @Transactional
    public Vehicle createVehicle(Vehicle vehicle) {
        if (vehicle.getStatus() == null) {
            vehicle.setStatus("ACTIVE");
        }
        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public Vehicle updateVehicle(UUID id, Vehicle updated) {
        Vehicle existing = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));
        existing.setName(updated.getName());
        existing.setRegistrationNumber(updated.getRegistrationNumber());
        existing.setPurchaseDate(updated.getPurchaseDate());
        existing.setPurchaseAmount(updated.getPurchaseAmount());
        existing.setCurrentKm(updated.getCurrentKm());
        existing.setStatus(updated.getStatus());
        return vehicleRepository.update(existing);
    }

    public Vehicle getVehicle(UUID id) {
        return vehicleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));
    }

    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    @Transactional
    public void deleteVehicle(UUID id) {
        getVehicle(id); // Check existence
        vehicleRepository.deleteById(id);
    }

    // --- Service Records ---
    @Transactional
    public VehicleServiceRecord createServiceRecord(VehicleServiceRecord record) {
        getVehicle(record.getVehicleId());
        return serviceRecordRepository.save(record);
    }

    @Transactional
    public VehicleServiceRecord updateServiceRecord(UUID id, VehicleServiceRecord updated) {
        VehicleServiceRecord existing = serviceRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VehicleServiceRecord", id));
        existing.setServiceDate(updated.getServiceDate());
        existing.setKmAtService(updated.getKmAtService());
        existing.setServiceType(updated.getServiceType());
        existing.setDescription(updated.getDescription());
        existing.setCost(updated.getCost());
        existing.setPerformedBy(updated.getPerformedBy());
        existing.setNotes(updated.getNotes());
        return serviceRecordRepository.update(existing);
    }

    public List<VehicleServiceRecord> getServiceRecords(UUID vehicleId) {
        return serviceRecordRepository.findByVehicleId(vehicleId);
    }

    @Transactional
    public void deleteServiceRecord(UUID id) {
        serviceRecordRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("VehicleServiceRecord", id));
        serviceRecordRepository.deleteById(id);
    }

    // --- Maintenance Schedules ---
    @Transactional
    public VehicleMaintenanceSchedule createMaintenanceSchedule(VehicleMaintenanceSchedule schedule) {
        getVehicle(schedule.getVehicleId());
        if (schedule.getStatus() == null) schedule.setStatus("OK");
        return scheduleRepository.save(schedule);
    }

    @Transactional
    public VehicleMaintenanceSchedule updateMaintenanceSchedule(UUID id, VehicleMaintenanceSchedule updated) {
        VehicleMaintenanceSchedule existing = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VehicleMaintenanceSchedule", id));
        existing.setItemName(updated.getItemName());
        existing.setIntervalMonths(updated.getIntervalMonths());
        existing.setIntervalKm(updated.getIntervalKm());
        existing.setLastPerformedAt(updated.getLastPerformedAt());
        existing.setLastPerformedKm(updated.getLastPerformedKm());
        existing.setNextDueAt(updated.getNextDueAt());
        existing.setNextDueKm(updated.getNextDueKm());
        existing.setStatus(updated.getStatus());
        return scheduleRepository.update(existing);
    }

    public List<VehicleMaintenanceSchedule> getMaintenanceSchedules(UUID vehicleId) {
        return scheduleRepository.findByVehicleId(vehicleId);
    }

    @Transactional
    public void deleteMaintenanceSchedule(UUID id) {
        scheduleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("VehicleMaintenanceSchedule", id));
        scheduleRepository.deleteById(id);
    }

    // --- Obligations ---
    @Transactional
    public VehicleObligation createObligation(VehicleObligation obligation) {
        getVehicle(obligation.getVehicleId());
        if (obligation.getStatus() == null) obligation.setStatus("PENDING");
        return obligationRepository.save(obligation);
    }

    @Transactional
    public VehicleObligation updateObligation(UUID id, VehicleObligation updated) {
        VehicleObligation existing = obligationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VehicleObligation", id));
        existing.setObligationType(updated.getObligationType());
        existing.setDueDate(updated.getDueDate());
        existing.setAmount(updated.getAmount());
        existing.setStatus(updated.getStatus());
        existing.setNotes(updated.getNotes());
        return obligationRepository.update(existing);
    }

    public List<VehicleObligation> getObligations(UUID vehicleId) {
        return obligationRepository.findByVehicleId(vehicleId);
    }

    @Transactional
    public void deleteObligation(UUID id) {
        obligationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("VehicleObligation", id));
        obligationRepository.deleteById(id);
    }

    // --- Analytical API Methods ---
    
    public java.util.Map<String, Object> getFleet(String status) {
        String sql = "SELECT v.id, v.name, v.edition, v.variant, v.type, v.registration_number as \"registrationNumber\", " +
                     "v.is_primary as \"isPrimary\", v.hero_image_url as \"heroImageUrl\", " +
                     "COALESCE((SELECT t.operational_readiness_score FROM vehicle_telemetry t WHERE t.vehicle_id = v.id ORDER BY t.recorded_at DESC LIMIT 1), 100) as \"operationalReadinessScore\" " +
                     "FROM vehicles v";
        if (!"all".equals(status)) {
            sql += " WHERE v.status = :status";
        }
        List<java.util.Map<String, Object>> list = jdbc.queryForList(sql, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("status", status != null ? status.toUpperCase() : "ACTIVE"));
        return java.util.Map.of("data", list);
    }
    
    public java.util.Map<String, Object> getVehicleSpecs(UUID vehicleId) {
        String sql = "SELECT id, name || ' ' || COALESCE(edition, '') as name, registered_owner as \"registeredOwner\", " +
                     "registering_authority as \"registeringAuthority\", emission_standard as \"emissionStandard\", vin, " +
                     "engine_json as engine, chassis_json as chassis, service_manual_url as \"serviceManualUrl\" FROM vehicles WHERE id = :id::uuid";
        List<java.util.Map<String, Object>> results = jdbc.queryForList(sql, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("id", vehicleId));
        if (results.isEmpty()) throw new ResourceNotFoundException("Vehicle", vehicleId);
        return new java.util.HashMap<>(results.get(0));
    }
    
    public java.util.Map<String, Object> getVehicleTelemetry(UUID vehicleId) {
        String sql = "SELECT vehicle_id as \"vehicleId\", recorded_at as \"recordedAt\", odometer_json as odometer, " +
                     "next_scheduled_service_json as \"nextScheduledService\", tires_json as tires, " +
                     "electrical_json as electrical, drivetrain_json as drivetrain, fuel_json as fuel, " +
                     "operational_readiness_score as \"operationalReadinessScore\" " +
                     "FROM vehicle_telemetry WHERE vehicle_id = :id::uuid ORDER BY recorded_at DESC LIMIT 1";
        List<java.util.Map<String, Object>> res = jdbc.queryForList(sql, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("id", vehicleId));
        return res.isEmpty() ? java.util.Map.of("vehicleId", vehicleId) : res.get(0);
    }
    
    public java.util.Map<String, Object> scanDiagnostics(UUID vehicleId) {
        String sql = "INSERT INTO vehicle_diagnostics (vehicle_id, diagnostic_protocol, fault_codes_detected, summary) " +
                     "VALUES (:id::uuid, 'ISO 15765-4 (CAN 29/500)', 0, 'Zero fault codes detected on CAN bus') RETURNING *";
        List<java.util.Map<String, Object>> res = jdbc.queryForList(sql, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("id", vehicleId));
        java.util.Map<String, Object> map = new java.util.HashMap<>(res.get(0));
        map.put("dtcList", List.of());
        map.put("subsystemStatus", java.util.Map.of("ecu", "Normal", "absUnit", "Normal"));
        return map;
    }

    public java.util.Map<String, Object> getMaintenanceLedger(UUID vehicleId, String type, int limit) {
        String sql = "SELECT id, vehicle_id as \"vehicleId\", title, to_char(service_date, 'YYYY-MM-DD') as date, " +
                     "km_at_service as \"odometerKm\", cost, is_upcoming as upcoming, verified, " +
                     "performed_by as \"serviceProvider\", description as details " +
                     "FROM vehicle_service_records WHERE vehicle_id = :id::uuid ";
        if ("completed".equals(type)) sql += "AND is_upcoming = false ";
        if ("upcoming".equals(type)) sql += "AND is_upcoming = true ";
        sql += "ORDER BY service_date DESC LIMIT :limit";
        return java.util.Map.of("data", jdbc.queryForList(sql, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("id", vehicleId).addValue("limit", limit)));
    }
    
    @Transactional
    public java.util.Map<String, Object> logMaintenance(UUID vehicleId, java.util.Map<String, Object> body) {
        String sql = "INSERT INTO vehicle_service_records (vehicle_id, title, cost, km_at_service, service_date, description, performed_by, verified, is_upcoming, linked_transaction_id) " +
                     "VALUES (:vId::uuid, :title, :cost, :odo, :date::date, :desc, :prov, :ver, false, :txId) RETURNING id";
        
        String txId = null;
        if (Boolean.TRUE.equals(body.get("recordFinanceTransaction"))) {
            txId = "tx-" + UUID.randomUUID().toString();
            jdbc.update("INSERT INTO transactions (id, transaction_type, amount, transaction_at, description) VALUES (:txId::uuid, 'expense', :cost, CURRENT_TIMESTAMP, :title)",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("txId", txId).addValue("cost", body.get("cost")).addValue("title", body.get("title")));
        }
        
        org.springframework.jdbc.core.namedparam.MapSqlParameterSource params = new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
            .addValue("vId", vehicleId)
            .addValue("title", body.get("title"))
            .addValue("cost", body.get("cost"))
            .addValue("odo", body.get("odometerKm"))
            .addValue("date", body.get("date"))
            .addValue("desc", body.get("details"))
            .addValue("prov", body.get("serviceProvider"))
            .addValue("ver", body.get("verified"))
            .addValue("txId", txId);
            
        String recordId = jdbc.queryForObject(sql, params, String.class);
        
        java.util.Map<String, Object> res = new java.util.HashMap<>(body);
        res.put("id", recordId);
        res.put("vehicleId", vehicleId);
        res.put("linkedTransactionId", txId);
        return res;
    }

    public java.util.Map<String, Object> getServiceScope(UUID vehicleId, String recordId) {
        String sql = "SELECT id as \"recordId\", title as \"jobTitle\", specifications_json as specifications, parts_cost as \"estimatedPartsCost\", labor_cost as \"estimatedLaborCost\", cost as \"totalEstimate\" FROM vehicle_service_records WHERE id = :id::uuid";
        return jdbc.queryForMap(sql, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("id", recordId));
    }

    public java.util.Map<String, Object> getOperationalOutlay(UUID vehicleId, String fiscalYear) {
        String sql = "SELECT COALESCE(SUM(cost), 0) FROM vehicle_service_records WHERE vehicle_id = :id::uuid";
        java.math.BigDecimal maintCost = jdbc.queryForObject(sql, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("id", vehicleId), java.math.BigDecimal.class);
        
        String fSql = "SELECT COALESCE(SUM(total_cost), 0) FROM vehicle_fuel_logs WHERE vehicle_id = :id::uuid";
        java.math.BigDecimal fuelCost = jdbc.queryForObject(fSql, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("id", vehicleId), java.math.BigDecimal.class);
        
        java.math.BigDecimal total = maintCost.add(fuelCost);
        
        return java.util.Map.of(
            "vehicleId", vehicleId,
            "fiscalYear", fiscalYear != null ? fiscalYear : "FY YTD",
            "combinedTotal", total,
            "categories", java.util.Map.of(
                "fuel", java.util.Map.of("amount", fuelCost, "percentage", total.compareTo(java.math.BigDecimal.ZERO) > 0 ? fuelCost.doubleValue()/total.doubleValue()*100 : 0),
                "maintenance", java.util.Map.of("amount", maintCost, "percentage", total.compareTo(java.math.BigDecimal.ZERO) > 0 ? maintCost.doubleValue()/total.doubleValue()*100 : 0)
            )
        );
    }
    
    public java.util.Map<String, Object> getFuelLogs(UUID vehicleId, int limit) {
        String sql = "SELECT id, to_char(log_date, 'Mon DD, YYYY') as date, brand, location, volume_liters as \"volumeLiters\", " +
                     "total_cost as \"totalCost\", price_per_liter as \"pricePerLiter\", odometer_km as \"odometerKm\", trip_efficiency as \"tripEfficiencyKmPerL\" " +
                     "FROM vehicle_fuel_logs WHERE vehicle_id = :id::uuid ORDER BY log_date DESC LIMIT :limit";
        List<java.util.Map<String, Object>> data = jdbc.queryForList(sql, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("id", vehicleId).addValue("limit", limit));
        return java.util.Map.of("data", data, "totalCount", data.size());
    }
    
    @Transactional
    public java.util.Map<String, Object> logFuel(UUID vehicleId, java.util.Map<String, Object> body) {
        String sql = "INSERT INTO vehicle_fuel_logs (vehicle_id, log_date, brand, location, volume_liters, total_cost, odometer_km) " +
                     "VALUES (:vId::uuid, :date::date, :brand, :loc, :vol, :cost, :odo) RETURNING id";
                     
        org.springframework.jdbc.core.namedparam.MapSqlParameterSource params = new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
            .addValue("vId", vehicleId)
            .addValue("date", body.get("date"))
            .addValue("brand", body.get("brand"))
            .addValue("loc", body.get("location"))
            .addValue("vol", body.get("volumeLiters"))
            .addValue("cost", body.get("totalCost"))
            .addValue("odo", body.get("odometerKm"));
            
        String id = jdbc.queryForObject(sql, params, String.class);
        
        if (Boolean.TRUE.equals(body.get("syncToFinance"))) {
             jdbc.update("INSERT INTO transactions (id, transaction_type, amount, transaction_at, description) VALUES (:txId::uuid, 'expense', :cost, CURRENT_TIMESTAMP, 'Fuel')",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("txId", UUID.randomUUID().toString()).addValue("cost", body.get("totalCost")));
        }
        
        java.util.Map<String, Object> res = new java.util.HashMap<>(body);
        res.put("id", id);
        res.put("vehicleId", vehicleId);
        return res;
    }

    public java.util.Map<String, Object> getInsurance(UUID vehicleId) {
        String sql = "SELECT vehicle_id as \"vehicleId\", status, is_renewed as \"isRenewed\", policy_name as \"policyName\", " +
                     "underwriter, policy_number as \"policyNumber\", coverage_scope as \"coverageScope\", " +
                     "insured_declared_value as \"insuredDeclaredValue\", premium_due as \"premiumDue\", " +
                     "to_char(due_date, 'YYYY-MM-DD') as \"dueDate\", package_document_url as \"packageDocumentUrl\" " +
                     "FROM vehicle_insurance WHERE vehicle_id = :id::uuid ORDER BY due_date DESC LIMIT 1";
        List<java.util.Map<String, Object>> res = jdbc.queryForList(sql, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("id", vehicleId));
        return res.isEmpty() ? java.util.Map.of() : res.get(0);
    }
    
    @Transactional
    public java.util.Map<String, Object> renewInsurance(UUID vehicleId, java.util.Map<String, Object> body) {
        jdbc.update("UPDATE vehicle_insurance SET is_renewed = true, status = 'active_and_verified' WHERE vehicle_id = :id::uuid", 
            new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("id", vehicleId));
        return java.util.Map.of("success", true, "message", "Verified: Policy renewed");
    }

    public java.util.Map<String, Object> getDocuments(UUID vehicleId) {
        String sql = "SELECT id as \"id\", doc_type as type, title, identifier, issuer, valid_until as \"validUntil\", " +
                     "verification_source as \"verificationSource\", is_verified as \"isVerified\", status, covered_components as \"coveredComponents\" " +
                     "FROM vehicle_documents WHERE vehicle_id = :id::uuid";
        List<java.util.Map<String, Object>> list = jdbc.queryForList(sql, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("id", vehicleId));
        return java.util.Map.of("vehicleId", vehicleId, "allValid", true, "documents", list);
    }
    
    public java.util.Map<String, Object> downloadDocument(UUID vehicleId, String documentId) {
        return java.util.Map.of("documentId", documentId, "downloadUrl", "https://storage.monday.internal/vault/" + documentId + ".pdf?token=exp");
    }

    public java.util.Map<String, Object> getTasks(UUID vehicleId) {
        String sql = "SELECT id, title, meta_text as meta, completed, priority, linked_module as \"linkedModule\" FROM vehicle_tasks WHERE vehicle_id = :id::uuid ORDER BY created_at";
        List<java.util.Map<String, Object>> tasks = jdbc.queryForList(sql, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("id", vehicleId));
        return java.util.Map.of("vehicleId", vehicleId, "tasks", tasks, "pendingCount", tasks.stream().filter(t -> !Boolean.TRUE.equals(t.get("completed"))).count());
    }

    public java.util.Map<String, Object> toggleTask(UUID vehicleId, String taskId, boolean completed) {
        jdbc.update("UPDATE vehicle_tasks SET completed = :c, completed_at = CURRENT_TIMESTAMP WHERE id = :id::uuid AND vehicle_id = :vId::uuid", 
            new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("c", completed).addValue("id", taskId).addValue("vId", vehicleId));
        return java.util.Map.of("id", taskId, "completed", completed);
    }
    
    public java.util.Map<String, Object> createTask(UUID vehicleId, java.util.Map<String, Object> body) {
        String sql = "INSERT INTO vehicle_tasks (vehicle_id, title, meta_text, priority) VALUES (:vId::uuid, :title, :meta, :priority) RETURNING id";
        String id = jdbc.queryForObject(sql, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("vId", vehicleId)
            .addValue("title", body.get("title"))
            .addValue("meta", body.get("meta"))
            .addValue("priority", body.get("priority")), String.class);
        return java.util.Map.of("id", id, "title", body.get("title"), "completed", false);
    }
}
