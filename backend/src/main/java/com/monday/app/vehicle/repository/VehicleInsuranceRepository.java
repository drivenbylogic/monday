package com.monday.app.vehicle.repository;

import com.monday.app.vehicle.entity.VehicleInsurance;
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
public class VehicleInsuranceRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public VehicleInsuranceRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public VehicleInsurance save(VehicleInsurance ins) {
        UUID id = ins.getId() != null ? ins.getId() : UUID.randomUUID();
        String sql = """
                INSERT INTO vehicle_insurance (
                    id, vehicle_id, document_id, policy_name, underwriter, policy_number,
                    coverage_scope, insured_declared_value, premium_due, due_date, status,
                    package_document_url, is_renewed
                )
                VALUES (
                    :id, :vehicleId, :documentId, :policyName, :underwriter, :policyNumber,
                    :coverageScope, :idv, :premiumDue, :dueDate, :status,
                    :packageDocumentUrl, :isRenewed
                )
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(id, ins), VehicleInsuranceRowMapper.INSTANCE);
    }

    public VehicleInsurance update(VehicleInsurance ins) {
        String sql = """
                UPDATE vehicle_insurance SET
                    document_id = :documentId, policy_name = :policyName, underwriter = :underwriter,
                    policy_number = :policyNumber, coverage_scope = :coverageScope,
                    insured_declared_value = :idv, premium_due = :premiumDue, due_date = :dueDate,
                    status = :status, package_document_url = :packageDocumentUrl, is_renewed = :isRenewed
                WHERE id = :id RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(ins.getId(), ins), VehicleInsuranceRowMapper.INSTANCE);
    }

    public Optional<VehicleInsurance> findById(UUID id) {
        return jdbc.query("SELECT * FROM vehicle_insurance WHERE id = :id",
                new MapSqlParameterSource("id", id), VehicleInsuranceRowMapper.INSTANCE).stream().findFirst();
    }

    public Optional<VehicleInsurance> findByDocumentId(UUID documentId) {
        return jdbc.query("SELECT * FROM vehicle_insurance WHERE document_id = :docId",
                new MapSqlParameterSource("docId", documentId), VehicleInsuranceRowMapper.INSTANCE).stream().findFirst();
    }

    public List<VehicleInsurance> findByVehicleId(UUID vehicleId) {
        return jdbc.query("SELECT * FROM vehicle_insurance WHERE vehicle_id = :vehicleId ORDER BY due_date DESC, created_at DESC",
                new MapSqlParameterSource("vehicleId", vehicleId), VehicleInsuranceRowMapper.INSTANCE);
    }

    public Optional<VehicleInsurance> findLatestActiveByVehicleId(UUID vehicleId) {
        return jdbc.query("SELECT * FROM vehicle_insurance WHERE vehicle_id = :vehicleId ORDER BY due_date DESC LIMIT 1",
                new MapSqlParameterSource("vehicleId", vehicleId), VehicleInsuranceRowMapper.INSTANCE).stream().findFirst();
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM vehicle_insurance WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource toParams(UUID id, VehicleInsurance ins) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("vehicleId", ins.getVehicleId())
                .addValue("documentId", ins.getDocumentId())
                .addValue("policyName", ins.getPolicyName())
                .addValue("underwriter", ins.getUnderwriter())
                .addValue("policyNumber", ins.getPolicyNumber())
                .addValue("coverageScope", ins.getCoverageScope())
                .addValue("idv", ins.getInsuredDeclaredValue())
                .addValue("premiumDue", ins.getPremiumDue())
                .addValue("dueDate", ins.getDueDate() != null ? Date.valueOf(ins.getDueDate()) : null)
                .addValue("status", ins.getStatus() != null ? ins.getStatus() : "ACTIVE")
                .addValue("packageDocumentUrl", ins.getPackageDocumentUrl())
                .addValue("isRenewed", Boolean.TRUE.equals(ins.getIsRenewed()));
    }

    static final class VehicleInsuranceRowMapper implements RowMapper<VehicleInsurance> {
        static final VehicleInsuranceRowMapper INSTANCE = new VehicleInsuranceRowMapper();

        @Override
        public VehicleInsurance mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new VehicleInsurance(
                    UUID.fromString(rs.getString("id")),
                    UUID.fromString(rs.getString("vehicle_id")),
                    rs.getString("document_id") != null ? UUID.fromString(rs.getString("document_id")) : null,
                    rs.getString("policy_name"),
                    rs.getString("underwriter"),
                    rs.getString("policy_number"),
                    rs.getString("coverage_scope"),
                    rs.getBigDecimal("insured_declared_value"),
                    rs.getBigDecimal("premium_due"),
                    rs.getDate("due_date") != null ? rs.getDate("due_date").toLocalDate() : null,
                    rs.getString("status"),
                    rs.getString("package_document_url"),
                    rs.getBoolean("is_renewed"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toInstant() : rs.getTimestamp("created_at").toInstant()
            );
        }
    }
}
