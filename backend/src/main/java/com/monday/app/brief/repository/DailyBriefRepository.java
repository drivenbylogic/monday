package com.monday.app.brief.repository;

import com.monday.app.brief.entity.DailyBrief;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DailyBriefRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public DailyBriefRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public DailyBrief save(DailyBrief brief) {
        UUID id = brief.getId() != null ? brief.getId() : UUID.randomUUID();
        String sql = """
                INSERT INTO daily_briefs (id, brief_date, structured_agenda_json, ai_summary_markdown, status)
                VALUES (:id, :briefDate, :structuredAgendaJson::jsonb, :aiSummaryMarkdown, :status)
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(id, brief), DailyBriefRowMapper.INSTANCE);
    }

    public DailyBrief update(DailyBrief brief) {
        String sql = """
                UPDATE daily_briefs SET
                    brief_date = :briefDate, structured_agenda_json = :structuredAgendaJson::jsonb,
                    ai_summary_markdown = :aiSummaryMarkdown, status = :status
                WHERE id = :id RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(brief.getId(), brief), DailyBriefRowMapper.INSTANCE);
    }

    public Optional<DailyBrief> findByDate(LocalDate date) {
        return jdbc.query("SELECT * FROM daily_briefs WHERE brief_date = :date",
                new MapSqlParameterSource("date", Date.valueOf(date)), DailyBriefRowMapper.INSTANCE).stream().findFirst();
    }

    public List<DailyBrief> findRecent(int days) {
        String sql = "SELECT * FROM daily_briefs ORDER BY brief_date DESC LIMIT :days";
        return jdbc.query(sql, new MapSqlParameterSource("days", days), DailyBriefRowMapper.INSTANCE);
    }

    public void deleteOlderThan(LocalDate cutoffDate) {
        jdbc.update("DELETE FROM daily_briefs WHERE brief_date < :cutoffDate",
                new MapSqlParameterSource("cutoffDate", Date.valueOf(cutoffDate)));
    }

    private MapSqlParameterSource toParams(UUID id, DailyBrief b) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("briefDate", Date.valueOf(b.getBriefDate()))
                .addValue("structuredAgendaJson", b.getStructuredAgendaJson())
                .addValue("aiSummaryMarkdown", b.getAiSummaryMarkdown())
                .addValue("status", b.getStatus());
    }

    static final class DailyBriefRowMapper implements RowMapper<DailyBrief> {
        static final DailyBriefRowMapper INSTANCE = new DailyBriefRowMapper();
        @Override
        public DailyBrief mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new DailyBrief(
                    UUID.fromString(rs.getString("id")),
                    rs.getDate("brief_date").toLocalDate(),
                    rs.getString("structured_agenda_json"),
                    rs.getString("ai_summary_markdown"),
                    rs.getString("status"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()
            );
        }
    }
}
