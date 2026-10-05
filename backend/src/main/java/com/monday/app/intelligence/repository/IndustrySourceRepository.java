package com.monday.app.intelligence.repository;

import com.monday.app.intelligence.entity.IndustrySource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class IndustrySourceRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public IndustrySourceRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public IndustrySource save(IndustrySource source) {
        UUID id = source.getId() != null ? source.getId() : UUID.randomUUID();
        String sql = """
                INSERT INTO industry_sources (id, name, feed_url, source_type, is_active)
                VALUES (:id, :name, :feedUrl, :sourceType, :isActive)
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(id, source), IndustrySourceRowMapper.INSTANCE);
    }

    public IndustrySource update(IndustrySource source) {
        String sql = """
                UPDATE industry_sources SET
                    name = :name, feed_url = :feedUrl, source_type = :sourceType, is_active = :isActive
                WHERE id = :id RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(source.getId(), source), IndustrySourceRowMapper.INSTANCE);
    }

    public Optional<IndustrySource> findById(UUID id) {
        return jdbc.query("SELECT * FROM industry_sources WHERE id = :id",
                new MapSqlParameterSource("id", id), IndustrySourceRowMapper.INSTANCE).stream().findFirst();
    }

    public List<IndustrySource> findAllActive() {
        return jdbc.query("SELECT * FROM industry_sources WHERE is_active = true ORDER BY name", IndustrySourceRowMapper.INSTANCE);
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM industry_sources WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource toParams(UUID id, IndustrySource s) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("name", s.getName())
                .addValue("feedUrl", s.getFeedUrl())
                .addValue("sourceType", s.getSourceType())
                .addValue("isActive", s.isActive());
    }

    static final class IndustrySourceRowMapper implements RowMapper<IndustrySource> {
        static final IndustrySourceRowMapper INSTANCE = new IndustrySourceRowMapper();
        @Override
        public IndustrySource mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new IndustrySource(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("name"),
                    rs.getString("feed_url"),
                    rs.getString("source_type"),
                    rs.getBoolean("is_active"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()
            );
        }
    }
}
