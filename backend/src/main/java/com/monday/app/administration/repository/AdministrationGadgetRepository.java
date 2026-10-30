package com.monday.app.administration.repository;

import com.monday.app.administration.entity.AdministrationGadget;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AdministrationGadgetRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AdministrationGadgetRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public AdministrationGadget save(AdministrationGadget g) {
        UUID id = g.getId() != null ? g.getId() : UUID.randomUUID();
        g.setId(id);
        String sql = """
                INSERT INTO administration_gadgets (
                    id, name, category, brand, model, serial_number, purchase_date,
                    purchase_price, warranty_start_date, warranty_end_date,
                    warranty_provider, notes, archived_at
                ) VALUES (
                    :id, :name, :category, :brand, :model, :serialNumber, :purchaseDate,
                    :purchasePrice, :warrantyStartDate, :warrantyEndDate,
                    :warrantyProvider, :notes, :archivedAt
                ) RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(g), GadgetRowMapper.INSTANCE);
    }

    public AdministrationGadget update(AdministrationGadget g) {
        String sql = """
                UPDATE administration_gadgets SET
                    name = :name,
                    category = :category,
                    brand = :brand,
                    model = :model,
                    serial_number = :serialNumber,
                    purchase_date = :purchaseDate,
                    purchase_price = :purchasePrice,
                    warranty_start_date = :warrantyStartDate,
                    warranty_end_date = :warrantyEndDate,
                    warranty_provider = :warrantyProvider,
                    notes = :notes,
                    archived_at = :archivedAt
                WHERE id = :id
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(g), GadgetRowMapper.INSTANCE);
    }

    public Optional<AdministrationGadget> findById(UUID id) {
        String sql = "SELECT * FROM administration_gadgets WHERE id = :id";
        return jdbc.query(sql, new MapSqlParameterSource("id", id), GadgetRowMapper.INSTANCE)
                .stream().findFirst();
    }

    public List<AdministrationGadget> findAll(boolean includeArchived) {
        String sql = includeArchived
                ? "SELECT * FROM administration_gadgets ORDER BY name ASC"
                : "SELECT * FROM administration_gadgets WHERE archived_at IS NULL ORDER BY name ASC";
        return jdbc.query(sql, GadgetRowMapper.INSTANCE);
    }

    public List<AdministrationGadget> search(String query, String category, Boolean archived) {
        StringBuilder sql = new StringBuilder("SELECT * FROM administration_gadgets WHERE 1=1");
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

        if (query != null && !query.isBlank()) {
            sql.append(" AND (LOWER(name) LIKE :q OR LOWER(brand) LIKE :q OR LOWER(model) LIKE :q OR LOWER(serial_number) LIKE :q)");
            params.addValue("q", "%" + query.toLowerCase().trim() + "%");
        }

        sql.append(" ORDER BY name ASC");
        return jdbc.query(sql.toString(), params, GadgetRowMapper.INSTANCE);
    }

    public void archive(UUID id) {
        jdbc.update("UPDATE administration_gadgets SET archived_at = CURRENT_TIMESTAMP WHERE id = :id",
                new MapSqlParameterSource("id", id));
    }

    public void unarchive(UUID id) {
        jdbc.update("UPDATE administration_gadgets SET archived_at = NULL WHERE id = :id",
                new MapSqlParameterSource("id", id));
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM administration_gadgets WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    public int countAll() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM administration_gadgets WHERE archived_at IS NULL",
                new MapSqlParameterSource(), Integer.class);
        return count != null ? count : 0;
    }

    public int countMaintenance(UUID gadgetId) {
        String sql = "SELECT COUNT(*) FROM administration_maintenance WHERE gadget_id = :gadgetId";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("gadgetId", gadgetId), Integer.class);
        return count != null ? count : 0;
    }

    public int countWarrantiesExpiringSoon(LocalDate today, int daysThreshold) {
        String sql = """
                SELECT COUNT(*) FROM administration_gadgets
                WHERE archived_at IS NULL
                  AND warranty_end_date IS NOT NULL
                  AND warranty_end_date >= :today
                  AND warranty_end_date <= :maxDate
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("today", today)
                .addValue("maxDate", today.plusDays(daysThreshold));
        Integer count = jdbc.queryForObject(sql, params, Integer.class);
        return count != null ? count : 0;
    }

    private MapSqlParameterSource toParams(AdministrationGadget g) {
        return new MapSqlParameterSource()
                .addValue("id", g.getId())
                .addValue("name", g.getName())
                .addValue("category", g.getCategory())
                .addValue("brand", g.getBrand())
                .addValue("model", g.getModel())
                .addValue("serialNumber", g.getSerialNumber())
                .addValue("purchaseDate", g.getPurchaseDate())
                .addValue("purchasePrice", g.getPurchasePrice())
                .addValue("warrantyStartDate", g.getWarrantyStartDate())
                .addValue("warrantyEndDate", g.getWarrantyEndDate())
                .addValue("warrantyProvider", g.getWarrantyProvider())
                .addValue("notes", g.getNotes())
                .addValue("archivedAt", g.getArchivedAt() != null ? Timestamp.from(g.getArchivedAt()) : null);
    }

    static final class GadgetRowMapper implements RowMapper<AdministrationGadget> {
        static final GadgetRowMapper INSTANCE = new GadgetRowMapper();

        @Override
        public AdministrationGadget mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new AdministrationGadget(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getString("brand"),
                    rs.getString("model"),
                    rs.getString("serial_number"),
                    rs.getDate("purchase_date") != null ? rs.getDate("purchase_date").toLocalDate() : null,
                    rs.getBigDecimal("purchase_price"),
                    rs.getDate("warranty_start_date") != null ? rs.getDate("warranty_start_date").toLocalDate() : null,
                    rs.getDate("warranty_end_date") != null ? rs.getDate("warranty_end_date").toLocalDate() : null,
                    rs.getString("warranty_provider"),
                    rs.getString("notes"),
                    rs.getTimestamp("archived_at") != null ? rs.getTimestamp("archived_at").toInstant() : null,
                    rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : Instant.now(),
                    rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toInstant() : Instant.now()
            );
        }
    }
}
