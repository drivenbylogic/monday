package com.monday.app.finance.repository;

import com.monday.app.finance.entity.FinanceAccount;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class FinanceAccountRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public FinanceAccountRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public FinanceAccount save(FinanceAccount account) {
        UUID id = account.getId() != null ? account.getId() : UUID.randomUUID();
        String sql = """
                INSERT INTO finance_accounts (id, account_name, institution, account_type, balance, currency, last_reconciled)
                VALUES (:id, :accountName, :institution, :accountType, :balance, :currency, :lastReconciled)
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(id, account), FinanceAccountRowMapper.INSTANCE);
    }

    public FinanceAccount update(FinanceAccount account) {
        String sql = """
                UPDATE finance_accounts SET
                    account_name = :accountName, institution = :institution,
                    account_type = :accountType, balance = :balance,
                    currency = :currency, last_reconciled = :lastReconciled
                WHERE id = :id RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(account.getId(), account), FinanceAccountRowMapper.INSTANCE);
    }

    public Optional<FinanceAccount> findById(UUID id) {
        return jdbc.query("SELECT * FROM finance_accounts WHERE id = :id",
                new MapSqlParameterSource("id", id), FinanceAccountRowMapper.INSTANCE)
                .stream().findFirst();
    }

    public List<FinanceAccount> findAll() {
        return jdbc.query("SELECT * FROM finance_accounts ORDER BY created_at ASC",
                FinanceAccountRowMapper.INSTANCE);
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM finance_accounts WHERE id = :id",
                new MapSqlParameterSource("id", id));
    }

    public void adjustBalance(UUID id, java.math.BigDecimal delta) {
        jdbc.update("UPDATE finance_accounts SET balance = balance + :delta WHERE id = :id",
                new MapSqlParameterSource("delta", delta).addValue("id", id));
    }

    private MapSqlParameterSource toParams(UUID id, FinanceAccount a) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("accountName", a.getAccountName())
                .addValue("institution", a.getInstitution())
                .addValue("accountType", a.getAccountType())
                .addValue("balance", a.getBalance())
                .addValue("currency", a.getCurrency() != null ? a.getCurrency() : "INR")
                .addValue("lastReconciled", a.getLastReconciled() != null ? Timestamp.from(a.getLastReconciled()) : null);
    }

    static final class FinanceAccountRowMapper implements RowMapper<FinanceAccount> {
        static final FinanceAccountRowMapper INSTANCE = new FinanceAccountRowMapper();

        @Override
        public FinanceAccount mapRow(ResultSet rs, int rowNum) throws SQLException {
            FinanceAccount a = new FinanceAccount();
            a.setId(UUID.fromString(rs.getString("id")));
            try { a.setAccountName(rs.getString("account_name")); } catch (SQLException ignored) {}
            a.setInstitution(rs.getString("institution"));
            a.setAccountType(rs.getString("account_type"));
            a.setBalance(rs.getBigDecimal("balance"));
            a.setCurrency(rs.getString("currency"));
            Timestamp lr = rs.getTimestamp("last_reconciled");
            if (lr != null) a.setLastReconciled(lr.toInstant());
            a.setCreatedAt(rs.getTimestamp("created_at").toInstant());
            a.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
            return a;
        }
    }
}
