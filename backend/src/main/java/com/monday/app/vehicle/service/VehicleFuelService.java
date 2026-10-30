package com.monday.app.vehicle.service;

import com.monday.app.shared.exception.ResourceNotFoundException;
import com.monday.app.vehicle.dto.RecordOdometerRequest;
import com.monday.app.vehicle.entity.Vehicle;
import com.monday.app.vehicle.entity.VehicleFuelLog;
import com.monday.app.vehicle.repository.VehicleFuelLogRepository;
import com.monday.app.vehicle.repository.VehicleRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class VehicleFuelService {

    private final VehicleFuelLogRepository fuelLogRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleOdometerService odometerService;
    private final NamedParameterJdbcTemplate jdbc;

    public VehicleFuelService(VehicleFuelLogRepository fuelLogRepository,
                              VehicleRepository vehicleRepository,
                              VehicleOdometerService odometerService,
                              NamedParameterJdbcTemplate jdbc) {
        this.fuelLogRepository = fuelLogRepository;
        this.vehicleRepository = vehicleRepository;
        this.odometerService = odometerService;
        this.jdbc = jdbc;
    }

    public List<VehicleFuelLog> getFuelLogs(UUID vehicleId, int limit) {
        vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));
        List<VehicleFuelLog> logs = fuelLogRepository.findByVehicleId(vehicleId);
        return logs.stream().limit(limit > 0 ? limit : 20).toList();
    }

    @Transactional
    public VehicleFuelLog logFuel(UUID vehicleId, Map<String, Object> body) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));

        Object dateObj = body.containsKey("loggedAt") ? body.get("loggedAt") : body.get("date");
        LocalDate logDate = dateObj != null
                ? LocalDate.parse(dateObj.toString().substring(0, Math.min(10, dateObj.toString().length())))
                : LocalDate.now();

        String fuelBrand = (String) (body.containsKey("fuelBrand") ? body.get("fuelBrand") : body.get("brand"));
        String stationLocation = (String) (body.containsKey("stationLocation") ? body.get("stationLocation") : body.get("location"));
        String fuelGrade = (String) body.get("fuelGrade");
        Boolean fullTank = body.containsKey("fullTank") ? Boolean.TRUE.equals(body.get("fullTank")) : true;

        BigDecimal volumeLiters = toBigDecimal(body.get("volumeLiters"));
        BigDecimal totalCost = toBigDecimal(body.get("totalCost"));
        BigDecimal pricePerLiter = toBigDecimal(body.get("pricePerLiter"));
        if (pricePerLiter == null && volumeLiters != null && totalCost != null && volumeLiters.compareTo(BigDecimal.ZERO) > 0) {
            pricePerLiter = totalCost.divide(volumeLiters, 2, RoundingMode.HALF_UP);
        }

        Integer odoKm = toInteger(body.get("odometerKm"));

        // Calculate trip efficiency based on previous fill-up
        BigDecimal calculatedEfficiency = null;
        if (odoKm != null && volumeLiters != null && volumeLiters.compareTo(BigDecimal.ZERO) > 0) {
            List<VehicleFuelLog> previousLogs = fuelLogRepository.findByVehicleId(vehicleId);
            Optional<VehicleFuelLog> prevLogOpt = previousLogs.stream()
                    .filter(l -> l.getOdometerKm() != null && l.getOdometerKm() < odoKm)
                    .findFirst();

            if (prevLogOpt.isPresent()) {
                int distance = odoKm - prevLogOpt.get().getOdometerKm();
                if (distance > 0) {
                    calculatedEfficiency = BigDecimal.valueOf(distance)
                            .divide(volumeLiters, 2, RoundingMode.HALF_UP);
                }
            }
        }

        VehicleFuelLog log = new VehicleFuelLog(
                null,
                vehicleId,
                logDate,
                fuelBrand,
                fuelBrand,
                stationLocation,
                stationLocation,
                fuelGrade,
                fullTank,
                volumeLiters,
                totalCost,
                pricePerLiter,
                odoKm,
                calculatedEfficiency,
                null
        );

        VehicleFuelLog saved = fuelLogRepository.save(log);

        // Record odometer reading if provided
        if (odoKm != null && odoKm > 0) {
            try {
                odometerService.recordReading(vehicleId, new RecordOdometerRequest(
                        logDate,
                        odoKm,
                        "FUEL_LOG",
                        false,
                        "Recorded at fuel fill-up: " + (fuelBrand != null ? fuelBrand : "Fuel")
                ));
            } catch (Exception ignored) {
                // Ignore if odometer is already newer
            }
        }

        // Sync to Personal Finances if requested
        if (Boolean.TRUE.equals(body.get("syncToFinance")) && totalCost != null && totalCost.compareTo(BigDecimal.ZERO) > 0) {
            String txId = UUID.randomUUID().toString();
            String desc = "Fuel: " + (fuelBrand != null ? fuelBrand : vehicle.getName());
            String txSql = """
                    INSERT INTO transactions (id, transaction_type, amount, transaction_at, description, linked_module, linked_entity_id, linked_entity_type)
                    VALUES (:txId::uuid, 'EXPENSE', :amount, CURRENT_TIMESTAMP, :desc, 'VEHICLES', :vId, 'FUEL_LOG')
                    """;
            jdbc.update(txSql, new MapSqlParameterSource()
                    .addValue("txId", txId)
                    .addValue("amount", totalCost)
                    .addValue("desc", desc)
                    .addValue("vId", vehicleId.toString()));
        }

        return saved;
    }

    private BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return null;
        if (obj instanceof BigDecimal bd) return bd;
        if (obj instanceof Number num) return BigDecimal.valueOf(num.doubleValue());
        try {
            return new BigDecimal(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Integer toInteger(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Integer i) return i;
        if (obj instanceof Number num) return num.intValue();
        try {
            return Integer.parseInt(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
