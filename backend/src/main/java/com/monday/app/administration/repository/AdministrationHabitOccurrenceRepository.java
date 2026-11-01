package com.monday.app.administration.repository;

import com.monday.app.administration.entity.AdministrationHabitOccurrence;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AdministrationHabitOccurrenceRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AdministrationHabitOccurrenceRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public AdministrationHabitOccurrence save(AdministrationHabitOccurrence o) {
        UUID id = o.getId() != null ? o.getId() : UUID.randomUUID();
        o.setId(id);
        String sql = """
                INSERT INTO habit_occurrences (
                    id, habit_id, schedule_version_id, scheduled_date,
                    target_value_snapshot, minimum_target_snapshot, target_unit_snapshot,
                    status, actual_value, completed_at, notes, skip_reason
                ) VALUES (
                    :id, :habitId, :scheduleVersionId, :scheduledDate,
                    :targetValueSnapshot, :minimumTargetSnapshot, :targetUnitSnapshot,
                    :status, :actualValue, :completedAt, :notes, :skipReason
                )
                ON CONFLICT (habit_id, scheduled_date) DO UPDATE SET
                    schedule_version_id = EXCLUDED.schedule_version_id,
                    target_value_snapshot = EXCLUDED.target_value_snapshot,
                    minimum_target_snapshot = EXCLUDED.minimum_target_snapshot,
                    target_unit_snapshot = EXCLUDED.target_unit_snapshot
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(o), HabitOccurrenceRowMapper.INSTANCE);
    }

    public AdministrationHabitOccurrence update(AdministrationHabitOccurrence o) {
        String sql = """
                UPDATE habit_occurrences SET
                    status = :status,
                    actual_value = :actualValue,
                    completed_at = :completedAt,
                    notes = :notes,
                    skip_reason = :skipReason
                WHERE id = :id
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(o), HabitOccurrenceRowMapper.INSTANCE);
    }

    public Optional<AdministrationHabitOccurrence> findById(UUID id) {
        String sql = "SELECT * FROM habit_occurrences WHERE id = :id";
        return jdbc.query(sql, new MapSqlParameterSource("id", id), HabitOccurrenceRowMapper.INSTANCE)
                .stream().findFirst();
    }

    public Optional<AdministrationHabitOccurrence> findByHabitIdAndScheduledDate(UUID habitId, LocalDate date) {
        String sql = "SELECT * FROM habit_occurrences WHERE habit_id = :habitId AND scheduled_date = :date";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("habitId", habitId)
                .addValue("date", Date.valueOf(date));
        return jdbc.query(sql, params, HabitOccurrenceRowMapper.INSTANCE).stream().findFirst();
    }

    public List<AdministrationHabitOccurrence> findByDate(LocalDate date) {
        String sql = "SELECT * FROM habit_occurrences WHERE scheduled_date = :date ORDER BY created_at ASC";
        return jdbc.query(sql, new MapSqlParameterSource("date", Date.valueOf(date)), HabitOccurrenceRowMapper.INSTANCE);
    }

    public List<AdministrationHabitOccurrence> findByUserIdAndDate(UUID userId, LocalDate date) {
        String sql = """
                SELECT o.* FROM habit_occurrences o
                JOIN habits h ON o.habit_id = h.id
                WHERE (h.user_id = :userId OR :userId IS NULL)
                  AND o.scheduled_date = :date
                ORDER BY o.created_at ASC
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("date", Date.valueOf(date));
        return jdbc.query(sql, params, HabitOccurrenceRowMapper.INSTANCE);
    }

    public List<AdministrationHabitOccurrence> findByDateRange(LocalDate from, LocalDate to) {
        String sql = """
                SELECT * FROM habit_occurrences
                WHERE scheduled_date >= :from AND scheduled_date <= :to
                ORDER BY scheduled_date ASC, created_at ASC
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("from", Date.valueOf(from))
                .addValue("to", Date.valueOf(to));
        return jdbc.query(sql, params, HabitOccurrenceRowMapper.INSTANCE);
    }

    public List<AdministrationHabitOccurrence> findByUserIdAndDateRange(UUID userId, LocalDate from, LocalDate to) {
        String sql = """
                SELECT o.* FROM habit_occurrences o
                JOIN habits h ON o.habit_id = h.id
                WHERE (h.user_id = :userId OR :userId IS NULL)
                  AND o.scheduled_date >= :from AND o.scheduled_date <= :to
                ORDER BY o.scheduled_date ASC, o.created_at ASC
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("from", Date.valueOf(from))
                .addValue("to", Date.valueOf(to));
        return jdbc.query(sql, params, HabitOccurrenceRowMapper.INSTANCE);
    }

    public List<AdministrationHabitOccurrence> findByHabitIdAndDateRange(UUID habitId, LocalDate from, LocalDate to) {
        String sql = """
                SELECT * FROM habit_occurrences
                WHERE habit_id = :habitId AND scheduled_date >= :from AND scheduled_date <= :to
                ORDER BY scheduled_date ASC
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("habitId", habitId)
                .addValue("from", Date.valueOf(from))
                .addValue("to", Date.valueOf(to));
        return jdbc.query(sql, params, HabitOccurrenceRowMapper.INSTANCE);
    }

    public List<AdministrationHabitOccurrence> findByHabitIdsAndDateRange(List<UUID> habitIds, LocalDate from, LocalDate to) {
        if (habitIds == null || habitIds.isEmpty()) return Collections.emptyList();
        String sql = """
                SELECT * FROM habit_occurrences
                WHERE habit_id IN (:habitIds) AND scheduled_date >= :from AND scheduled_date <= :to
                ORDER BY scheduled_date ASC
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("habitIds", habitIds)
                .addValue("from", Date.valueOf(from))
                .addValue("to", Date.valueOf(to));
        return jdbc.query(sql, params, HabitOccurrenceRowMapper.INSTANCE);
    }

    public List<AdministrationHabitOccurrence> findRecentByHabitId(UUID habitId, int limit) {
        String sql = """
                SELECT * FROM habit_occurrences
                WHERE habit_id = :habitId
                ORDER BY scheduled_date DESC
                LIMIT :limit
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("habitId", habitId)
                .addValue("limit", limit);
        return jdbc.query(sql, params, HabitOccurrenceRowMapper.INSTANCE);
    }

    public int markOverduePendingAsMissed(LocalDate beforeDate) {
        String sql = """
                UPDATE habit_occurrences
                SET status = 'MISSED'
                WHERE scheduled_date < :beforeDate AND status = 'PENDING'
                """;
        return jdbc.update(sql, new MapSqlParameterSource("beforeDate", Date.valueOf(beforeDate)));
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM habit_occurrences WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource toParams(AdministrationHabitOccurrence o) {
        return new MapSqlParameterSource()
                .addValue("id", o.getId())
                .addValue("habitId", o.getHabitId())
                .addValue("scheduleVersionId", o.getScheduleVersionId())
                .addValue("scheduledDate", o.getScheduledDate() != null ? Date.valueOf(o.getScheduledDate()) : null)
                .addValue("targetValueSnapshot", o.getTargetValueSnapshot())
                .addValue("minimumTargetSnapshot", o.getMinimumTargetSnapshot())
                .addValue("targetUnitSnapshot", o.getTargetUnitSnapshot())
                .addValue("status", o.getStatus() != null ? o.getStatus() : "PENDING")
                .addValue("actualValue", o.getActualValue())
                .addValue("completedAt", o.getCompletedAt() != null ? Timestamp.from(o.getCompletedAt()) : null)
                .addValue("notes", o.getNotes())
                .addValue("skipReason", o.getSkipReason());
    }

    private static class HabitOccurrenceRowMapper implements RowMapper<AdministrationHabitOccurrence> {
        static final HabitOccurrenceRowMapper INSTANCE = new HabitOccurrenceRowMapper();

        @Override
        public AdministrationHabitOccurrence mapRow(ResultSet rs, int rowNum) throws SQLException {
            AdministrationHabitOccurrence o = new AdministrationHabitOccurrence();
            o.setId(rs.getObject("id", UUID.class));
            o.setHabitId(rs.getObject("habit_id", UUID.class));
            o.setScheduleVersionId(rs.getObject("schedule_version_id", UUID.class));

            Date d = rs.getDate("scheduled_date");
            if (d != null) o.setScheduledDate(d.toLocalDate());

            o.setTargetValueSnapshot(rs.getBigDecimal("target_value_snapshot"));
            o.setMinimumTargetSnapshot(rs.getBigDecimal("minimum_target_snapshot"));
            o.setTargetUnitSnapshot(rs.getString("target_unit_snapshot"));
            o.setStatus(rs.getString("status"));
            o.setActualValue(rs.getBigDecimal("actual_value"));

            Timestamp comp = rs.getTimestamp("completed_at");
            if (comp != null) o.setCompletedAt(comp.toInstant());

            o.setNotes(rs.getString("notes"));
            o.setSkipReason(rs.getString("skip_reason"));

            Timestamp created = rs.getTimestamp("created_at");
            if (created != null) o.setCreatedAt(created.toInstant());

            Timestamp updated = rs.getTimestamp("updated_at");
            if (updated != null) o.setUpdatedAt(updated.toInstant());

            return o;
        }
    }
}

