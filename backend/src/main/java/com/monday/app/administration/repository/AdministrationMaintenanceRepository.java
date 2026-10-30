package com.monday.app.administration.repository;

import com.monday.app.administration.entity.AdministrationMaintenance;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AdministrationMaintenanceRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AdministrationMaintenanceRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public AdministrationMaintenance save(AdministrationMaintenance m) {
        UUID id = m.getId() != null ? m.getId() : UUID.randomUUID();
        m.setId(id);
        String sql = """
                INSERT INTO administration_maintenance (
                    id, gadget_id, title, description, maintenance_type, due_date,
                    recurrence_rule, last_performed_at, cost, status, service_provider, service_notes
                ) VALUES (
                    :id, :gadgetId, :title, :description, :maintenanceType, :dueDate,
                    :recurrenceRule, :lastPerformedAt, :cost, :status, :serviceProvider, :serviceNotes
                ) RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(m), MaintenanceRowMapper.INSTANCE);
    }

    public AdministrationMaintenance update(AdministrationMaintenance m) {
        String sql = """
                UPDATE administration_maintenance SET
                    title = :title,
                    description = :description,
                    maintenance_type = :maintenanceType,
                    due_date = :dueDate,
                    recurrence_rule = :recurrenceRule,
                    last_performed_at = :lastPerformedAt,
                    cost = :cost,
                    status = :status,
                    service_provider = :serviceProvider,
                    service_notes = :serviceNotes
                WHERE id = :id
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(m), MaintenanceRowMapper.INSTANCE);
    }

    public Optional<AdministrationMaintenance> findById(UUID id) {
        String sql = "SELECT * FROM administration_maintenance WHERE id = :id";
        return jdbc.query(sql, new MapSqlParameterSource("id", id), MaintenanceRowMapper.INSTANCE)
                .stream().findFirst();
    }

    public List<AdministrationMaintenance> findByGadgetId(UUID gadgetId) {
        String sql = "SELECT * FROM administration_maintenance WHERE gadget_id = :gadgetId ORDER BY due_date ASC";
        return jdbc.query(sql, new MapSqlParameterSource("gadgetId", gadgetId), MaintenanceRowMapper.INSTANCE);
    }

    public List<AdministrationMaintenance> findAll() {
        String sql = "SELECT * FROM administration_maintenance ORDER BY due_date ASC";
        return jdbc.query(sql, MaintenanceRowMapper.INSTANCE);
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM administration_maintenance WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    public List<AdministrationMaintenance> findUpcomingDue(LocalDate fromDate, LocalDate toDate, int limit) {
        String sql = """
                SELECT * FROM administration_maintenance
                WHERE status NOT IN ('COMPLETED', 'CANCELLED')
                  AND due_date >= :from
                  AND due_date <= :to
                ORDER BY due_date ASC
                LIMIT :limit
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("from", fromDate)
                .addValue("to", toDate)
                .addValue("limit", limit);
        return jdbc.query(sql, params, MaintenanceRowMapper.INSTANCE);
    }

    public int countDue(LocalDate today) {
        String sql = """
                SELECT COUNT(*) FROM administration_maintenance
                WHERE status NOT IN ('COMPLETED', 'CANCELLED')
                  AND due_date <= :maxDate
                """;
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("maxDate", today.plusDays(30)), Integer.class);
        return count != null ? count : 0;
    }

    public String findGadgetName(UUID gadgetId) {
        if (gadgetId == null) return null;
        String sql = "SELECT name FROM administration_gadgets WHERE id = :gadgetId";
        List<String> names = jdbc.query(sql, new MapSqlParameterSource("gadgetId", gadgetId), (rs, rowNum) -> rs.getString("name"));
        return names.isEmpty() ? null : names.get(0);
    }

    private MapSqlParameterSource toParams(AdministrationMaintenance m) {
        return new MapSqlParameterSource()
                .addValue("id", m.getId())
                .addValue("gadgetId", m.getGadgetId())
                .addValue("title", m.getTitle())
                .addValue("description", m.getDescription())
                .addValue("maintenanceType", m.getMaintenanceType())
                .addValue("dueDate", m.getDueDate())
                .addValue("recurrenceRule", m.getRecurrenceRule())
                .addValue("lastPerformedAt", m.getLastPerformedAt())
                .addValue("cost", m.getCost())
                .addValue("status", m.getStatus())
                .addValue("serviceProvider", m.getServiceProvider())
                .addValue("serviceNotes", m.getServiceNotes());
    }

    static final class MaintenanceRowMapper implements RowMapper<AdministrationMaintenance> {
        static final MaintenanceRowMapper INSTANCE = new MaintenanceRowMapper();

        @Override
        public AdministrationMaintenance mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new AdministrationMaintenance(
                    UUID.fromString(rs.getString("id")),
                    UUID.fromString(rs.getString("gadget_id")),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getString("maintenance_type"),
                    rs.getDate("due_date").toLocalDate(),
                    rs.getString("recurrence_rule"),
                    rs.getDate("last_performed_at") != null ? rs.getDate("last_performed_at").toLocalDate() : null,
                    rs.getBigDecimal("cost"),
                    rs.getString("status"),
                    rs.getString("service_provider"),
                    rs.getString("service_notes"),
                    rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : Instant.now(),
                    rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toInstant() : Instant.now()
            );
        }
    }
}
