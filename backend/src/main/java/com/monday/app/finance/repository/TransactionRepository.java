package com.monday.app.finance.repository;

import com.monday.app.finance.entity.Transaction;
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
public class TransactionRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public TransactionRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Transaction save(Transaction txn) {
        UUID id = txn.getId() != null ? txn.getId() : UUID.randomUUID();
        String sql = """
                INSERT INTO transactions (id, transaction_type, amount, transaction_at,
                    category_id, payment_method, description, notes,
                    linked_module, linked_entity_id, linked_entity_type, account_id, credit_card_id)
                VALUES (:id, :transactionType, :amount, :transactionAt,
                    :categoryId, :paymentMethod, :description, :notes,
                    :linkedModule, :linkedEntityId, :linkedEntityType, :accountId, :creditCardId)
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(id, txn), TransactionRowMapper.INSTANCE);
    }

    public Transaction update(Transaction txn) {
        String sql = """
                UPDATE transactions SET
                    transaction_type = :transactionType, amount = :amount,
                    transaction_at = :transactionAt, category_id = :categoryId,
                    payment_method = :paymentMethod, description = :description, notes = :notes,
                    linked_module = :linkedModule, linked_entity_id = :linkedEntityId, linked_entity_type = :linkedEntityType,
                    account_id = :accountId, credit_card_id = :creditCardId
                WHERE id = :id RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(txn.getId(), txn), TransactionRowMapper.INSTANCE);
    }

    public Optional<Transaction> findById(UUID id) {
        String sql = """
                SELECT t.*, bc.name as category_name
                FROM transactions t
                LEFT JOIN budget_categories bc ON t.category_id = bc.id
                WHERE t.id = :id
                """;
        return jdbc.query(sql,
                new MapSqlParameterSource("id", id), TransactionRowMapper.INSTANCE).stream().findFirst();
    }

    public List<Transaction> findAll() {
        String sql = """
                SELECT t.*, bc.name as category_name
                FROM transactions t
                LEFT JOIN budget_categories bc ON t.category_id = bc.id
                ORDER BY t.transaction_at DESC
                """;
        return jdbc.query(sql, TransactionRowMapper.INSTANCE);
    }

    public List<Transaction> findByDateRange(Instant from, Instant to) {
        String sql = """
                SELECT t.*, bc.name as category_name
                FROM transactions t
                LEFT JOIN budget_categories bc ON t.category_id = bc.id
                WHERE t.transaction_at >= :from AND t.transaction_at <= :to
                ORDER BY t.transaction_at DESC
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("from", Timestamp.from(from))
                .addValue("to", Timestamp.from(to));
        return jdbc.query(sql, params, TransactionRowMapper.INSTANCE);
    }

    /**
     * Sum of expenses for a specific category within a date range (for safe-to-spend).
     */
    public BigDecimal sumExpensesByCategoryAndPeriod(UUID categoryId, LocalDate periodStart, LocalDate periodEnd) {
        String sql = """
                SELECT COALESCE(SUM(amount), 0) FROM transactions
                WHERE category_id = :categoryId
                  AND transaction_type = 'EXPENSE'
                  AND transaction_at >= :periodStart::date::timestamptz
                  AND transaction_at < (:periodEnd::date + INTERVAL '1 day')::timestamptz
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("categoryId", categoryId)
                .addValue("periodStart", java.sql.Date.valueOf(periodStart))
                .addValue("periodEnd", java.sql.Date.valueOf(periodEnd));
        return jdbc.queryForObject(sql, params, BigDecimal.class);
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM transactions WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource toParams(UUID id, Transaction t) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("transactionType", t.getTransactionType())
                .addValue("amount", t.getAmount())
                .addValue("transactionAt", Timestamp.from(t.getTransactionAt() != null ? t.getTransactionAt() : Instant.now()))
                .addValue("categoryId", t.getCategoryId())
                .addValue("paymentMethod", t.getPaymentMethod())
                .addValue("description", t.getDescription())
                .addValue("notes", t.getNotes())
                .addValue("linkedModule", t.getLinkedModule())
                .addValue("linkedEntityId", t.getLinkedEntityId())
                .addValue("linkedEntityType", t.getLinkedEntityType())
                .addValue("accountId", t.getAccountId())
                .addValue("creditCardId", t.getCreditCardId());
    }

    static final class TransactionRowMapper implements RowMapper<Transaction> {
        static final TransactionRowMapper INSTANCE = new TransactionRowMapper();

        @Override
        public Transaction mapRow(ResultSet rs, int rowNum) throws SQLException {
            String linkedModule = null;
            String linkedEntityId = null;
            String linkedEntityType = null;
            try {
                linkedModule = rs.getString("linked_module");
                linkedEntityId = rs.getString("linked_entity_id");
                linkedEntityType = rs.getString("linked_entity_type");
            } catch (SQLException ignored) {}

            UUID accountId = null;
            try {
                String accStr = rs.getString("account_id");
                if (accStr != null) accountId = UUID.fromString(accStr);
            } catch (SQLException ignored) {}

            UUID creditCardId = null;
            try {
                String ccStr = rs.getString("credit_card_id");
                if (ccStr != null) creditCardId = UUID.fromString(ccStr);
            } catch (SQLException ignored) {}

            Transaction t = new Transaction(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("transaction_type"),
                    rs.getBigDecimal("amount"),
                    rs.getTimestamp("transaction_at").toInstant(),
                    rs.getString("category_id") != null ? UUID.fromString(rs.getString("category_id")) : null,
                    rs.getString("payment_method"),
                    rs.getString("description"),
                    rs.getString("notes"),
                    linkedModule,
                    linkedEntityId,
                    linkedEntityType,
                    accountId,
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()
            );
            t.setCreditCardId(creditCardId);
            try {
                t.setCategoryName(rs.getString("category_name"));
            } catch (SQLException ignored) {}
            return t;
        }
    }
}
