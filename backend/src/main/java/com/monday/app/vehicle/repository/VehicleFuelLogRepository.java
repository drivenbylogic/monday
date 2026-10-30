package com.monday.app.vehicle.repository;

import com.monday.app.vehicle.entity.VehicleFuelLog;
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
public class VehicleFuelLogRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public VehicleFuelLogRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public VehicleFuelLog save(VehicleFuelLog log) {
        UUID id = log.getId() != null ? log.getId() : UUID.randomUUID();
        String sql = """
                INSERT INTO vehicle_fuel_logs (
                    id, vehicle_id, log_date, brand, fuel_brand, location, station_location,
                    fuel_grade, full_tank, volume_liters, total_cost, price_per_liter,
                    odometer_km, trip_efficiency
                )
                VALUES (
                    :id, :vehicleId, :logDate, :brand, :fuelBrand, :location, :stationLocation,
                    :fuelGrade, :fullTank, :volumeLiters, :totalCost, :pricePerLiter,
                    :odometerKm, :tripEfficiency
                )
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(id, log), VehicleFuelLogRowMapper.INSTANCE);
    }

    public Optional<VehicleFuelLog> findById(UUID id) {
        return jdbc.query("SELECT * FROM vehicle_fuel_logs WHERE id = :id",
                new MapSqlParameterSource("id", id), VehicleFuelLogRowMapper.INSTANCE).stream().findFirst();
    }

    public List<VehicleFuelLog> findByVehicleId(UUID vehicleId) {
        return jdbc.query("SELECT * FROM vehicle_fuel_logs WHERE vehicle_id = :vehicleId ORDER BY log_date DESC, created_at DESC",
                new MapSqlParameterSource("vehicleId", vehicleId), VehicleFuelLogRowMapper.INSTANCE);
    }

    public List<VehicleFuelLog> findByVehicleIdAndDateRange(UUID vehicleId, Date startDate, Date endDate) {
        String sql = "SELECT * FROM vehicle_fuel_logs WHERE vehicle_id = :vehicleId AND log_date BETWEEN :start AND :end ORDER BY log_date ASC, odometer_km ASC";
        return jdbc.query(sql, new MapSqlParameterSource("vehicleId", vehicleId).addValue("start", startDate).addValue("end", endDate), VehicleFuelLogRowMapper.INSTANCE);
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM vehicle_fuel_logs WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource toParams(UUID id, VehicleFuelLog log) {
        String brand = log.getFuelBrand() != null ? log.getFuelBrand() : log.getBrand();
        String loc = log.getStationLocation() != null ? log.getStationLocation() : log.getLocation();
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("vehicleId", log.getVehicleId())
                .addValue("logDate", Date.valueOf(log.getLogDate()))
                .addValue("brand", brand)
                .addValue("fuelBrand", brand)
                .addValue("location", loc)
                .addValue("stationLocation", loc)
                .addValue("fuelGrade", log.getFuelGrade())
                .addValue("fullTank", Boolean.TRUE.equals(log.getFullTank()))
                .addValue("volumeLiters", log.getVolumeLiters())
                .addValue("totalCost", log.getTotalCost())
                .addValue("pricePerLiter", log.getPricePerLiter())
                .addValue("odometerKm", log.getOdometerKm())
                .addValue("tripEfficiency", log.getTripEfficiency());
    }

    static final class VehicleFuelLogRowMapper implements RowMapper<VehicleFuelLog> {
        static final VehicleFuelLogRowMapper INSTANCE = new VehicleFuelLogRowMapper();

        @Override
        public VehicleFuelLog mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new VehicleFuelLog(
                    UUID.fromString(rs.getString("id")),
                    UUID.fromString(rs.getString("vehicle_id")),
                    rs.getDate("log_date").toLocalDate(),
                    rs.getString("brand"),
                    rs.getString("fuel_brand"),
                    rs.getString("location"),
                    rs.getString("station_location"),
                    rs.getString("fuel_grade"),
                    rs.getBoolean("full_tank"),
                    rs.getBigDecimal("volume_liters"),
                    rs.getBigDecimal("total_cost"),
                    rs.getBigDecimal("price_per_liter"),
                    rs.getObject("odometer_km") != null ? rs.getInt("odometer_km") : null,
                    rs.getBigDecimal("trip_efficiency"),
                    rs.getTimestamp("created_at").toInstant()
            );
        }
    }
}
