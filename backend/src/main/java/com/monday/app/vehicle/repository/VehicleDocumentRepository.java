package com.monday.app.vehicle.repository;

import com.monday.app.vehicle.entity.VehicleDocument;
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
public class VehicleDocumentRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public VehicleDocumentRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public VehicleDocument save(VehicleDocument doc) {
        UUID id = doc.getId() != null ? doc.getId() : UUID.randomUUID();
        String sql = """
                INSERT INTO vehicle_documents (
                    id, vehicle_id, doc_type, title, identifier, issuer, valid_until,
                    issue_date, valid_from, expiry_date, verification_source, is_verified,
                    status, notes, metadata
                )
                VALUES (
                    :id, :vehicleId, :docType, :title, :identifier, :issuer, :validUntil,
                    :issueDate, :validFrom, :expiryDate, :verificationSource, :isVerified,
                    :status, :notes, :metadata::jsonb
                )
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(id, doc), VehicleDocumentRowMapper.INSTANCE);
    }

    public VehicleDocument update(VehicleDocument doc) {
        String sql = """
                UPDATE vehicle_documents SET
                    doc_type = :docType, title = :title, identifier = :identifier, issuer = :issuer,
                    valid_until = :validUntil, issue_date = :issueDate, valid_from = :validFrom,
                    expiry_date = :expiryDate, verification_source = :verificationSource,
                    is_verified = :isVerified, status = :status, notes = :notes,
                    metadata = :metadata::jsonb
                WHERE id = :id RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(doc.getId(), doc), VehicleDocumentRowMapper.INSTANCE);
    }

    public Optional<VehicleDocument> findById(UUID id) {
        return jdbc.query("SELECT * FROM vehicle_documents WHERE id = :id",
                new MapSqlParameterSource("id", id), VehicleDocumentRowMapper.INSTANCE).stream().findFirst();
    }

    public List<VehicleDocument> findByVehicleId(UUID vehicleId) {
        return jdbc.query("SELECT * FROM vehicle_documents WHERE vehicle_id = :vehicleId ORDER BY expiry_date ASC NULLS LAST, created_at DESC",
                new MapSqlParameterSource("vehicleId", vehicleId), VehicleDocumentRowMapper.INSTANCE);
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM vehicle_documents WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource toParams(UUID id, VehicleDocument doc) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("vehicleId", doc.getVehicleId())
                .addValue("docType", doc.getDocType() != null ? doc.getDocType() : "CUSTOM")
                .addValue("title", doc.getTitle())
                .addValue("identifier", doc.getIdentifier())
                .addValue("issuer", doc.getIssuer())
                .addValue("validUntil", doc.getValidUntil())
                .addValue("issueDate", doc.getIssueDate() != null ? Date.valueOf(doc.getIssueDate()) : null)
                .addValue("validFrom", doc.getValidFrom() != null ? Date.valueOf(doc.getValidFrom()) : null)
                .addValue("expiryDate", doc.getExpiryDate() != null ? Date.valueOf(doc.getExpiryDate()) : null)
                .addValue("verificationSource", doc.getVerificationSource())
                .addValue("isVerified", Boolean.TRUE.equals(doc.getIsVerified()))
                .addValue("status", doc.getStatus() != null ? doc.getStatus() : "VALID")
                .addValue("notes", doc.getNotes())
                .addValue("metadata", doc.getMetadata());
    }

    static final class VehicleDocumentRowMapper implements RowMapper<VehicleDocument> {
        static final VehicleDocumentRowMapper INSTANCE = new VehicleDocumentRowMapper();

        @Override
        public VehicleDocument mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new VehicleDocument(
                    UUID.fromString(rs.getString("id")),
                    UUID.fromString(rs.getString("vehicle_id")),
                    rs.getString("doc_type"),
                    rs.getString("title"),
                    rs.getString("identifier"),
                    rs.getString("issuer"),
                    rs.getString("valid_until"),
                    rs.getDate("issue_date") != null ? rs.getDate("issue_date").toLocalDate() : null,
                    rs.getDate("valid_from") != null ? rs.getDate("valid_from").toLocalDate() : null,
                    rs.getDate("expiry_date") != null ? rs.getDate("expiry_date").toLocalDate() : null,
                    rs.getString("verification_source"),
                    rs.getBoolean("is_verified"),
                    rs.getString("status"),
                    rs.getString("covered_components"),
                    rs.getString("notes"),
                    rs.getString("metadata"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toInstant() : rs.getTimestamp("created_at").toInstant()
            );
        }
    }
}
