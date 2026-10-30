package com.monday.app.finance.repository;

import com.monday.app.finance.entity.CreditAccount;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CreditAccountRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public CreditAccountRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public CreditAccount save(CreditAccount card) {
        UUID id = card.getId() != null ? card.getId() : UUID.randomUUID();
        String cardId = card.getCardId() != null ? card.getCardId() : id.toString().substring(0, 8).toUpperCase();
        String sql = """
                INSERT INTO credit_accounts (id, card_id, card_name, last_four, network, credit_limit,
                    current_balance, statement_closing_date, payment_due_date, billing_date, due_date,
                    min_amount_due, auto_pay_enabled, notes)
                VALUES (:id, :cardId, :cardName, :lastFour, :network, :creditLimit,
                    :currentBalance, :statementClosingDate, :paymentDueDate, :billingDate, :dueDate,
                    :minAmountDue, :autoPayEnabled, :notes)
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(id, cardId, card), CreditAccountRowMapper.INSTANCE);
    }

    public CreditAccount update(CreditAccount card) {
        String sql = """
                UPDATE credit_accounts SET
                    card_name = :cardName, last_four = :lastFour, network = :network,
                    credit_limit = :creditLimit, current_balance = :currentBalance,
                    statement_closing_date = :statementClosingDate, payment_due_date = :paymentDueDate,
                    billing_date = :billingDate, due_date = :dueDate,
                    min_amount_due = :minAmountDue, auto_pay_enabled = :autoPayEnabled, notes = :notes
                WHERE id = :id RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(card.getId(), card.getCardId(), card), CreditAccountRowMapper.INSTANCE);
    }

    public Optional<CreditAccount> findById(UUID id) {
        return jdbc.query("SELECT * FROM credit_accounts WHERE id = :id",
                new MapSqlParameterSource("id", id), CreditAccountRowMapper.INSTANCE)
                .stream().findFirst();
    }

    public List<CreditAccount> findAll() {
        return jdbc.query("SELECT * FROM credit_accounts ORDER BY credit_limit DESC",
                CreditAccountRowMapper.INSTANCE);
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM credit_accounts WHERE id = :id",
                new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource toParams(UUID id, String cardId, CreditAccount c) {
        Date statementClosingDate = c.getStatementClosingDate() != null ? Date.valueOf(c.getStatementClosingDate()) : null;
        Date paymentDueDate = c.getPaymentDueDate() != null ? Date.valueOf(c.getPaymentDueDate()) : null;
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("cardId", cardId)
                .addValue("cardName", c.getCardName())
                .addValue("lastFour", c.getLastFour())
                .addValue("network", c.getNetwork() != null ? c.getNetwork() : "VISA")
                .addValue("creditLimit", c.getCreditLimit())
                .addValue("currentBalance", c.getCurrentBalance() != null ? c.getCurrentBalance() : java.math.BigDecimal.ZERO)
                .addValue("statementClosingDate", statementClosingDate)
                .addValue("paymentDueDate", paymentDueDate)
                .addValue("billingDate", statementClosingDate)
                .addValue("dueDate", paymentDueDate)
                .addValue("minAmountDue", c.getMinAmountDue() != null ? c.getMinAmountDue() : java.math.BigDecimal.ZERO)
                .addValue("autoPayEnabled", c.isAutoPayEnabled())
                .addValue("notes", c.getNotes());
    }

    static final class CreditAccountRowMapper implements RowMapper<CreditAccount> {
        static final CreditAccountRowMapper INSTANCE = new CreditAccountRowMapper();

        @Override
        public CreditAccount mapRow(ResultSet rs, int rowNum) throws SQLException {
            CreditAccount c = new CreditAccount();
            c.setId(UUID.fromString(rs.getString("id")));
            c.setCardId(rs.getString("card_id"));
            c.setCardName(rs.getString("card_name"));
            c.setLastFour(rs.getString("last_four"));
            c.setNetwork(rs.getString("network"));
            c.setCreditLimit(rs.getBigDecimal("credit_limit"));
            c.setCurrentBalance(rs.getBigDecimal("current_balance"));
            Date scd = null;
            try { scd = rs.getDate("billing_date"); } catch (SQLException ignored) {}
            if (scd == null) scd = rs.getDate("statement_closing_date");
            if (scd != null) {
                c.setStatementClosingDate(scd.toLocalDate());
                c.setBillingDate(scd.toLocalDate());
            }

            Date pdd = null;
            try { pdd = rs.getDate("due_date"); } catch (SQLException ignored) {}
            if (pdd == null) pdd = rs.getDate("payment_due_date");
            if (pdd != null) {
                c.setPaymentDueDate(pdd.toLocalDate());
                c.setDueDate(pdd.toLocalDate());
            }

            c.setMinAmountDue(rs.getBigDecimal("min_amount_due"));
            c.setAutoPayEnabled(rs.getBoolean("auto_pay_enabled"));
            try { c.setNotes(rs.getString("notes")); } catch (SQLException ignored) {}
            if (rs.getTimestamp("created_at") != null)
                c.setCreatedAt(rs.getTimestamp("created_at").toInstant());
            if (rs.getTimestamp("updated_at") != null)
                c.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
            return c;
        }
    }
}
