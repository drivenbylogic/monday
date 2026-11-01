package com.monday.app.administration.repository;

import com.monday.app.administration.entity.AdministrationHabitPausePeriod;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AdministrationHabitPausePeriodRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AdministrationHabitPausePeriodRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public AdministrationHabitPausePeriod save(AdministrationHabitPausePeriod p) {
        UUID id = p.getId() != null ? p.getId() : UUID.randomUUID();
        p.setId(id);
        String sql = """
                INSERT INTO habit_pause_periods (
                    id, habit_id, start_date, end_date, reason
                ) VALUES (
                    :id, :habitId, :startDate, :endDate, :reason
                ) RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(p), PausePeriodRowMapper.INSTANCE);
    }

    public boolean isHabitPausedOnDate(UUID habitId, LocalDate date) {
        String sql = """
                SELECT COUNT(*) FROM habit_pause_periods
                WHERE habit_id = :habitId
                  AND start_date <= :date
                  AND (end_date IS NULL OR end_date >= :date)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("habitId", habitId)
                .addValue("date", Date.valueOf(date));
        Integer count = jdbc.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    public List<AdministrationHabitPausePeriod> findByHabitId(UUID habitId) {
        String sql = "SELECT * FROM habit_pause_periods WHERE habit_id = :habitId ORDER BY start_date DESC";
        return jdbc.query(sql, new MapSqlParameterSource("habitId", habitId), PausePeriodRowMapper.INSTANCE);
    }

    public void closeOpenPauses(UUID habitId, LocalDate resumeDate) {
        String sql = """
                UPDATE habit_pause_periods
                SET end_date = :resumeDate
                WHERE habit_id = :habitId AND (end_date IS NULL OR end_date > :resumeDate)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("habitId", habitId)
                .addValue("resumeDate", Date.valueOf(resumeDate));
        jdbc.update(sql, params);
    }

    private MapSqlParameterSource toParams(AdministrationHabitPausePeriod p) {
        return new MapSqlParameterSource()
                .addValue("id", p.getId())
                .addValue("habitId", p.getHabitId())
                .addValue("startDate", p.getStartDate() != null ? Date.valueOf(p.getStartDate()) : null)
                .addValue("endDate", p.getEndDate() != null ? Date.valueOf(p.getEndDate()) : null)
                .addValue("reason", p.getReason());
    }

    private static class PausePeriodRowMapper implements RowMapper<AdministrationHabitPausePeriod> {
        static final PausePeriodRowMapper INSTANCE = new PausePeriodRowMapper();

        @Override
        public AdministrationHabitPausePeriod mapRow(ResultSet rs, int rowNum) throws SQLException {
            AdministrationHabitPausePeriod p = new AdministrationHabitPausePeriod();
            p.setId(rs.getObject("id", UUID.class));
            p.setHabitId(rs.getObject("habit_id", UUID.class));

            Date start = rs.getDate("start_date");
            if (start != null) p.setStartDate(start.toLocalDate());

            Date end = rs.getDate("end_date");
            if (end != null) p.setEndDate(end.toLocalDate());

            p.setReason(rs.getString("reason"));

            Timestamp created = rs.getTimestamp("created_at");
            if (created != null) p.setCreatedAt(created.toInstant());

            return p;
        }
    }
}

