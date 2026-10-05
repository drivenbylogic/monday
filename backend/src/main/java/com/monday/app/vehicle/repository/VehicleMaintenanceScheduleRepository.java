package com.monday.app.vehicle.repository;

import com.monday.app.vehicle.entity.VehicleMaintenanceSchedule;
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
public class VehicleMaintenanceScheduleRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public VehicleMaintenanceScheduleRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public VehicleMaintenanceSchedule save(VehicleMaintenanceSchedule schedule) {
        UUID id = schedule.getId() != null ? schedule.getId() : UUID.randomUUID();
        String sql = """
                INSERT INTO vehicle_maintenance_schedules (id, vehicle_id, item_name, interval_months, interval_km, last_performed_at, last_performed_km, next_due_at, next_due_km, status)
                VALUES (:id, :vehicleId, :itemName, :intervalMonths, :intervalKm, :lastPerformedAt, :lastPerformedKm, :nextDueAt, :nextDueKm, :status)
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(id, schedule), VehicleMaintenanceScheduleRowMapper.INSTANCE);
    }

    public VehicleMaintenanceSchedule update(VehicleMaintenanceSchedule schedule) {
        String sql = """
                UPDATE vehicle_maintenance_schedules SET
                    item_name = :itemName, interval_months = :intervalMonths, interval_km = :intervalKm,
                    last_performed_at = :lastPerformedAt, last_performed_km = :lastPerformedKm,
                    next_due_at = :nextDueAt, next_due_km = :nextDueKm, status = :status
                WHERE id = :id RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(schedule.getId(), schedule), VehicleMaintenanceScheduleRowMapper.INSTANCE);
    }

    public Optional<VehicleMaintenanceSchedule> findById(UUID id) {
        return jdbc.query("SELECT * FROM vehicle_maintenance_schedules WHERE id = :id",
                new MapSqlParameterSource("id", id), VehicleMaintenanceScheduleRowMapper.INSTANCE).stream().findFirst();
    }

    public List<VehicleMaintenanceSchedule> findByVehicleId(UUID vehicleId) {
        return jdbc.query("SELECT * FROM vehicle_maintenance_schedules WHERE vehicle_id = :vehicleId ORDER BY next_due_at, next_due_km",
                new MapSqlParameterSource("vehicleId", vehicleId), VehicleMaintenanceScheduleRowMapper.INSTANCE);
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM vehicle_maintenance_schedules WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource toParams(UUID id, VehicleMaintenanceSchedule s) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("vehicleId", s.getVehicleId())
                .addValue("itemName", s.getItemName())
                .addValue("intervalMonths", s.getIntervalMonths())
                .addValue("intervalKm", s.getIntervalKm())
                .addValue("lastPerformedAt", s.getLastPerformedAt() != null ? Date.valueOf(s.getLastPerformedAt()) : null)
                .addValue("lastPerformedKm", s.getLastPerformedKm())
                .addValue("nextDueAt", s.getNextDueAt() != null ? Date.valueOf(s.getNextDueAt()) : null)
                .addValue("nextDueKm", s.getNextDueKm())
                .addValue("status", s.getStatus());
    }

    static final class VehicleMaintenanceScheduleRowMapper implements RowMapper<VehicleMaintenanceSchedule> {
        static final VehicleMaintenanceScheduleRowMapper INSTANCE = new VehicleMaintenanceScheduleRowMapper();
        @Override
        public VehicleMaintenanceSchedule mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new VehicleMaintenanceSchedule(
                    UUID.fromString(rs.getString("id")),
                    UUID.fromString(rs.getString("vehicle_id")),
                    rs.getString("item_name"),
                    rs.getObject("interval_months") != null ? rs.getInt("interval_months") : null,
                    rs.getObject("interval_km") != null ? rs.getInt("interval_km") : null,
                    rs.getDate("last_performed_at") != null ? rs.getDate("last_performed_at").toLocalDate() : null,
                    rs.getObject("last_performed_km") != null ? rs.getInt("last_performed_km") : null,
                    rs.getDate("next_due_at") != null ? rs.getDate("next_due_at").toLocalDate() : null,
                    rs.getObject("next_due_km") != null ? rs.getInt("next_due_km") : null,
                    rs.getString("status"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()
            );
        }
    }
}
