package com.monday.app.vehicle.repository;

import com.monday.app.vehicle.entity.VehicleOdometerReading;
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
public class VehicleOdometerReadingRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public VehicleOdometerReadingRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public VehicleOdometerReading save(VehicleOdometerReading reading) {
        UUID id = reading.getId() != null ? reading.getId() : UUID.randomUUID();
        String sql = """
                INSERT INTO vehicle_odometer_readings (
                    id, vehicle_id, reading_date, odometer_km, source, is_correction, notes
                )
                VALUES (
                    :id, :vehicleId, :readingDate, :odometerKm, :source, :isCorrection, :notes
                )
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(id, reading), VehicleOdometerReadingRowMapper.INSTANCE);
    }

    public Optional<VehicleOdometerReading> findById(UUID id) {
        return jdbc.query("SELECT * FROM vehicle_odometer_readings WHERE id = :id",
                new MapSqlParameterSource("id", id), VehicleOdometerReadingRowMapper.INSTANCE).stream().findFirst();
    }

    public List<VehicleOdometerReading> findByVehicleId(UUID vehicleId) {
        return jdbc.query("SELECT * FROM vehicle_odometer_readings WHERE vehicle_id = :vehicleId ORDER BY reading_date DESC, odometer_km DESC, created_at DESC",
                new MapSqlParameterSource("vehicleId", vehicleId), VehicleOdometerReadingRowMapper.INSTANCE);
    }

    public Optional<VehicleOdometerReading> findLatestByVehicleId(UUID vehicleId) {
        String sql = "SELECT * FROM vehicle_odometer_readings WHERE vehicle_id = :vehicleId ORDER BY reading_date DESC, odometer_km DESC, created_at DESC LIMIT 1";
        return jdbc.query(sql, new MapSqlParameterSource("vehicleId", vehicleId), VehicleOdometerReadingRowMapper.INSTANCE).stream().findFirst();
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM vehicle_odometer_readings WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource toParams(UUID id, VehicleOdometerReading r) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("vehicleId", r.getVehicleId())
                .addValue("readingDate", Date.valueOf(r.getReadingDate()))
                .addValue("odometerKm", r.getOdometerKm())
                .addValue("source", r.getSource() != null ? r.getSource() : "MANUAL")
                .addValue("isCorrection", Boolean.TRUE.equals(r.getIsCorrection()))
                .addValue("notes", r.getNotes());
    }

    static final class VehicleOdometerReadingRowMapper implements RowMapper<VehicleOdometerReading> {
        static final VehicleOdometerReadingRowMapper INSTANCE = new VehicleOdometerReadingRowMapper();

        @Override
        public VehicleOdometerReading mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new VehicleOdometerReading(
                    UUID.fromString(rs.getString("id")),
                    UUID.fromString(rs.getString("vehicle_id")),
                    rs.getDate("reading_date").toLocalDate(),
                    rs.getInt("odometer_km"),
                    rs.getString("source"),
                    rs.getBoolean("is_correction"),
                    rs.getString("notes"),
                    rs.getTimestamp("created_at").toInstant()
            );
        }
    }
}
