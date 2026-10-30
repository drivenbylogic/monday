package com.monday.app.administration.repository;

import com.monday.app.administration.entity.AdministrationImportantDate;
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
public class AdministrationImportantDateRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AdministrationImportantDateRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public AdministrationImportantDate save(AdministrationImportantDate d) {
        UUID id = d.getId() != null ? d.getId() : UUID.randomUUID();
        d.setId(id);
        String sql = """
                INSERT INTO administration_important_dates (
                    id, title, description, date, date_type, person_id,
                    recurrence_type, recurrence_rule, reminder_lead_days, archived_at
                ) VALUES (
                    :id, :title, :description, :date, :dateType, :personId,
                    :recurrenceType, :recurrenceRule, :reminderLeadDays, :archivedAt
                ) RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(d), DateRowMapper.INSTANCE);
    }

    public AdministrationImportantDate update(AdministrationImportantDate d) {
        String sql = """
                UPDATE administration_important_dates SET
                    title = :title,
                    description = :description,
                    date = :date,
                    date_type = :dateType,
                    person_id = :personId,
                    recurrence_type = :recurrenceType,
                    recurrence_rule = :recurrenceRule,
                    reminder_lead_days = :reminderLeadDays,
                    archived_at = :archivedAt
                WHERE id = :id
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(d), DateRowMapper.INSTANCE);
    }

    public Optional<AdministrationImportantDate> findById(UUID id) {
        String sql = "SELECT * FROM administration_important_dates WHERE id = :id";
        return jdbc.query(sql, new MapSqlParameterSource("id", id), DateRowMapper.INSTANCE)
                .stream().findFirst();
    }

    public List<AdministrationImportantDate> findAll(boolean includeArchived) {
        String sql = includeArchived
                ? "SELECT * FROM administration_important_dates ORDER BY date ASC"
                : "SELECT * FROM administration_important_dates WHERE archived_at IS NULL ORDER BY date ASC";
        return jdbc.query(sql, DateRowMapper.INSTANCE);
    }

    public List<AdministrationImportantDate> findByPersonId(UUID personId, boolean includeArchived) {
        String sql = includeArchived
                ? "SELECT * FROM administration_important_dates WHERE person_id = :personId ORDER BY date ASC"
                : "SELECT * FROM administration_important_dates WHERE person_id = :personId AND archived_at IS NULL ORDER BY date ASC";
        return jdbc.query(sql, new MapSqlParameterSource("personId", personId), DateRowMapper.INSTANCE);
    }

    public List<AdministrationImportantDate> search(String query, String dateType, Boolean archived) {
        StringBuilder sql = new StringBuilder("SELECT * FROM administration_important_dates WHERE 1=1");
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (archived != null && !archived) {
            sql.append(" AND archived_at IS NULL");
        } else if (archived != null) {
            sql.append(" AND archived_at IS NOT NULL");
        }

        if (dateType != null && !dateType.isBlank() && !"all".equalsIgnoreCase(dateType)) {
            sql.append(" AND date_type = :dateType");
            params.addValue("dateType", dateType);
        }

        if (query != null && !query.isBlank()) {
            sql.append(" AND (LOWER(title) LIKE :q OR LOWER(description) LIKE :q)");
            params.addValue("q", "%" + query.toLowerCase().trim() + "%");
        }

        sql.append(" ORDER BY date ASC");
        return jdbc.query(sql.toString(), params, DateRowMapper.INSTANCE);
    }

    public void archive(UUID id) {
        jdbc.update("UPDATE administration_important_dates SET archived_at = CURRENT_TIMESTAMP WHERE id = :id",
                new MapSqlParameterSource("id", id));
    }

    public void unarchive(UUID id) {
        jdbc.update("UPDATE administration_important_dates SET archived_at = NULL WHERE id = :id",
                new MapSqlParameterSource("id", id));
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM administration_important_dates WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    public int countAll() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM administration_important_dates WHERE archived_at IS NULL",
                new MapSqlParameterSource(), Integer.class);
        return count != null ? count : 0;
    }

    public String findPersonName(UUID personId) {
        if (personId == null) return null;
        String sql = "SELECT display_name FROM administration_people WHERE id = :personId";
        List<String> names = jdbc.query(sql, new MapSqlParameterSource("personId", personId), (rs, rowNum) -> rs.getString("display_name"));
        return names.isEmpty() ? null : names.get(0);
    }

    private MapSqlParameterSource toParams(AdministrationImportantDate d) {
        return new MapSqlParameterSource()
                .addValue("id", d.getId())
                .addValue("title", d.getTitle())
                .addValue("description", d.getDescription())
                .addValue("date", d.getDate())
                .addValue("dateType", d.getDateType())
                .addValue("personId", d.getPersonId())
                .addValue("recurrenceType", d.getRecurrenceType())
                .addValue("recurrenceRule", d.getRecurrenceRule())
                .addValue("reminderLeadDays", d.getReminderLeadDays())
                .addValue("archivedAt", d.getArchivedAt() != null ? Timestamp.from(d.getArchivedAt()) : null);
    }

    static final class DateRowMapper implements RowMapper<AdministrationImportantDate> {
        static final DateRowMapper INSTANCE = new DateRowMapper();

        @Override
        public AdministrationImportantDate mapRow(ResultSet rs, int rowNum) throws SQLException {
            String personIdStr = rs.getString("person_id");
            return new AdministrationImportantDate(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getDate("date").toLocalDate(),
                    rs.getString("date_type"),
                    personIdStr != null ? UUID.fromString(personIdStr) : null,
                    rs.getString("recurrence_type"),
                    rs.getString("recurrence_rule"),
                    rs.getInt("reminder_lead_days"),
                    rs.getTimestamp("archived_at") != null ? rs.getTimestamp("archived_at").toInstant() : null,
                    rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : Instant.now(),
                    rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toInstant() : Instant.now()
            );
        }
    }
}
