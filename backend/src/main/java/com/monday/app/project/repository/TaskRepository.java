package com.monday.app.project.repository;

import com.monday.app.project.entity.Task;
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
public class TaskRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public TaskRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Task save(Task task) {
        UUID id = task.getId() != null ? task.getId() : UUID.randomUUID();
        String sql = """
                INSERT INTO tasks (id, title, description, project_id, milestone_id, event_id, parent_task_id,
                    status, priority, due_at, started_at, completed_at, estimated_minutes,
                    is_recurring, recurrence_pattern)
                VALUES (:id, :title, :description, :projectId, :milestoneId, :eventId, :parentTaskId,
                    :status, :priority, :dueAt, :startedAt, :completedAt, :estimatedMinutes,
                    :isRecurring, :recurrencePattern)
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(id, task), TaskRowMapper.INSTANCE);
    }

    public Task update(Task task) {
        String sql = """
                UPDATE tasks SET
                    title = :title,
                    description = :description,
                    project_id = :projectId,
                    milestone_id = :milestoneId,
                    event_id = :eventId,
                    parent_task_id = :parentTaskId,
                    status = :status,
                    priority = :priority,
                    due_at = :dueAt,
                    started_at = :startedAt,
                    completed_at = :completedAt,
                    estimated_minutes = :estimatedMinutes,
                    is_recurring = :isRecurring,
                    recurrence_pattern = :recurrencePattern
                WHERE id = :id
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(task.getId(), task), TaskRowMapper.INSTANCE);
    }

    public Optional<Task> findById(UUID id) {
        String sql = "SELECT * FROM tasks WHERE id = :id";
        return jdbc.query(sql, new MapSqlParameterSource("id", id), TaskRowMapper.INSTANCE)
                .stream().findFirst();
    }

    public List<Task> findAll() {
        return jdbc.query("SELECT * FROM tasks ORDER BY due_at NULLS LAST, priority, created_at DESC",
                TaskRowMapper.INSTANCE);
    }

    public List<Task> findByProjectId(UUID projectId) {
        String sql = "SELECT * FROM tasks WHERE project_id = :projectId ORDER BY due_at NULLS LAST, priority";
        return jdbc.query(sql, new MapSqlParameterSource("projectId", projectId), TaskRowMapper.INSTANCE);
    }

    public List<Task> findByMilestoneId(UUID milestoneId) {
        String sql = "SELECT * FROM tasks WHERE milestone_id = :milestoneId ORDER BY due_at NULLS LAST, priority";
        return jdbc.query(sql, new MapSqlParameterSource("milestoneId", milestoneId), TaskRowMapper.INSTANCE);
    }

    public List<Task> findByEventId(UUID eventId) {
        String sql = "SELECT * FROM tasks WHERE event_id = :eventId ORDER BY due_at NULLS LAST, priority";
        return jdbc.query(sql, new MapSqlParameterSource("eventId", eventId), TaskRowMapper.INSTANCE);
    }

    public List<Task> findByStatus(String status) {
        String sql = "SELECT * FROM tasks WHERE status = :status ORDER BY due_at NULLS LAST, priority";
        return jdbc.query(sql, new MapSqlParameterSource("status", status), TaskRowMapper.INSTANCE);
    }

    /**
     * Tasks due on or before the given instant that are not yet completed or cancelled.
     */
    public List<Task> findDueByDate(Instant dueBy) {
        String sql = """
                SELECT * FROM tasks
                WHERE due_at <= :dueBy
                  AND status NOT IN ('COMPLETED', 'CANCELLED')
                ORDER BY due_at, priority
                """;
        return jdbc.query(sql, new MapSqlParameterSource("dueBy", Timestamp.from(dueBy)), TaskRowMapper.INSTANCE);
    }

    /**
     * Overdue tasks: due before now and not completed.
     */
    public List<Task> findOverdue() {
        String sql = """
                SELECT * FROM tasks
                WHERE due_at < CURRENT_TIMESTAMP
                  AND status NOT IN ('COMPLETED', 'CANCELLED')
                ORDER BY due_at, priority
                """;
        return jdbc.query(sql, new MapSqlParameterSource(), TaskRowMapper.INSTANCE);
    }

    /**
     * Recurring template tasks that need occurrence generation.
     */
    public List<Task> findRecurringTemplates() {
        String sql = """
                SELECT * FROM tasks
                WHERE is_recurring = true AND parent_task_id IS NULL
                ORDER BY created_at
                """;
        return jdbc.query(sql, new MapSqlParameterSource(), TaskRowMapper.INSTANCE);
    }

    /**
     * Find occurrences generated from a recurring template.
     */
    public List<Task> findByParentTaskId(UUID parentTaskId) {
        String sql = "SELECT * FROM tasks WHERE parent_task_id = :parentTaskId ORDER BY due_at";
        return jdbc.query(sql, new MapSqlParameterSource("parentTaskId", parentTaskId), TaskRowMapper.INSTANCE);
    }

    public int countOpenTasks() {
        String sql = "SELECT COUNT(*) FROM tasks WHERE status NOT IN ('COMPLETED', 'CANCELLED')";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource(), Integer.class);
        return count != null ? count : 0;
    }

    public int countCompletedTasksBetween(Instant start, Instant end) {
        String sql = "SELECT COUNT(*) FROM tasks WHERE status = 'COMPLETED' AND completed_at >= :start AND completed_at < :end";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("start", Timestamp.from(start))
            .addValue("end", Timestamp.from(end));
        Integer count = jdbc.queryForObject(sql, params, Integer.class);
        return count != null ? count : 0;
    }

    public List<Task> findExecutionQueue() {
        String sql = """
                SELECT * FROM tasks
                WHERE status NOT IN ('COMPLETED', 'CANCELLED')
                ORDER BY due_at ASC NULLS LAST
                """;
        return jdbc.query(sql, new MapSqlParameterSource(), TaskRowMapper.INSTANCE);
    }

    public List<java.util.Map<String, Object>> getDailyCompletionsBetween(Instant start, Instant end) {
        String sql = """
                SELECT DATE(completed_at) as completed_date, COUNT(*) as task_count
                FROM tasks
                WHERE status = 'COMPLETED' AND completed_at >= :start AND completed_at < :end
                GROUP BY DATE(completed_at)
                ORDER BY completed_date
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("start", Timestamp.from(start))
            .addValue("end", Timestamp.from(end));
        return jdbc.queryForList(sql, params);
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM tasks WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    public Optional<Task> findNextByProjectId(UUID projectId) {
        String sql = """
                SELECT * FROM tasks
                WHERE project_id = :projectId
                  AND status NOT IN ('COMPLETED', 'CANCELLED')
                ORDER BY due_at ASC NULLS LAST
                LIMIT 1
                """;
        return jdbc.query(sql, new MapSqlParameterSource("projectId", projectId), TaskRowMapper.INSTANCE)
                .stream().findFirst();
    }

    private MapSqlParameterSource toParams(UUID id, Task t) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("title", t.getTitle())
                .addValue("description", t.getDescription())
                .addValue("projectId", t.getProjectId())
                .addValue("milestoneId", t.getMilestoneId())
                .addValue("eventId", t.getEventId())
                .addValue("parentTaskId", t.getParentTaskId())
                .addValue("status", t.getStatus())
                .addValue("priority", t.getPriority())
                .addValue("dueAt", t.getDueAt() != null ? Timestamp.from(t.getDueAt()) : null)
                .addValue("startedAt", t.getStartedAt() != null ? Timestamp.from(t.getStartedAt()) : null)
                .addValue("completedAt", t.getCompletedAt() != null ? Timestamp.from(t.getCompletedAt()) : null)
                .addValue("estimatedMinutes", t.getEstimatedMinutes())
                .addValue("isRecurring", t.isRecurring())
                .addValue("recurrencePattern", t.getRecurrencePattern());
    }

    static final class TaskRowMapper implements RowMapper<Task> {
        static final TaskRowMapper INSTANCE = new TaskRowMapper();

        @Override
        public Task mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Task(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getString("project_id") != null ? UUID.fromString(rs.getString("project_id")) : null,
                    rs.getString("milestone_id") != null ? UUID.fromString(rs.getString("milestone_id")) : null,
                    rs.getString("event_id") != null ? UUID.fromString(rs.getString("event_id")) : null,
                    rs.getString("parent_task_id") != null ? UUID.fromString(rs.getString("parent_task_id")) : null,
                    rs.getString("status"),
                    rs.getString("priority"),
                    rs.getTimestamp("due_at") != null ? rs.getTimestamp("due_at").toInstant() : null,
                    rs.getTimestamp("started_at") != null ? rs.getTimestamp("started_at").toInstant() : null,
                    rs.getTimestamp("completed_at") != null ? rs.getTimestamp("completed_at").toInstant() : null,
                    rs.getObject("estimated_minutes") != null ? rs.getInt("estimated_minutes") : null,
                    rs.getBoolean("is_recurring"),
                    rs.getString("recurrence_pattern"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()
            );
        }
    }
}
