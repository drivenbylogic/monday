package com.monday.app.intelligence.repository;

import com.monday.app.intelligence.entity.IndustrySummary;
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
public class IndustrySummaryRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public IndustrySummaryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public IndustrySummary save(IndustrySummary summary) {
        UUID id = summary.getId() != null ? summary.getId() : UUID.randomUUID();
        String sql = """
                INSERT INTO industry_summaries (id, summary_date, title, content_markdown, sources_json)
                VALUES (:id, :summaryDate, :title, :contentMarkdown, :sourcesJson::jsonb)
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(id, summary), IndustrySummaryRowMapper.INSTANCE);
    }

    public Optional<IndustrySummary> findByDate(LocalDate date) {
        return jdbc.query("SELECT * FROM industry_summaries WHERE summary_date = :date",
                new MapSqlParameterSource("date", Date.valueOf(date)), IndustrySummaryRowMapper.INSTANCE).stream().findFirst();
    }

    public List<IndustrySummary> findRecent(int days) {
        String sql = "SELECT * FROM industry_summaries ORDER BY summary_date DESC LIMIT :days";
        return jdbc.query(sql, new MapSqlParameterSource("days", days), IndustrySummaryRowMapper.INSTANCE);
    }

    public void deleteOlderThan(LocalDate cutoffDate) {
        jdbc.update("DELETE FROM industry_summaries WHERE summary_date < :cutoffDate",
                new MapSqlParameterSource("cutoffDate", Date.valueOf(cutoffDate)));
    }

    private MapSqlParameterSource toParams(UUID id, IndustrySummary s) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("summaryDate", Date.valueOf(s.getSummaryDate()))
                .addValue("title", s.getTitle())
                .addValue("contentMarkdown", s.getContentMarkdown())
                .addValue("sourcesJson", s.getSourcesJson());
    }

    static final class IndustrySummaryRowMapper implements RowMapper<IndustrySummary> {
        static final IndustrySummaryRowMapper INSTANCE = new IndustrySummaryRowMapper();
        @Override
        public IndustrySummary mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new IndustrySummary(
                    UUID.fromString(rs.getString("id")),
                    rs.getDate("summary_date").toLocalDate(),
                    rs.getString("title"),
                    rs.getString("content_markdown"),
                    rs.getString("sources_json"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()
            );
        }
    }
}
