package com.monday.app.administration.repository;

import com.monday.app.administration.entity.AdministrationHabitScheduleVersion;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AdministrationHabitScheduleVersionRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AdministrationHabitScheduleVersionRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public AdministrationHabitScheduleVersion save(AdministrationHabitScheduleVersion v) {
        UUID id = v.getId() != null ? v.getId() : UUID.randomUUID();
        v.setId(id);
        String sql = """
                INSERT INTO habit_schedule_versions (
                    id, habit_id, schedule_config, target_value, minimum_target_value,
                    target_unit, effective_from, effective_until
                ) VALUES (
                    :id, :habitId, :scheduleConfig::jsonb, :targetValue, :minimumTargetValue,
                    :targetUnit, :effectiveFrom, :effectiveUntil
                ) RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(v), ScheduleVersionRowMapper.INSTANCE);
    }

    public Optional<AdministrationHabitScheduleVersion> findActiveVersionOnDate(UUID habitId, LocalDate date) {
        String sql = """
                SELECT * FROM habit_schedule_versions
                WHERE habit_id = :habitId
                  AND effective_from <= :date
                  AND (effective_until IS NULL OR effective_until >= :date)
                ORDER BY effective_from DESC
                LIMIT 1
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("habitId", habitId)
                .addValue("date", Date.valueOf(date));
        return jdbc.query(sql, params, ScheduleVersionRowMapper.INSTANCE).stream().findFirst();
    }

    public Optional<AdministrationHabitScheduleVersion> findLatestVersion(UUID habitId) {
        String sql = """
                SELECT * FROM habit_schedule_versions
                WHERE habit_id = :habitId
                ORDER BY effective_from DESC, created_at DESC
                LIMIT 1
                """;
        return jdbc.query(sql, new MapSqlParameterSource("habitId", habitId), ScheduleVersionRowMapper.INSTANCE)
                .stream().findFirst();
    }

    public List<AdministrationHabitScheduleVersion> findByHabitId(UUID habitId) {
        String sql = "SELECT * FROM habit_schedule_versions WHERE habit_id = :habitId ORDER BY effective_from ASC";
        return jdbc.query(sql, new MapSqlParameterSource("habitId", habitId), ScheduleVersionRowMapper.INSTANCE);
    }

    public void closeActiveVersions(UUID habitId, LocalDate effectiveUntil) {
        String sql = """
                UPDATE habit_schedule_versions
                SET effective_until = :effectiveUntil
                WHERE habit_id = :habitId AND effective_until IS NULL
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("habitId", habitId)
                .addValue("effectiveUntil", Date.valueOf(effectiveUntil));
        jdbc.update(sql, params);
    }

    private MapSqlParameterSource toParams(AdministrationHabitScheduleVersion v) {
        return new MapSqlParameterSource()
                .addValue("id", v.getId())
                .addValue("habitId", v.getHabitId())
                .addValue("scheduleConfig", v.getScheduleConfig() != null ? v.getScheduleConfig() : "{}")
                .addValue("targetValue", v.getTargetValue())
                .addValue("minimumTargetValue", v.getMinimumTargetValue())
                .addValue("targetUnit", v.getTargetUnit())
                .addValue("effectiveFrom", v.getEffectiveFrom() != null ? Date.valueOf(v.getEffectiveFrom()) : null)
                .addValue("effectiveUntil", v.getEffectiveUntil() != null ? Date.valueOf(v.getEffectiveUntil()) : null);
    }

    private static class ScheduleVersionRowMapper implements RowMapper<AdministrationHabitScheduleVersion> {
        static final ScheduleVersionRowMapper INSTANCE = new ScheduleVersionRowMapper();

        @Override
        public AdministrationHabitScheduleVersion mapRow(ResultSet rs, int rowNum) throws SQLException {
            AdministrationHabitScheduleVersion v = new AdministrationHabitScheduleVersion();
            v.setId(rs.getObject("id", UUID.class));
            v.setHabitId(rs.getObject("habit_id", UUID.class));
            v.setScheduleConfig(rs.getString("schedule_config"));
            v.setTargetValue(rs.getBigDecimal("target_value"));
            v.setMinimumTargetValue(rs.getBigDecimal("minimum_target_value"));
            v.setTargetUnit(rs.getString("target_unit"));

            Date from = rs.getDate("effective_from");
            if (from != null) v.setEffectiveFrom(from.toLocalDate());

            Date until = rs.getDate("effective_until");
            if (until != null) v.setEffectiveUntil(until.toLocalDate());

            Timestamp created = rs.getTimestamp("created_at");
            if (created != null) v.setCreatedAt(created.toInstant());

            return v;
        }
    }
}

