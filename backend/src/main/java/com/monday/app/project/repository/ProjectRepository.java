package com.monday.app.project.repository;

import com.monday.app.project.entity.Project;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ProjectRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ProjectRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Project save(Project project) {
        UUID id = project.getId() != null ? project.getId() : UUID.randomUUID();
        String sql = """
                INSERT INTO projects (id, name, description, category, status, priority, tags, start_at, due_at, completed_at)
                VALUES (:id, :name, :description, :category, :status, :priority, :tags, :startAt, :dueAt, :completedAt)
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(id, project), ProjectRowMapper.INSTANCE);
    }

    public Project update(Project project) {
        String sql = """
                UPDATE projects SET
                    name = :name,
                    description = :description,
                    category = :category,
                    status = :status,
                    priority = :priority,
                    tags = :tags,
                    start_at = :startAt,
                    due_at = :dueAt,
                    completed_at = :completedAt
                WHERE id = :id
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(project.getId(), project), ProjectRowMapper.INSTANCE);
    }

    public Optional<Project> findById(UUID id) {
        String sql = "SELECT * FROM projects WHERE id = :id";
        return jdbc.query(sql, new MapSqlParameterSource("id", id), ProjectRowMapper.INSTANCE)
                .stream().findFirst();
    }

    public List<Project> findAll() {
        return jdbc.query("SELECT * FROM projects ORDER BY created_at DESC", ProjectRowMapper.INSTANCE);
    }

    public List<Project> findByStatus(String status) {
        String sql = "SELECT * FROM projects WHERE status = :status ORDER BY priority, created_at DESC";
        return jdbc.query(sql, new MapSqlParameterSource("status", status), ProjectRowMapper.INSTANCE);
    }

    public List<Project> findByCategory(String category) {
        String sql = "SELECT * FROM projects WHERE category = :category ORDER BY priority, created_at DESC";
        return jdbc.query(sql, new MapSqlParameterSource("category", category), ProjectRowMapper.INSTANCE);
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM projects WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    public int countActiveProjects() {
        String sql = "SELECT COUNT(*) FROM projects WHERE status NOT IN ('COMPLETED', 'CANCELLED')";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource(), Integer.class);
        return count != null ? count : 0;
    }

    private MapSqlParameterSource toParams(UUID id, Project p) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("name", p.getName())
                .addValue("description", p.getDescription())
                .addValue("category", p.getCategory())
                .addValue("status", p.getStatus())
                .addValue("priority", p.getPriority())
                .addValue("tags", p.getTags())
                .addValue("startAt", p.getStartAt() != null ? Timestamp.from(p.getStartAt()) : null)
                .addValue("dueAt", p.getDueAt() != null ? Timestamp.from(p.getDueAt()) : null)
                .addValue("completedAt", p.getCompletedAt() != null ? Timestamp.from(p.getCompletedAt()) : null);
    }

    static final class ProjectRowMapper implements RowMapper<Project> {
        static final ProjectRowMapper INSTANCE = new ProjectRowMapper();

        @Override
        public Project mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Project(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getString("category"),
                    rs.getString("status"),
                    rs.getString("priority"),
                    rs.getString("tags"),
                    rs.getTimestamp("start_at") != null ? rs.getTimestamp("start_at").toInstant() : null,
                    rs.getTimestamp("due_at") != null ? rs.getTimestamp("due_at").toInstant() : null,
                    rs.getTimestamp("completed_at") != null ? rs.getTimestamp("completed_at").toInstant() : null,
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()
            );
        }
    }
}
