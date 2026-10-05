package com.monday.app.reminder.repository;

import com.monday.app.reminder.entity.Reminder;
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
public class ReminderRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ReminderRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Reminder save(Reminder reminder) {
        UUID id = reminder.getId() != null ? reminder.getId() : UUID.randomUUID();
        String sql = """
                INSERT INTO reminders (id, task_id, event_id, remind_at, title, description, status)
                VALUES (:id, :taskId, :eventId, :remindAt, :title, :description, :status)
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(id, reminder), ReminderRowMapper.INSTANCE);
    }

    public Reminder update(Reminder reminder) {
        String sql = """
                UPDATE reminders SET
                    remind_at = :remindAt, title = :title, description = :description, status = :status
                WHERE id = :id
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(reminder.getId(), reminder), ReminderRowMapper.INSTANCE);
    }

    public Optional<Reminder> findById(UUID id) {
        return jdbc.query("SELECT * FROM reminders WHERE id = :id",
                new MapSqlParameterSource("id", id), ReminderRowMapper.INSTANCE).stream().findFirst();
    }

    public List<Reminder> findAll() {
        return jdbc.query("SELECT * FROM reminders ORDER BY remind_at", ReminderRowMapper.INSTANCE);
    }

    /**
     * Pending reminders due on or before the given instant.
     */
    public List<Reminder> findPendingDueBefore(Instant before) {
        String sql = """
                SELECT * FROM reminders
                WHERE status = 'PENDING' AND remind_at <= :before
                ORDER BY remind_at
                """;
        return jdbc.query(sql, new MapSqlParameterSource("before", Timestamp.from(before)), ReminderRowMapper.INSTANCE);
    }

    public List<Reminder> findByTaskId(UUID taskId) {
        return jdbc.query("SELECT * FROM reminders WHERE task_id = :taskId ORDER BY remind_at",
                new MapSqlParameterSource("taskId", taskId), ReminderRowMapper.INSTANCE);
    }

    public List<Reminder> findByEventId(UUID eventId) {
        return jdbc.query("SELECT * FROM reminders WHERE event_id = :eventId ORDER BY remind_at",
                new MapSqlParameterSource("eventId", eventId), ReminderRowMapper.INSTANCE);
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM reminders WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource toParams(UUID id, Reminder r) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("taskId", r.getTaskId())
                .addValue("eventId", r.getEventId())
                .addValue("remindAt", Timestamp.from(r.getRemindAt()))
                .addValue("title", r.getTitle())
                .addValue("description", r.getDescription())
                .addValue("status", r.getStatus());
    }

    static final class ReminderRowMapper implements RowMapper<Reminder> {
        static final ReminderRowMapper INSTANCE = new ReminderRowMapper();

        @Override
        public Reminder mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Reminder(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("task_id") != null ? UUID.fromString(rs.getString("task_id")) : null,
                    rs.getString("event_id") != null ? UUID.fromString(rs.getString("event_id")) : null,
                    rs.getTimestamp("remind_at").toInstant(),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getString("status"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()
            );
        }
    }
}
