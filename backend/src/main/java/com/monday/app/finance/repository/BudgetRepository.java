package com.monday.app.finance.repository;

import com.monday.app.finance.entity.Budget;
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
public class BudgetRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public BudgetRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Budget save(Budget budget) {
        UUID id = budget.getId() != null ? budget.getId() : UUID.randomUUID();
        String sql = """
                INSERT INTO budgets (id, name, category_id, period_start, period_end, amount)
                VALUES (:id, :name, :categoryId, :periodStart, :periodEnd, :amount)
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(id, budget), BudgetRowMapper.INSTANCE);
    }

    public Budget update(Budget budget) {
        String sql = """
                UPDATE budgets SET
                    name = :name, category_id = :categoryId,
                    period_start = :periodStart, period_end = :periodEnd, amount = :amount
                WHERE id = :id RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(budget.getId(), budget), BudgetRowMapper.INSTANCE);
    }

    public Optional<Budget> findById(UUID id) {
        return jdbc.query("SELECT * FROM budgets WHERE id = :id",
                new MapSqlParameterSource("id", id), BudgetRowMapper.INSTANCE).stream().findFirst();
    }

    public List<Budget> findAll() {
        return jdbc.query("SELECT * FROM budgets ORDER BY period_start DESC", BudgetRowMapper.INSTANCE);
    }

    public List<Budget> findByCategoryId(UUID categoryId) {
        return jdbc.query("SELECT * FROM budgets WHERE category_id = :categoryId ORDER BY period_start DESC",
                new MapSqlParameterSource("categoryId", categoryId), BudgetRowMapper.INSTANCE);
    }

    /**
     * Find budgets whose period includes the given date.
     */
    public List<Budget> findActiveForDate(LocalDate date) {
        String sql = """
                SELECT * FROM budgets
                WHERE period_start <= :date AND period_end >= :date
                ORDER BY period_start
                """;
        return jdbc.query(sql, new MapSqlParameterSource("date", Date.valueOf(date)), BudgetRowMapper.INSTANCE);
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM budgets WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource toParams(UUID id, Budget b) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("name", b.getName())
                .addValue("categoryId", b.getCategoryId())
                .addValue("periodStart", Date.valueOf(b.getPeriodStart()))
                .addValue("periodEnd", Date.valueOf(b.getPeriodEnd()))
                .addValue("amount", b.getAmount());
    }

    static final class BudgetRowMapper implements RowMapper<Budget> {
        static final BudgetRowMapper INSTANCE = new BudgetRowMapper();

        @Override
        public Budget mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Budget(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("name"),
                    UUID.fromString(rs.getString("category_id")),
                    rs.getDate("period_start").toLocalDate(),
                    rs.getDate("period_end").toLocalDate(),
                    rs.getBigDecimal("amount"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()
            );
        }
    }
}
