package com.monday.app.agenda.repository;

import com.monday.app.agenda.entity.BetaDailyBrief;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public class BetaDailyBriefRepository {

    private static final Logger log = LoggerFactory.getLogger(BetaDailyBriefRepository.class);

    private final NamedParameterJdbcTemplate jdbc;

    public BetaDailyBriefRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void ensureTableExists() {
        try {
            String createTableSql = """
                    CREATE TABLE IF NOT EXISTS beta_daily_briefs (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        brief_date DATE NOT NULL UNIQUE,
                        generation_mode VARCHAR(50) NOT NULL DEFAULT 'AI',
                        status VARCHAR(50) NOT NULL DEFAULT 'GENERATED',
                        snapshot_json JSONB NOT NULL,
                        briefing_json JSONB NOT NULL,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
                    );
                    CREATE INDEX IF NOT EXISTS idx_beta_daily_briefs_date ON beta_daily_briefs(brief_date);
                    """;
            jdbc.getJdbcOperations().execute(createTableSql);
            log.info("Initialized beta_daily_briefs schema verification");
        } catch (Exception e) {
            log.warn("Could not run automated DDL for beta_daily_briefs (table may already exist or running in restricted mode): {}", e.getMessage());
        }
    }

    public BetaDailyBrief save(BetaDailyBrief brief) {
        UUID id = brief.getId() != null ? brief.getId() : UUID.randomUUID();
        String sql = """
                INSERT INTO beta_daily_briefs (id, brief_date, generation_mode, status, snapshot_json, briefing_json)
                VALUES (:id, :briefDate, :generationMode, :status, :snapshotJson::jsonb, :briefingJson::jsonb)
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(id, brief), BetaDailyBriefRowMapper.INSTANCE);
    }

    public BetaDailyBrief update(BetaDailyBrief brief) {
        String sql = """
                UPDATE beta_daily_briefs SET
                    brief_date = :briefDate,
                    generation_mode = :generationMode,
                    status = :status,
                    snapshot_json = :snapshotJson::jsonb,
                    briefing_json = :briefingJson::jsonb,
                    updated_at = NOW()
                WHERE id = :id RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(brief.getId(), brief), BetaDailyBriefRowMapper.INSTANCE);
    }

    public Optional<BetaDailyBrief> findByDate(LocalDate date) {
        try {
            String sql = "SELECT * FROM beta_daily_briefs WHERE brief_date = :date";
            return jdbc.query(sql, new MapSqlParameterSource("date", Date.valueOf(date)), BetaDailyBriefRowMapper.INSTANCE)
                    .stream()
                    .findFirst();
        } catch (Exception e) {
            log.warn("Failed to find beta brief by date {}: {}", date, e.getMessage());
            return Optional.empty();
        }
    }

    private MapSqlParameterSource toParams(UUID id, BetaDailyBrief b) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("briefDate", Date.valueOf(b.getBriefDate()))
                .addValue("generationMode", b.getGenerationMode())
                .addValue("status", b.getStatus())
                .addValue("snapshotJson", b.getSnapshotJson())
                .addValue("briefingJson", b.getBriefingJson());
    }

    static final class BetaDailyBriefRowMapper implements RowMapper<BetaDailyBrief> {
        static final BetaDailyBriefRowMapper INSTANCE = new BetaDailyBriefRowMapper();

        @Override
        public BetaDailyBrief mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new BetaDailyBrief(
                    UUID.fromString(rs.getString("id")),
                    rs.getDate("brief_date").toLocalDate(),
                    rs.getString("generation_mode"),
                    rs.getString("status"),
                    rs.getString("snapshot_json"),
                    rs.getString("briefing_json"),
                    rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null,
                    rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toInstant() : null
            );
        }
    }
}

