package com.monday.app.vehicle.repository;

import com.monday.app.vehicle.entity.VehicleTask;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class VehicleTaskRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public VehicleTaskRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public VehicleTask save(VehicleTask task) {
        UUID id = task.getId() != null ? task.getId() : UUID.randomUUID();
        String sql = """
                INSERT INTO vehicle_tasks (id, vehicle_id, title, meta_text, completed, priority, linked_module)
                VALUES (:id, :vehicleId, :title, :metaText, :completed, :priority, :linkedModule)
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(id, task), VehicleTaskRowMapper.INSTANCE);
    }

    public VehicleTask update(VehicleTask task) {
        String sql = """
                UPDATE vehicle_tasks SET
                    title = :title, meta_text = :metaText, completed = :completed,
                    priority = :priority, linked_module = :linkedModule,
                    completed_at = :completedAt
                WHERE id = :id RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(task.getId(), task), VehicleTaskRowMapper.INSTANCE);
    }

    public Optional<VehicleTask> findById(UUID id) {
        return jdbc.query("SELECT * FROM vehicle_tasks WHERE id = :id",
                new MapSqlParameterSource("id", id), VehicleTaskRowMapper.INSTANCE).stream().findFirst();
    }

    public List<VehicleTask> findByVehicleId(UUID vehicleId) {
        return jdbc.query("SELECT * FROM vehicle_tasks WHERE vehicle_id = :vehicleId ORDER BY completed ASC, created_at DESC",
                new MapSqlParameterSource("vehicleId", vehicleId), VehicleTaskRowMapper.INSTANCE);
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM vehicle_tasks WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource toParams(UUID id, VehicleTask task) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("vehicleId", task.getVehicleId())
                .addValue("title", task.getTitle())
                .addValue("metaText", task.getMetaText())
                .addValue("completed", Boolean.TRUE.equals(task.getCompleted()))
                .addValue("priority", task.getPriority() != null ? task.getPriority() : "MEDIUM")
                .addValue("linkedModule", task.getLinkedModule())
                .addValue("completedAt", task.getCompletedAt() != null ? Timestamp.from(task.getCompletedAt()) : null);
    }

    static final class VehicleTaskRowMapper implements RowMapper<VehicleTask> {
        static final VehicleTaskRowMapper INSTANCE = new VehicleTaskRowMapper();

        @Override
        public VehicleTask mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new VehicleTask(
                    UUID.fromString(rs.getString("id")),
                    UUID.fromString(rs.getString("vehicle_id")),
                    rs.getString("title"),
                    rs.getString("meta_text"),
                    rs.getBoolean("completed"),
                    rs.getString("priority"),
                    rs.getString("linked_module"),
                    rs.getTimestamp("completed_at") != null ? rs.getTimestamp("completed_at").toInstant() : null,
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()
            );
        }
    }
}
