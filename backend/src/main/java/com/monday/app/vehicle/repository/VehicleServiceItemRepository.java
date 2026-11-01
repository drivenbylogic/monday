package com.monday.app.vehicle.repository;

import com.monday.app.vehicle.entity.VehicleServiceItem;
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
public class VehicleServiceItemRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public VehicleServiceItemRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public VehicleServiceItem save(VehicleServiceItem item) {
        UUID id = item.getId() != null ? item.getId() : UUID.randomUUID();
        String sql = """
                INSERT INTO vehicle_service_items (
                    id, service_record_id, item_name, category, brand_spec, quantity,
                    unit, item_cost, replacement_interval_months, replacement_interval_km, notes
                )
                VALUES (
                    :id, :serviceRecordId, :itemName, :category, :brandSpec, :quantity,
                    :unit, :itemCost, :months, :km, :notes
                )
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(id, item), VehicleServiceItemRowMapper.INSTANCE);
    }

    public List<VehicleServiceItem> findByServiceRecordId(UUID serviceRecordId) {
        return jdbc.query("SELECT * FROM vehicle_service_items WHERE service_record_id = :serviceRecordId ORDER BY created_at ASC",
                new MapSqlParameterSource("serviceRecordId", serviceRecordId), VehicleServiceItemRowMapper.INSTANCE);
    }

    public List<VehicleServiceItem> findByVehicleId(UUID vehicleId) {
        String sql = """
                SELECT i.* FROM vehicle_service_items i
                JOIN vehicle_service_records r ON i.service_record_id = r.id
                WHERE r.vehicle_id = :vehicleId
                ORDER BY r.service_date DESC, i.created_at ASC
                """;
        return jdbc.query(sql, new MapSqlParameterSource("vehicleId", vehicleId), VehicleServiceItemRowMapper.INSTANCE);
    }

    public void deleteByServiceRecordId(UUID serviceRecordId) {
        jdbc.update("DELETE FROM vehicle_service_items WHERE service_record_id = :id", new MapSqlParameterSource("id", serviceRecordId));
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM vehicle_service_items WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource toParams(UUID id, VehicleServiceItem item) {
        String itemName = item.getItemName() != null && !item.getItemName().isBlank() ? item.getItemName().trim() : "Consumable / Part";
        java.math.BigDecimal qty = item.getQuantity() != null && item.getQuantity().compareTo(java.math.BigDecimal.ZERO) > 0 ? item.getQuantity() : java.math.BigDecimal.ONE;
        java.math.BigDecimal cost = item.getItemCost() != null && item.getItemCost().compareTo(java.math.BigDecimal.ZERO) >= 0 ? item.getItemCost() : java.math.BigDecimal.ZERO;
        Integer months = item.getReplacementIntervalMonths() != null && item.getReplacementIntervalMonths() > 0 ? item.getReplacementIntervalMonths() : null;
        Integer km = item.getReplacementIntervalKm() != null && item.getReplacementIntervalKm() > 0 ? item.getReplacementIntervalKm() : null;

        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("serviceRecordId", item.getServiceRecordId())
                .addValue("itemName", itemName)
                .addValue("category", item.getCategory() != null && !item.getCategory().isBlank() ? item.getCategory().trim() : "PART")
                .addValue("brandSpec", item.getBrandSpec())
                .addValue("quantity", qty)
                .addValue("unit", item.getUnit() != null ? item.getUnit() : "pcs")
                .addValue("itemCost", cost)
                .addValue("months", months)
                .addValue("km", km)
                .addValue("notes", item.getNotes());
    }

    static final class VehicleServiceItemRowMapper implements RowMapper<VehicleServiceItem> {
        static final VehicleServiceItemRowMapper INSTANCE = new VehicleServiceItemRowMapper();

        @Override
        public VehicleServiceItem mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new VehicleServiceItem(
                    UUID.fromString(rs.getString("id")),
                    UUID.fromString(rs.getString("service_record_id")),
                    rs.getString("item_name") != null ? rs.getString("item_name") : "Consumable / Part",
                    rs.getString("category") != null ? rs.getString("category") : "PART",
                    rs.getString("brand_spec"),
                    rs.getBigDecimal("quantity") != null ? rs.getBigDecimal("quantity") : java.math.BigDecimal.ONE,
                    rs.getString("unit"),
                    rs.getBigDecimal("item_cost") != null ? rs.getBigDecimal("item_cost") : java.math.BigDecimal.ZERO,
                    rs.getObject("replacement_interval_months") != null ? rs.getInt("replacement_interval_months") : null,
                    rs.getObject("replacement_interval_km") != null ? rs.getInt("replacement_interval_km") : null,
                    rs.getString("notes"),
                    rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : java.time.Instant.now()
            );
        }
    }
}
