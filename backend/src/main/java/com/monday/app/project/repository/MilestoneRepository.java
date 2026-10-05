package com.monday.app.project.repository;

import com.monday.app.project.entity.Milestone;
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
public class MilestoneRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public MilestoneRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Milestone save(Milestone milestone) {
        UUID id = milestone.getId() != null ? milestone.getId() : UUID.randomUUID();
        String sql = """
                INSERT INTO milestones (id, project_id, name, description, status, priority, start_at, due_at, completed_at)
                VALUES (:id, :projectId, :name, :description, :status, :priority, :startAt, :dueAt, :completedAt)
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(id, milestone), MilestoneRowMapper.INSTANCE);
    }

    public Milestone update(Milestone milestone) {
        String sql = """
                UPDATE milestones SET
                    name = :name,
                    description = :description,
                    status = :status,
                    priority = :priority,
                    start_at = :startAt,
                    due_at = :dueAt,
                    completed_at = :completedAt
                WHERE id = :id
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(milestone.getId(), milestone), MilestoneRowMapper.INSTANCE);
    }

    public Optional<Milestone> findById(UUID id) {
        String sql = "SELECT * FROM milestones WHERE id = :id";
        return jdbc.query(sql, new MapSqlParameterSource("id", id), MilestoneRowMapper.INSTANCE)
                .stream().findFirst();
    }

    public List<Milestone> findByProjectId(UUID projectId) {
        String sql = "SELECT * FROM milestones WHERE project_id = :projectId ORDER BY due_at, created_at";
        return jdbc.query(sql, new MapSqlParameterSource("projectId", projectId), MilestoneRowMapper.INSTANCE);
    }

    public List<Milestone> findImpendingMilestones(Instant targetDate) {
        String sql = """
                SELECT * FROM milestones
                WHERE due_at <= :targetDate
                  AND status NOT IN ('COMPLETED', 'CANCELLED')
                ORDER BY due_at ASC NULLS LAST
                """;
        return jdbc.query(sql, new MapSqlParameterSource("targetDate", Timestamp.from(targetDate)), MilestoneRowMapper.INSTANCE);
    }

    public int countActiveMilestones() {
        String sql = "SELECT COUNT(*) FROM milestones WHERE status NOT IN ('COMPLETED', 'CANCELLED')";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource(), Integer.class);
        return count != null ? count : 0;
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM milestones WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    public Optional<Milestone> findNextByProjectId(UUID projectId) {
        String sql = """
                SELECT * FROM milestones
                WHERE project_id = :projectId
                  AND status NOT IN ('COMPLETED', 'CANCELLED')
                ORDER BY due_at ASC NULLS LAST
                LIMIT 1
                """;
        return jdbc.query(sql, new MapSqlParameterSource("projectId", projectId), MilestoneRowMapper.INSTANCE)
                .stream().findFirst();
    }

    private MapSqlParameterSource toParams(UUID id, Milestone m) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("projectId", m.getProjectId())
                .addValue("name", m.getName())
                .addValue("description", m.getDescription())
                .addValue("status", m.getStatus())
                .addValue("priority", m.getPriority())
                .addValue("startAt", m.getStartAt() != null ? Timestamp.from(m.getStartAt()) : null)
                .addValue("dueAt", m.getDueAt() != null ? Timestamp.from(m.getDueAt()) : null)
                .addValue("completedAt", m.getCompletedAt() != null ? Timestamp.from(m.getCompletedAt()) : null);
    }

    static final class MilestoneRowMapper implements RowMapper<Milestone> {
        static final MilestoneRowMapper INSTANCE = new MilestoneRowMapper();

        @Override
        public Milestone mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Milestone(
                    UUID.fromString(rs.getString("id")),
                    UUID.fromString(rs.getString("project_id")),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getString("status"),
                    rs.getString("priority"),
                    rs.getTimestamp("start_at") != null ? rs.getTimestamp("start_at").toInstant() : null,
                    rs.getTimestamp("due_at") != null ? rs.getTimestamp("due_at").toInstant() : null,
                    rs.getTimestamp("completed_at") != null ? rs.getTimestamp("completed_at").toInstant() : null,
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()
            );
        }
    }
}
