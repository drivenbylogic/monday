package com.monday.app.administration.repository;

import com.monday.app.administration.entity.AdministrationHabitGroup;
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
public class AdministrationHabitGroupRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AdministrationHabitGroupRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public AdministrationHabitGroup save(AdministrationHabitGroup g) {
        UUID id = g.getId() != null ? g.getId() : UUID.randomUUID();
        g.setId(id);
        String sql = """
                INSERT INTO habit_groups (
                    id, user_id, name, description, display_order
                ) VALUES (
                    :id, :userId, :name, :description, :displayOrder
                ) RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(g), HabitGroupRowMapper.INSTANCE);
    }

    public AdministrationHabitGroup update(AdministrationHabitGroup g) {
        String sql = """
                UPDATE habit_groups SET
                    name = :name,
                    description = :description,
                    display_order = :displayOrder
                WHERE id = :id AND (user_id = :userId OR :userId IS NULL OR user_id IS NULL)
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(g), HabitGroupRowMapper.INSTANCE);
    }

    public Optional<AdministrationHabitGroup> findById(UUID id) {
        String sql = "SELECT * FROM habit_groups WHERE id = :id";
        return jdbc.query(sql, new MapSqlParameterSource("id", id), HabitGroupRowMapper.INSTANCE)
                .stream().findFirst();
    }

    public Optional<AdministrationHabitGroup> findByIdAndUserId(UUID id, UUID userId) {
        String sql = "SELECT * FROM habit_groups WHERE id = :id AND (user_id = :userId OR :userId IS NULL OR user_id IS NULL)";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("userId", userId);
        return jdbc.query(sql, params, HabitGroupRowMapper.INSTANCE)
                .stream().findFirst();
    }

    public List<AdministrationHabitGroup> findAllByUserId(UUID userId) {
        String sql = "SELECT * FROM habit_groups WHERE (user_id = :userId OR :userId IS NULL OR user_id IS NULL) ORDER BY display_order ASC, name ASC";
        return jdbc.query(sql, new MapSqlParameterSource("userId", userId), HabitGroupRowMapper.INSTANCE);
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM habit_groups WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource toParams(AdministrationHabitGroup g) {
        return new MapSqlParameterSource()
                .addValue("id", g.getId())
                .addValue("userId", g.getUserId())
                .addValue("name", g.getName())
                .addValue("description", g.getDescription())
                .addValue("displayOrder", g.getDisplayOrder());
    }

    private static class HabitGroupRowMapper implements RowMapper<AdministrationHabitGroup> {
        static final HabitGroupRowMapper INSTANCE = new HabitGroupRowMapper();

        @Override
        public AdministrationHabitGroup mapRow(ResultSet rs, int rowNum) throws SQLException {
            AdministrationHabitGroup g = new AdministrationHabitGroup();
            g.setId(rs.getObject("id", UUID.class));
            g.setUserId(rs.getObject("user_id", UUID.class));
            g.setName(rs.getString("name"));
            g.setDescription(rs.getString("description"));
            g.setDisplayOrder(rs.getInt("display_order"));

            Timestamp created = rs.getTimestamp("created_at");
            if (created != null) g.setCreatedAt(created.toInstant());

            Timestamp updated = rs.getTimestamp("updated_at");
            if (updated != null) g.setUpdatedAt(updated.toInstant());

            return g;
        }
    }
}

