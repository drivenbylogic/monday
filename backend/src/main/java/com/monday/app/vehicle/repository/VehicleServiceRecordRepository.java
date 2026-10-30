package com.monday.app.vehicle.repository;

import com.monday.app.vehicle.entity.VehicleServiceRecord;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class VehicleServiceRecordRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public VehicleServiceRecordRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public VehicleServiceRecord save(VehicleServiceRecord record) {
        UUID id = record.getId() != null ? record.getId() : UUID.randomUUID();
        String sql = """
                INSERT INTO vehicle_service_records (
                    id, vehicle_id, service_date, km_at_service, service_type, description,
                    cost, parts_cost, labor_cost, performed_by, notes, verified, is_upcoming,
                    linked_transaction_id
                )
                VALUES (
                    :id, :vehicleId, :serviceDate, :kmAtService, :serviceType, :description,
                    :cost, :partsCost, :laborCost, :performedBy, :notes, :verified, :isUpcoming,
                    :linkedTransactionId
                )
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(id, record), VehicleServiceRecordRowMapper.INSTANCE);
    }

    public VehicleServiceRecord update(VehicleServiceRecord record) {
        String sql = """
                UPDATE vehicle_service_records SET
                    service_date = :serviceDate, km_at_service = :kmAtService, service_type = :serviceType,
                    description = :description, cost = :cost, parts_cost = :partsCost, labor_cost = :laborCost,
                    performed_by = :performedBy, notes = :notes, verified = :verified, is_upcoming = :isUpcoming,
                    linked_transaction_id = :linkedTransactionId
                WHERE id = :id RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(record.getId(), record), VehicleServiceRecordRowMapper.INSTANCE);
    }

    public Optional<VehicleServiceRecord> findById(UUID id) {
        return jdbc.query("SELECT * FROM vehicle_service_records WHERE id = :id",
                new MapSqlParameterSource("id", id), VehicleServiceRecordRowMapper.INSTANCE).stream().findFirst();
    }

    public List<VehicleServiceRecord> findByVehicleId(UUID vehicleId) {
        return jdbc.query("SELECT * FROM vehicle_service_records WHERE vehicle_id = :vehicleId ORDER BY service_date DESC, created_at DESC",
                new MapSqlParameterSource("vehicleId", vehicleId), VehicleServiceRecordRowMapper.INSTANCE);
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM vehicle_service_records WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource toParams(UUID id, VehicleServiceRecord r) {
        BigDecimal totalCost = r.getCost();
        if (totalCost == null) {
            BigDecimal parts = r.getPartsCost() != null ? r.getPartsCost() : BigDecimal.ZERO;
            BigDecimal labor = r.getLaborCost() != null ? r.getLaborCost() : BigDecimal.ZERO;
            totalCost = parts.add(labor);
        }
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("vehicleId", r.getVehicleId())
                .addValue("serviceDate", Date.valueOf(r.getServiceDate()))
                .addValue("kmAtService", r.getKmAtService())
                .addValue("serviceType", r.getServiceType())
                .addValue("description", r.getDescription())
                .addValue("cost", totalCost)
                .addValue("partsCost", r.getPartsCost() != null ? r.getPartsCost() : BigDecimal.ZERO)
                .addValue("laborCost", r.getLaborCost() != null ? r.getLaborCost() : BigDecimal.ZERO)
                .addValue("performedBy", r.getPerformedBy())
                .addValue("notes", r.getNotes())
                .addValue("verified", Boolean.TRUE.equals(r.getVerified()))
                .addValue("isUpcoming", Boolean.TRUE.equals(r.getIsUpcoming()))
                .addValue("linkedTransactionId", r.getLinkedTransactionId());
    }

    static final class VehicleServiceRecordRowMapper implements RowMapper<VehicleServiceRecord> {
        static final VehicleServiceRecordRowMapper INSTANCE = new VehicleServiceRecordRowMapper();
        @Override
        public VehicleServiceRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            VehicleServiceRecord rec = new VehicleServiceRecord(
                    UUID.fromString(rs.getString("id")),
                    UUID.fromString(rs.getString("vehicle_id")),
                    rs.getDate("service_date").toLocalDate(),
                    rs.getObject("km_at_service") != null ? rs.getInt("km_at_service") : null,
                    rs.getString("service_type"),
                    rs.getString("description"),
                    rs.getBigDecimal("cost"),
                    rs.getString("performed_by"),
                    rs.getString("notes"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()
            );
            rec.setPartsCost(rs.getBigDecimal("parts_cost"));
            rec.setLaborCost(rs.getBigDecimal("labor_cost"));
            rec.setVerified(rs.getBoolean("verified"));
            rec.setIsUpcoming(rs.getBoolean("is_upcoming"));
            rec.setLinkedTransactionId(rs.getString("linked_transaction_id"));
            rec.setSpecificationsJson(rs.getString("specifications_json"));
            return rec;
        }
    }
}
