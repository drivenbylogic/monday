package com.monday.app.administration.repository;

import com.monday.app.administration.entity.AdministrationNote;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AdministrationNoteRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AdministrationNoteRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public AdministrationNote save(AdministrationNote n) {
        UUID id = n.getId() != null ? n.getId() : UUID.randomUUID();
        n.setId(id);
        String sql = """
                INSERT INTO administration_notes (id, title, body)
                VALUES (:id, :title, :body)
                RETURNING *
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("title", n.getTitle())
                .addValue("body", n.getBody());
        return jdbc.queryForObject(sql, params, NoteRowMapper.INSTANCE);
    }

    public AdministrationNote update(AdministrationNote n) {
        String sql = """
                UPDATE administration_notes SET
                    title = :title,
                    body = :body
                WHERE id = :id
                RETURNING *
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", n.getId())
                .addValue("title", n.getTitle())
                .addValue("body", n.getBody());
        return jdbc.queryForObject(sql, params, NoteRowMapper.INSTANCE);
    }

    public Optional<AdministrationNote> findById(UUID id) {
        String sql = "SELECT * FROM administration_notes WHERE id = :id";
        return jdbc.query(sql, new MapSqlParameterSource("id", id), NoteRowMapper.INSTANCE)
                .stream().findFirst();
    }

    public List<AdministrationNote> findAll() {
        String sql = "SELECT * FROM administration_notes ORDER BY updated_at DESC";
        return jdbc.query(sql, NoteRowMapper.INSTANCE);
    }

    public List<AdministrationNote> search(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }
        String sql = """
                SELECT * FROM administration_notes
                WHERE LOWER(title) LIKE :q OR LOWER(body) LIKE :q
                ORDER BY updated_at DESC
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("q", "%" + query.toLowerCase().trim() + "%");
        return jdbc.query(sql, params, NoteRowMapper.INSTANCE);
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM administration_notes WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    public int countAll() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM administration_notes",
                new MapSqlParameterSource(), Integer.class);
        return count != null ? count : 0;
    }

    public List<AdministrationNote> findRecent(int limit) {
        String sql = "SELECT * FROM administration_notes ORDER BY updated_at DESC LIMIT :limit";
        return jdbc.query(sql, new MapSqlParameterSource("limit", limit), NoteRowMapper.INSTANCE);
    }

    static final class NoteRowMapper implements RowMapper<AdministrationNote> {
        static final NoteRowMapper INSTANCE = new NoteRowMapper();

        @Override
        public AdministrationNote mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new AdministrationNote(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("title"),
                    rs.getString("body"),
                    rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : Instant.now(),
                    rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toInstant() : Instant.now()
            );
        }
    }
}
