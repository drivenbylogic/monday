package com.monday.app.administration.repository;

import com.monday.app.administration.entity.AdministrationHabitReminder;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class AdministrationHabitReminderRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AdministrationHabitReminderRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public AdministrationHabitReminder save(AdministrationHabitReminder r) {
        UUID id = r.getId() != null ? r.getId() : UUID.randomUUID();
        r.setId(id);
        String sql = """
                INSERT INTO habit_reminders (
                    id, habit_id, reminder_time, enabled
                ) VALUES (
                    :id, :habitId, :reminderTime, :enabled
                ) RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(r), ReminderRowMapper.INSTANCE);
    }

    public List<AdministrationHabitReminder> findByHabitId(UUID habitId) {
        String sql = "SELECT * FROM habit_reminders WHERE habit_id = :habitId ORDER BY reminder_time ASC";
        return jdbc.query(sql, new MapSqlParameterSource("habitId", habitId), ReminderRowMapper.INSTANCE);
    }

    public void deleteByHabitId(UUID habitId) {
        jdbc.update("DELETE FROM habit_reminders WHERE habit_id = :habitId", new MapSqlParameterSource("habitId", habitId));
    }

    private MapSqlParameterSource toParams(AdministrationHabitReminder r) {
        return new MapSqlParameterSource()
                .addValue("id", r.getId())
                .addValue("habitId", r.getHabitId())
                .addValue("reminderTime", r.getReminderTime() != null ? Time.valueOf(r.getReminderTime()) : null)
                .addValue("enabled", r.isEnabled());
    }

    private static class ReminderRowMapper implements RowMapper<AdministrationHabitReminder> {
        static final ReminderRowMapper INSTANCE = new ReminderRowMapper();

        @Override
        public AdministrationHabitReminder mapRow(ResultSet rs, int rowNum) throws SQLException {
            AdministrationHabitReminder r = new AdministrationHabitReminder();
            r.setId(rs.getObject("id", UUID.class));
            r.setHabitId(rs.getObject("habit_id", UUID.class));

            Time t = rs.getTime("reminder_time");
            if (t != null) r.setReminderTime(t.toLocalTime());

            r.setEnabled(rs.getBoolean("enabled"));

            Timestamp created = rs.getTimestamp("created_at");
            if (created != null) r.setCreatedAt(created.toInstant());

            Timestamp updated = rs.getTimestamp("updated_at");
            if (updated != null) r.setUpdatedAt(updated.toInstant());

            return r;
        }
    }
}

