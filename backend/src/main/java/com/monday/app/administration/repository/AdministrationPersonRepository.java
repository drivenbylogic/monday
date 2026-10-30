package com.monday.app.administration.repository;

import com.monday.app.administration.entity.AdministrationPerson;
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
public class AdministrationPersonRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AdministrationPersonRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public AdministrationPerson save(AdministrationPerson p) {
        UUID id = p.getId() != null ? p.getId() : UUID.randomUUID();
        p.setId(id);
        String sql = """
                INSERT INTO administration_people (
                    id, display_name, relationship_label, birthday, contact_information, notes, archived_at
                ) VALUES (
                    :id, :displayName, :relationshipLabel, :birthday, :contactInformation, :notes, :archivedAt
                ) RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(p), PersonRowMapper.INSTANCE);
    }

    public AdministrationPerson update(AdministrationPerson p) {
        String sql = """
                UPDATE administration_people SET
                    display_name = :displayName,
                    relationship_label = :relationshipLabel,
                    birthday = :birthday,
                    contact_information = :contactInformation,
                    notes = :notes,
                    archived_at = :archivedAt
                WHERE id = :id
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(p), PersonRowMapper.INSTANCE);
    }

    public Optional<AdministrationPerson> findById(UUID id) {
        String sql = "SELECT * FROM administration_people WHERE id = :id";
        return jdbc.query(sql, new MapSqlParameterSource("id", id), PersonRowMapper.INSTANCE)
                .stream().findFirst();
    }

    public List<AdministrationPerson> findAll(boolean includeArchived) {
        String sql = includeArchived
                ? "SELECT * FROM administration_people ORDER BY display_name ASC"
                : "SELECT * FROM administration_people WHERE archived_at IS NULL ORDER BY display_name ASC";
        return jdbc.query(sql, PersonRowMapper.INSTANCE);
    }

    public List<AdministrationPerson> search(String query, Boolean archived) {
        StringBuilder sql = new StringBuilder("SELECT * FROM administration_people WHERE 1=1");
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (archived != null && !archived) {
            sql.append(" AND archived_at IS NULL");
        } else if (archived != null) {
            sql.append(" AND archived_at IS NOT NULL");
        }

        if (query != null && !query.isBlank()) {
            sql.append(" AND (LOWER(display_name) LIKE :q OR LOWER(relationship_label) LIKE :q OR LOWER(notes) LIKE :q)");
            params.addValue("q", "%" + query.toLowerCase().trim() + "%");
        }

        sql.append(" ORDER BY display_name ASC");
        return jdbc.query(sql.toString(), params, PersonRowMapper.INSTANCE);
    }

    public void archive(UUID id) {
        jdbc.update("UPDATE administration_people SET archived_at = CURRENT_TIMESTAMP WHERE id = :id",
                new MapSqlParameterSource("id", id));
    }

    public void unarchive(UUID id) {
        jdbc.update("UPDATE administration_people SET archived_at = NULL WHERE id = :id",
                new MapSqlParameterSource("id", id));
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM administration_people WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    public int countAll() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM administration_people WHERE archived_at IS NULL",
                new MapSqlParameterSource(), Integer.class);
        return count != null ? count : 0;
    }

    public int countOccasions(UUID personId) {
        String sql = "SELECT COUNT(*) FROM administration_important_dates WHERE person_id = :personId AND archived_at IS NULL";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("personId", personId), Integer.class);
        return count != null ? count : 0;
    }

    private MapSqlParameterSource toParams(AdministrationPerson p) {
        return new MapSqlParameterSource()
                .addValue("id", p.getId())
                .addValue("displayName", p.getDisplayName())
                .addValue("relationshipLabel", p.getRelationshipLabel())
                .addValue("birthday", p.getBirthday())
                .addValue("contactInformation", p.getContactInformation())
                .addValue("notes", p.getNotes())
                .addValue("archivedAt", p.getArchivedAt() != null ? Timestamp.from(p.getArchivedAt()) : null);
    }

    static final class PersonRowMapper implements RowMapper<AdministrationPerson> {
        static final PersonRowMapper INSTANCE = new PersonRowMapper();

        @Override
        public AdministrationPerson mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new AdministrationPerson(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("display_name"),
                    rs.getString("relationship_label"),
                    rs.getDate("birthday") != null ? rs.getDate("birthday").toLocalDate() : null,
                    rs.getString("contact_information"),
                    rs.getString("notes"),
                    rs.getTimestamp("archived_at") != null ? rs.getTimestamp("archived_at").toInstant() : null,
                    rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : Instant.now(),
                    rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toInstant() : Instant.now()
            );
        }
    }
}
