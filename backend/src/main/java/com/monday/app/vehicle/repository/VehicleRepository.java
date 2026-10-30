package com.monday.app.vehicle.repository;

import com.monday.app.vehicle.entity.Vehicle;
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
public class VehicleRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public VehicleRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Vehicle save(Vehicle vehicle) {
        UUID id = vehicle.getId() != null ? vehicle.getId() : UUID.randomUUID();
        String sql = """
                INSERT INTO vehicles (id, name, registration_number, purchase_date, purchase_amount, current_km, status,
                                      edition, variant, type, is_primary, hero_image_url, vin, emission_standard,
                                      registered_owner, registering_authority)
                VALUES (:id, :name, :registrationNumber, :purchaseDate, :purchaseAmount, :currentKm, :status,
                        :edition, :variant, :type, :isPrimary, :heroImageUrl, :vin, :emissionStandard,
                        :registeredOwner, :registeringAuthority)
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(id, vehicle), VehicleRowMapper.INSTANCE);
    }

    public Vehicle update(Vehicle vehicle) {
        String sql = """
                UPDATE vehicles SET
                    name = :name, registration_number = :registrationNumber,
                    purchase_date = :purchaseDate, purchase_amount = :purchaseAmount,
                    current_km = :currentKm, status = :status,
                    edition = :edition, variant = :variant, type = :type,
                    is_primary = :isPrimary, hero_image_url = :heroImageUrl,
                    vin = :vin, emission_standard = :emissionStandard,
                    registered_owner = :registeredOwner, registering_authority = :registeringAuthority
                WHERE id = :id RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(vehicle.getId(), vehicle), VehicleRowMapper.INSTANCE);
    }

    public Optional<Vehicle> findById(UUID id) {
        return jdbc.query("SELECT * FROM vehicles WHERE id = :id",
                new MapSqlParameterSource("id", id), VehicleRowMapper.INSTANCE).stream().findFirst();
    }

    public List<Vehicle> findAll() {
        return jdbc.query("SELECT * FROM vehicles ORDER BY is_primary DESC, created_at DESC", VehicleRowMapper.INSTANCE);
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM vehicles WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource toParams(UUID id, Vehicle v) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("name", v.getName())
                .addValue("registrationNumber", v.getRegistrationNumber())
                .addValue("purchaseDate", v.getPurchaseDate() != null ? Date.valueOf(v.getPurchaseDate()) : null)
                .addValue("purchaseAmount", v.getPurchaseAmount())
                .addValue("currentKm", v.getCurrentKm())
                .addValue("status", v.getStatus() != null ? v.getStatus() : "ACTIVE")
                .addValue("edition", v.getEdition())
                .addValue("variant", v.getVariant())
                .addValue("type", v.getType())
                .addValue("isPrimary", Boolean.TRUE.equals(v.getIsPrimary()))
                .addValue("heroImageUrl", v.getHeroImageUrl())
                .addValue("vin", v.getVin())
                .addValue("emissionStandard", v.getEmissionStandard())
                .addValue("registeredOwner", v.getRegisteredOwner())
                .addValue("registeringAuthority", v.getRegisteringAuthority());
    }

    static final class VehicleRowMapper implements RowMapper<Vehicle> {
        static final VehicleRowMapper INSTANCE = new VehicleRowMapper();

        @Override
        public Vehicle mapRow(ResultSet rs, int rowNum) throws SQLException {
            Vehicle v = new Vehicle(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("name"),
                    rs.getString("registration_number"),
                    rs.getDate("purchase_date") != null ? rs.getDate("purchase_date").toLocalDate() : null,
                    rs.getBigDecimal("purchase_amount"),
                    rs.getInt("current_km"),
                    rs.getString("status"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()
            );
            v.setEdition(rs.getString("edition"));
            v.setVariant(rs.getString("variant"));
            v.setType(rs.getString("type"));
            v.setIsPrimary(rs.getBoolean("is_primary"));
            v.setHeroImageUrl(rs.getString("hero_image_url"));
            v.setVin(rs.getString("vin"));
            v.setEmissionStandard(rs.getString("emission_standard"));
            v.setRegisteredOwner(rs.getString("registered_owner"));
            v.setRegisteringAuthority(rs.getString("registering_authority"));
            return v;
        }
    }
}
