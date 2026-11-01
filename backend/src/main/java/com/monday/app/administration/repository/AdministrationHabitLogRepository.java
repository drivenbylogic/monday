package com.monday.app.administration.repository;

import com.monday.app.administration.entity.AdministrationHabitLog;
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
public class AdministrationHabitLogRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AdministrationHabitLogRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public AdministrationHabitLog save(AdministrationHabitLog l) {
        UUID id = l.getId() != null ? l.getId() : UUID.randomUUID();
        l.setId(id);
        String sql = """
                INSERT INTO habit_logs (
                    id, habit_id, entry_date, actual_value, status, notes
                ) VALUES (
                    :id, :habitId, :entryDate, :actualValue, :status, :notes
                ) RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(l), HabitLogRowMapper.INSTANCE);
    }

    public AdministrationHabitLog update(AdministrationHabitLog l) {
        String sql = """
                UPDATE habit_logs SET
                    actual_value = :actualValue,
                    status = :status,
                    notes = :notes
                WHERE id = :id
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(l), HabitLogRowMapper.INSTANCE);
    }

    public Optional<AdministrationHabitLog> findById(UUID id) {
        String sql = "SELECT * FROM habit_logs WHERE id = :id";
        return jdbc.query(sql, new MapSqlParameterSource("id", id), HabitLogRowMapper.INSTANCE)
                .stream().findFirst();
    }

    public List<AdministrationHabitLog> findByHabitIdAndDateRange(UUID habitId, LocalDate from, LocalDate to) {
        String sql = """
                SELECT * FROM habit_logs
                WHERE habit_id = :habitId AND entry_date >= :from AND entry_date <= :to
                ORDER BY entry_date DESC, created_at DESC
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("habitId", habitId)
                .addValue("from", Date.valueOf(from))
                .addValue("to", Date.valueOf(to));
        return jdbc.query(sql, params, HabitLogRowMapper.INSTANCE);
    }

    public List<AdministrationHabitLog> findByHabitIdsAndDateRange(List<UUID> habitIds, LocalDate from, LocalDate to) {
        if (habitIds == null || habitIds.isEmpty()) return Collections.emptyList();
        String sql = """
                SELECT * FROM habit_logs
                WHERE habit_id IN (:habitIds) AND entry_date >= :from AND entry_date <= :to
                ORDER BY entry_date DESC, created_at DESC
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("habitIds", habitIds)
                .addValue("from", Date.valueOf(from))
                .addValue("to", Date.valueOf(to));
        return jdbc.query(sql, params, HabitLogRowMapper.INSTANCE);
    }

    public List<AdministrationHabitLog> findByDate(LocalDate date) {
        String sql = "SELECT * FROM habit_logs WHERE entry_date = :date ORDER BY created_at DESC";
        return jdbc.query(sql, new MapSqlParameterSource("date", Date.valueOf(date)), HabitLogRowMapper.INSTANCE);
    }

    public List<AdministrationHabitLog> findByUserIdAndDate(UUID userId, LocalDate date) {
        String sql = """
                SELECT l.* FROM habit_logs l
                JOIN habits h ON l.habit_id = h.id
                WHERE (h.user_id = :userId OR :userId IS NULL)
                  AND l.entry_date = :date
                ORDER BY l.created_at DESC
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("date", Date.valueOf(date));
        return jdbc.query(sql, params, HabitLogRowMapper.INSTANCE);
    }

    public List<AdministrationHabitLog> findByUserIdAndDateRange(UUID userId, LocalDate from, LocalDate to) {
        String sql = """
                SELECT l.* FROM habit_logs l
                JOIN habits h ON l.habit_id = h.id
                WHERE (h.user_id = :userId OR :userId IS NULL)
                  AND l.entry_date >= :from AND l.entry_date <= :to
                ORDER BY l.entry_date DESC, l.created_at DESC
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("from", Date.valueOf(from))
                .addValue("to", Date.valueOf(to));
        return jdbc.query(sql, params, HabitLogRowMapper.INSTANCE);
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM habit_logs WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource toParams(AdministrationHabitLog l) {
        return new MapSqlParameterSource()
                .addValue("id", l.getId())
                .addValue("habitId", l.getHabitId())
                .addValue("entryDate", l.getEntryDate() != null ? Date.valueOf(l.getEntryDate()) : null)
                .addValue("actualValue", l.getActualValue())
                .addValue("status", l.getStatus() != null ? l.getStatus() : "COMPLETED")
                .addValue("notes", l.getNotes());
    }

    private static class HabitLogRowMapper implements RowMapper<AdministrationHabitLog> {
        static final HabitLogRowMapper INSTANCE = new HabitLogRowMapper();

        @Override
        public AdministrationHabitLog mapRow(ResultSet rs, int rowNum) throws SQLException {
            AdministrationHabitLog l = new AdministrationHabitLog();
            l.setId(rs.getObject("id", UUID.class));
            l.setHabitId(rs.getObject("habit_id", UUID.class));

            Date d = rs.getDate("entry_date");
            if (d != null) l.setEntryDate(d.toLocalDate());

            l.setActualValue(rs.getBigDecimal("actual_value"));
            l.setStatus(rs.getString("status"));
            l.setNotes(rs.getString("notes"));

            Timestamp created = rs.getTimestamp("created_at");
            if (created != null) l.setCreatedAt(created.toInstant());

            Timestamp updated = rs.getTimestamp("updated_at");
            if (updated != null) l.setUpdatedAt(updated.toInstant());

            return l;
        }
    }
}

