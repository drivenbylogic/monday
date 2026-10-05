package com.monday.app.vehicle.repository;

import com.monday.app.vehicle.entity.VehicleObligation;
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
public class VehicleObligationRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public VehicleObligationRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public VehicleObligation save(VehicleObligation obligation) {
        UUID id = obligation.getId() != null ? obligation.getId() : UUID.randomUUID();
        String sql = """
                INSERT INTO vehicle_obligations (id, vehicle_id, obligation_type, due_date, amount, status, notes)
                VALUES (:id, :vehicleId, :obligationType, :dueDate, :amount, :status, :notes)
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(id, obligation), VehicleObligationRowMapper.INSTANCE);
    }

    public VehicleObligation update(VehicleObligation obligation) {
        String sql = """
                UPDATE vehicle_obligations SET
                    obligation_type = :obligationType, due_date = :dueDate, amount = :amount, status = :status, notes = :notes
                WHERE id = :id RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(obligation.getId(), obligation), VehicleObligationRowMapper.INSTANCE);
    }

    public Optional<VehicleObligation> findById(UUID id) {
        return jdbc.query("SELECT * FROM vehicle_obligations WHERE id = :id",
                new MapSqlParameterSource("id", id), VehicleObligationRowMapper.INSTANCE).stream().findFirst();
    }

    public List<VehicleObligation> findByVehicleId(UUID vehicleId) {
        return jdbc.query("SELECT * FROM vehicle_obligations WHERE vehicle_id = :vehicleId ORDER BY due_date",
                new MapSqlParameterSource("vehicleId", vehicleId), VehicleObligationRowMapper.INSTANCE);
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM vehicle_obligations WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource toParams(UUID id, VehicleObligation o) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("vehicleId", o.getVehicleId())
                .addValue("obligationType", o.getObligationType())
                .addValue("dueDate", Date.valueOf(o.getDueDate()))
                .addValue("amount", o.getAmount())
                .addValue("status", o.getStatus())
                .addValue("notes", o.getNotes());
    }

    static final class VehicleObligationRowMapper implements RowMapper<VehicleObligation> {
        static final VehicleObligationRowMapper INSTANCE = new VehicleObligationRowMapper();
        @Override
        public VehicleObligation mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new VehicleObligation(
                    UUID.fromString(rs.getString("id")),
                    UUID.fromString(rs.getString("vehicle_id")),
                    rs.getString("obligation_type"),
                    rs.getDate("due_date").toLocalDate(),
                    rs.getBigDecimal("amount"),
                    rs.getString("status"),
                    rs.getString("notes"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()
            );
        }
    }
}
