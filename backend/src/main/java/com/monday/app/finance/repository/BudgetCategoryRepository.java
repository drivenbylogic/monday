package com.monday.app.finance.repository;

import com.monday.app.finance.entity.BudgetCategory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class BudgetCategoryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public BudgetCategoryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public BudgetCategory save(BudgetCategory category) {
        UUID id = category.getId() != null ? category.getId() : UUID.randomUUID();
        String sql = """
                INSERT INTO budget_categories (id, name, description, status)
                VALUES (:id, :name, :description, :status)
                RETURNING *
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("name", category.getName())
                .addValue("description", category.getDescription())
                .addValue("status", category.getStatus() != null ? category.getStatus() : "ACTIVE");
        return jdbc.queryForObject(sql, params, BudgetCategoryRowMapper.INSTANCE);
    }

    public BudgetCategory update(BudgetCategory category) {
        String sql = """
                UPDATE budget_categories SET name = :name, description = :description, status = :status
                WHERE id = :id RETURNING *
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", category.getId())
                .addValue("name", category.getName())
                .addValue("description", category.getDescription())
                .addValue("status", category.getStatus());
        return jdbc.queryForObject(sql, params, BudgetCategoryRowMapper.INSTANCE);
    }

    public Optional<BudgetCategory> findById(UUID id) {
        return jdbc.query("SELECT * FROM budget_categories WHERE id = :id",
                new MapSqlParameterSource("id", id), BudgetCategoryRowMapper.INSTANCE).stream().findFirst();
    }

    public List<BudgetCategory> findAll() {
        return jdbc.query("SELECT * FROM budget_categories ORDER BY name", BudgetCategoryRowMapper.INSTANCE);
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM budget_categories WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    static final class BudgetCategoryRowMapper implements RowMapper<BudgetCategory> {
        static final BudgetCategoryRowMapper INSTANCE = new BudgetCategoryRowMapper();

        @Override
        public BudgetCategory mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new BudgetCategory(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getString("status"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()
            );
        }
    }
}
