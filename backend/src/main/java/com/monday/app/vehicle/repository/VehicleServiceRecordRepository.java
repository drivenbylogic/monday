package com.monday.app.vehicle.repository;

import com.monday.app.vehicle.entity.VehicleServiceRecord;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

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
                INSERT INTO vehicle_service_records (id, vehicle_id, service_date, km_at_service, service_type, description, cost, performed_by, notes)
                VALUES (:id, :vehicleId, :serviceDate, :kmAtService, :serviceType, :description, :cost, :performedBy, :notes)
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(id, record), VehicleServiceRecordRowMapper.INSTANCE);
    }

    public VehicleServiceRecord update(VehicleServiceRecord record) {
        String sql = """
                UPDATE vehicle_service_records SET
                    service_date = :serviceDate, km_at_service = :kmAtService, service_type = :serviceType,
                    description = :description, cost = :cost, performed_by = :performedBy, notes = :notes
                WHERE id = :id RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(record.getId(), record), VehicleServiceRecordRowMapper.INSTANCE);
    }

    public Optional<VehicleServiceRecord> findById(UUID id) {
        return jdbc.query("SELECT * FROM vehicle_service_records WHERE id = :id",
                new MapSqlParameterSource("id", id), VehicleServiceRecordRowMapper.INSTANCE).stream().findFirst();
    }

    public List<VehicleServiceRecord> findByVehicleId(UUID vehicleId) {
        return jdbc.query("SELECT * FROM vehicle_service_records WHERE vehicle_id = :vehicleId ORDER BY service_date DESC",
                new MapSqlParameterSource("vehicleId", vehicleId), VehicleServiceRecordRowMapper.INSTANCE);
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM vehicle_service_records WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource toParams(UUID id, VehicleServiceRecord r) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("vehicleId", r.getVehicleId())
                .addValue("serviceDate", Date.valueOf(r.getServiceDate()))
                .addValue("kmAtService", r.getKmAtService())
                .addValue("serviceType", r.getServiceType())
                .addValue("description", r.getDescription())
                .addValue("cost", r.getCost())
                .addValue("performedBy", r.getPerformedBy())
                .addValue("notes", r.getNotes());
    }

    static final class VehicleServiceRecordRowMapper implements RowMapper<VehicleServiceRecord> {
        static final VehicleServiceRecordRowMapper INSTANCE = new VehicleServiceRecordRowMapper();
        @Override
        public VehicleServiceRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new VehicleServiceRecord(
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
        }
    }
}
