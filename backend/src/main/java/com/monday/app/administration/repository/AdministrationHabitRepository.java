package com.monday.app.administration.repository;

import com.monday.app.administration.entity.AdministrationHabit;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AdministrationHabitRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AdministrationHabitRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public AdministrationHabit save(AdministrationHabit h) {
        UUID id = h.getId() != null ? h.getId() : UUID.randomUUID();
        h.setId(id);
        String sql = """
                INSERT INTO habits (
                    id, user_id, group_id, name, description, category, frequency_type,
                    target_value, minimum_target_value, target_unit, priority, start_date, end_date,
                    status, display_order, estimated_duration
                ) VALUES (
                    :id, :userId, :groupId, :name, :description, :category, :frequencyType,
                    :targetValue, :minimumTargetValue, :targetUnit, :priority, :startDate, :endDate,
                    :status, :displayOrder, :estimatedDuration
                ) RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(h), HabitRowMapper.INSTANCE);
    }

    public AdministrationHabit update(AdministrationHabit h) {
        String sql = """
                UPDATE habits SET
                    group_id = :groupId,
                    name = :name,
                    description = :description,
                    category = :category,
                    frequency_type = :frequencyType,
                    target_value = :targetValue,
                    minimum_target_value = :minimumTargetValue,
                    target_unit = :targetUnit,
                    priority = :priority,
                    start_date = :startDate,
                    end_date = :endDate,
                    status = :status,
                    display_order = :displayOrder,
                    estimated_duration = :estimatedDuration
                WHERE id = :id AND (user_id = :userId OR :userId IS NULL OR user_id IS NULL)
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(h), HabitRowMapper.INSTANCE);
    }

    public Optional<AdministrationHabit> findById(UUID id) {
        String sql = "SELECT * FROM habits WHERE id = :id";
        return jdbc.query(sql, new MapSqlParameterSource("id", id), HabitRowMapper.INSTANCE)
                .stream().findFirst();
    }

    public Optional<AdministrationHabit> findByIdAndUserId(UUID id, UUID userId) {
        String sql = "SELECT * FROM habits WHERE id = :id AND (user_id = :userId OR :userId IS NULL OR user_id IS NULL)";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("userId", userId);
        return jdbc.query(sql, params, HabitRowMapper.INSTANCE).stream().findFirst();
    }

    public List<AdministrationHabit> search(UUID userId, String status, String category, UUID groupId, String search) {
        StringBuilder sql = new StringBuilder("SELECT * FROM habits WHERE 1=1");
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (userId != null) {
            sql.append(" AND (user_id = :userId OR user_id IS NULL)");
            params.addValue("userId", userId);
        }

        if (status != null && !status.isBlank()) {
            sql.append(" AND status = :status");
            params.addValue("status", status.toUpperCase());
        }

        if (category != null && !category.isBlank()) {
            sql.append(" AND LOWER(category) = :category");
            params.addValue("category", category.toLowerCase().trim());
        }

        if (groupId != null) {
            sql.append(" AND group_id = :groupId");
            params.addValue("groupId", groupId);
        }

        if (search != null && !search.isBlank()) {
            sql.append(" AND (LOWER(name) LIKE :q OR LOWER(description) LIKE :q)");
            params.addValue("q", "%" + search.toLowerCase().trim() + "%");
        }

        sql.append(" ORDER BY display_order ASC, name ASC");
        return jdbc.query(sql.toString(), params, HabitRowMapper.INSTANCE);
    }

    public List<AdministrationHabit> findActiveHabits(UUID userId) {
        String sql = """
                SELECT * FROM habits
                WHERE (user_id = :userId OR :userId IS NULL OR user_id IS NULL)
                  AND status = 'ACTIVE'
                ORDER BY display_order ASC, name ASC
                """;
        return jdbc.query(sql, new MapSqlParameterSource("userId", userId), HabitRowMapper.INSTANCE);
    }

    public List<AdministrationHabit> findAllForUser(UUID userId) {
        String sql = """
                SELECT * FROM habits
                WHERE (user_id = :userId OR :userId IS NULL OR user_id IS NULL)
                ORDER BY status ASC, display_order ASC, name ASC
                """;
        return jdbc.query(sql, new MapSqlParameterSource("userId", userId), HabitRowMapper.INSTANCE);
    }

    public void updateStatus(UUID id, String status) {
        String sql = "UPDATE habits SET status = :status WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("status", status);
        jdbc.update(sql, params);
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM habits WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    public int countActive(UUID userId) {
        String sql = "SELECT COUNT(*) FROM habits WHERE (user_id = :userId OR :userId IS NULL OR user_id IS NULL) AND status = 'ACTIVE'";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("userId", userId), Integer.class);
        return count != null ? count : 0;
    }

    private MapSqlParameterSource toParams(AdministrationHabit h) {
        return new MapSqlParameterSource()
                .addValue("id", h.getId())
                .addValue("userId", h.getUserId())
                .addValue("groupId", h.getGroupId())
                .addValue("name", h.getName())
                .addValue("description", h.getDescription())
                .addValue("category", h.getCategory())
                .addValue("frequencyType", h.getFrequencyType())
                .addValue("targetValue", h.getTargetValue())
                .addValue("minimumTargetValue", h.getMinimumTargetValue())
                .addValue("targetUnit", h.getTargetUnit())
                .addValue("priority", h.getPriority() != null ? h.getPriority() : "MEDIUM")
                .addValue("startDate", h.getStartDate() != null ? Date.valueOf(h.getStartDate()) : null)
                .addValue("endDate", h.getEndDate() != null ? Date.valueOf(h.getEndDate()) : null)
                .addValue("status", h.getStatus() != null ? h.getStatus() : "ACTIVE")
                .addValue("displayOrder", h.getDisplayOrder())
                .addValue("estimatedDuration", h.getEstimatedDuration());
    }

    private static class HabitRowMapper implements RowMapper<AdministrationHabit> {
        static final HabitRowMapper INSTANCE = new HabitRowMapper();

        @Override
        public AdministrationHabit mapRow(ResultSet rs, int rowNum) throws SQLException {
            AdministrationHabit h = new AdministrationHabit();
            h.setId(rs.getObject("id", UUID.class));
            h.setUserId(rs.getObject("user_id", UUID.class));
            h.setGroupId(rs.getObject("group_id", UUID.class));
            h.setName(rs.getString("name"));
            h.setDescription(rs.getString("description"));
            h.setCategory(rs.getString("category"));
            h.setFrequencyType(rs.getString("frequency_type"));
            h.setTargetValue(rs.getBigDecimal("target_value"));
            h.setMinimumTargetValue(rs.getBigDecimal("minimum_target_value"));
            h.setTargetUnit(rs.getString("target_unit"));
            h.setPriority(rs.getString("priority"));

            Date start = rs.getDate("start_date");
            if (start != null) h.setStartDate(start.toLocalDate());

            Date end = rs.getDate("end_date");
            if (end != null) h.setEndDate(end.toLocalDate());

            h.setStatus(rs.getString("status"));
            h.setDisplayOrder(rs.getInt("display_order"));

            int duration = rs.getInt("estimated_duration");
            if (!rs.wasNull()) h.setEstimatedDuration(duration);

            Timestamp created = rs.getTimestamp("created_at");
            if (created != null) h.setCreatedAt(created.toInstant());

            Timestamp updated = rs.getTimestamp("updated_at");
            if (updated != null) h.setUpdatedAt(updated.toInstant());

            return h;
        }
    }
}

