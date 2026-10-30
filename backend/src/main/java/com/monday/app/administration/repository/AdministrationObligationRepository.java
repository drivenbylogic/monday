package com.monday.app.administration.repository;

import com.monday.app.administration.entity.AdministrationObligation;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AdministrationObligationRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AdministrationObligationRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public AdministrationObligation save(AdministrationObligation o) {
        UUID id = o.getId() != null ? o.getId() : UUID.randomUUID();
        o.setId(id);
        String sql = """
                INSERT INTO administration_obligations (
                    id, name, description, provider_name, category, obligation_type,
                    expected_amount, currency, billing_frequency, recurrence_rule,
                    start_date, next_due_date, end_date, trial_end_date,
                    cancellation_deadline, auto_renew, status, archived_at
                ) VALUES (
                    :id, :name, :description, :providerName, :category, :obligationType,
                    :expectedAmount, :currency, :billingFrequency, :recurrenceRule,
                    :startDate, :nextDueDate, :endDate, :trialEndDate,
                    :cancellationDeadline, :autoRenew, :status, :archivedAt
                ) RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(o), ObligationRowMapper.INSTANCE);
    }

    public AdministrationObligation update(AdministrationObligation o) {
        String sql = """
                UPDATE administration_obligations SET
                    name = :name,
                    description = :description,
                    provider_name = :providerName,
                    category = :category,
                    obligation_type = :obligationType,
                    expected_amount = :expectedAmount,
                    currency = :currency,
                    billing_frequency = :billingFrequency,
                    recurrence_rule = :recurrenceRule,
                    start_date = :startDate,
                    next_due_date = :nextDueDate,
                    end_date = :endDate,
                    trial_end_date = :trialEndDate,
                    cancellation_deadline = :cancellationDeadline,
                    auto_renew = :autoRenew,
                    status = :status,
                    archived_at = :archivedAt
                WHERE id = :id
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(o), ObligationRowMapper.INSTANCE);
    }

    public Optional<AdministrationObligation> findById(UUID id) {
        String sql = "SELECT * FROM administration_obligations WHERE id = :id";
        return jdbc.query(sql, new MapSqlParameterSource("id", id), ObligationRowMapper.INSTANCE)
                .stream().findFirst();
    }

    public List<AdministrationObligation> findAll(boolean includeArchived) {
        String sql = includeArchived
                ? "SELECT * FROM administration_obligations ORDER BY next_due_date ASC, created_at DESC"
                : "SELECT * FROM administration_obligations WHERE archived_at IS NULL ORDER BY next_due_date ASC, created_at DESC";
        return jdbc.query(sql, ObligationRowMapper.INSTANCE);
    }

    public List<AdministrationObligation> search(String query, String category, String status, Boolean archived) {
        StringBuilder sql = new StringBuilder("SELECT * FROM administration_obligations WHERE 1=1");
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (archived != null && !archived) {
            sql.append(" AND archived_at IS NULL");
        } else if (archived != null) {
            sql.append(" AND archived_at IS NOT NULL");
        }

        if (category != null && !category.isBlank() && !"all".equalsIgnoreCase(category)) {
            sql.append(" AND category = :category");
            params.addValue("category", category);
        }

        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            sql.append(" AND status = :status");
            params.addValue("status", status);
        }

        if (query != null && !query.isBlank()) {
            sql.append(" AND (LOWER(name) LIKE :q OR LOWER(provider_name) LIKE :q OR LOWER(description) LIKE :q)");
            params.addValue("q", "%" + query.toLowerCase().trim() + "%");
        }

        sql.append(" ORDER BY next_due_date ASC, created_at DESC");
        return jdbc.query(sql.toString(), params, ObligationRowMapper.INSTANCE);
    }

    public void archive(UUID id) {
        String sql = "UPDATE administration_obligations SET archived_at = CURRENT_TIMESTAMP WHERE id = :id";
        jdbc.update(sql, new MapSqlParameterSource("id", id));
    }

    public void unarchive(UUID id) {
        String sql = "UPDATE administration_obligations SET archived_at = NULL WHERE id = :id";
        jdbc.update(sql, new MapSqlParameterSource("id", id));
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM administration_obligations WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    public List<AdministrationObligation> findUpcomingDue(LocalDate fromDate, LocalDate toDate, int limit) {
        String sql = """
                SELECT * FROM administration_obligations
                WHERE archived_at IS NULL
                  AND status NOT IN ('CANCELLED', 'EXPIRED')
                  AND next_due_date >= :from
                  AND next_due_date <= :to
                ORDER BY next_due_date ASC
                LIMIT :limit
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("from", fromDate)
                .addValue("to", toDate)
                .addValue("limit", limit);
        return jdbc.query(sql, params, ObligationRowMapper.INSTANCE);
    }

    public int countActive() {
        String sql = "SELECT COUNT(*) FROM administration_obligations WHERE archived_at IS NULL AND status NOT IN ('CANCELLED', 'EXPIRED')";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource(), Integer.class);
        return count != null ? count : 0;
    }

    public int countDueSoon(LocalDate today, int daysThreshold) {
        String sql = """
                SELECT COUNT(*) FROM administration_obligations
                WHERE archived_at IS NULL
                  AND status NOT IN ('CANCELLED', 'EXPIRED')
                  AND next_due_date >= :today
                  AND next_due_date <= :maxDate
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("today", today)
                .addValue("maxDate", today.plusDays(daysThreshold));
        Integer count = jdbc.queryForObject(sql, params, Integer.class);
        return count != null ? count : 0;
    }

    public int countOverdue(LocalDate today) {
        String sql = """
                SELECT COUNT(*) FROM administration_obligations
                WHERE archived_at IS NULL
                  AND status NOT IN ('CANCELLED', 'EXPIRED', 'PAID')
                  AND next_due_date < :today
                """;
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("today", today), Integer.class);
        return count != null ? count : 0;
    }

    public BigDecimal sumActiveSubscriptionsMonthly() {
        String sql = """
                SELECT COALESCE(SUM(
                    CASE 
                        WHEN billing_frequency = 'WEEKLY' THEN expected_amount * 4.33
                        WHEN billing_frequency = 'MONTHLY' THEN expected_amount
                        WHEN billing_frequency = 'QUARTERLY' THEN expected_amount / 3.0
                        WHEN billing_frequency = 'HALF_YEARLY' THEN expected_amount / 6.0
                        WHEN billing_frequency = 'YEARLY' THEN expected_amount / 12.0
                        ELSE expected_amount
                    END
                ), 0)
                FROM administration_obligations
                WHERE archived_at IS NULL
                  AND status NOT IN ('CANCELLED', 'EXPIRED')
                  AND expected_amount IS NOT NULL
                """;
        BigDecimal sum = jdbc.queryForObject(sql, new MapSqlParameterSource(), BigDecimal.class);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    private MapSqlParameterSource toParams(AdministrationObligation o) {
        return new MapSqlParameterSource()
                .addValue("id", o.getId())
                .addValue("name", o.getName())
                .addValue("description", o.getDescription())
                .addValue("providerName", o.getProviderName())
                .addValue("category", o.getCategory())
                .addValue("obligationType", o.getObligationType())
                .addValue("expectedAmount", o.getExpectedAmount())
                .addValue("currency", o.getCurrency())
                .addValue("billingFrequency", o.getBillingFrequency())
                .addValue("recurrenceRule", o.getRecurrenceRule())
                .addValue("startDate", o.getStartDate())
                .addValue("nextDueDate", o.getNextDueDate())
                .addValue("endDate", o.getEndDate())
                .addValue("trialEndDate", o.getTrialEndDate())
                .addValue("cancellationDeadline", o.getCancellationDeadline())
                .addValue("autoRenew", o.isAutoRenew())
                .addValue("status", o.getStatus())
                .addValue("archivedAt", o.getArchivedAt() != null ? Timestamp.from(o.getArchivedAt()) : null);
    }

    static final class ObligationRowMapper implements RowMapper<AdministrationObligation> {
        static final ObligationRowMapper INSTANCE = new ObligationRowMapper();

        @Override
        public AdministrationObligation mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new AdministrationObligation(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getString("provider_name"),
                    rs.getString("category"),
                    rs.getString("obligation_type"),
                    rs.getBigDecimal("expected_amount"),
                    rs.getString("currency"),
                    rs.getString("billing_frequency"),
                    rs.getString("recurrence_rule"),
                    rs.getDate("start_date") != null ? rs.getDate("start_date").toLocalDate() : null,
                    rs.getDate("next_due_date") != null ? rs.getDate("next_due_date").toLocalDate() : null,
                    rs.getDate("end_date") != null ? rs.getDate("end_date").toLocalDate() : null,
                    rs.getDate("trial_end_date") != null ? rs.getDate("trial_end_date").toLocalDate() : null,
                    rs.getDate("cancellation_deadline") != null ? rs.getDate("cancellation_deadline").toLocalDate() : null,
                    rs.getBoolean("auto_renew"),
                    rs.getString("status"),
                    rs.getTimestamp("archived_at") != null ? rs.getTimestamp("archived_at").toInstant() : null,
                    rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : Instant.now(),
                    rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toInstant() : Instant.now()
            );
        }
    }
}
