package com.monday.app.administration.repository;

import com.monday.app.administration.entity.AdministrationChecklist;
import com.monday.app.administration.entity.AdministrationChecklistItem;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AdministrationChecklistRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AdministrationChecklistRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // --- Checklist Operations ---

    public AdministrationChecklist saveChecklist(AdministrationChecklist c) {
        UUID id = c.getId() != null ? c.getId() : UUID.randomUUID();
        c.setId(id);
        String sql = """
                INSERT INTO administration_checklists (
                    id, title, description, administration_record_type, administration_record_id, status
                ) VALUES (
                    :id, :title, :description, :recordType, :recordId, :status
                ) RETURNING *
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("title", c.getTitle())
                .addValue("description", c.getDescription())
                .addValue("recordType", c.getAdministrationRecordType())
                .addValue("recordId", c.getAdministrationRecordId())
                .addValue("status", c.getStatus() != null ? c.getStatus() : "ACTIVE");
        return jdbc.queryForObject(sql, params, ChecklistRowMapper.INSTANCE);
    }

    public AdministrationChecklist updateChecklist(AdministrationChecklist c) {
        String sql = """
                UPDATE administration_checklists SET
                    title = :title,
                    description = :description,
                    administration_record_type = :recordType,
                    administration_record_id = :recordId,
                    status = :status
                WHERE id = :id
                RETURNING *
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", c.getId())
                .addValue("title", c.getTitle())
                .addValue("description", c.getDescription())
                .addValue("recordType", c.getAdministrationRecordType())
                .addValue("recordId", c.getAdministrationRecordId())
                .addValue("status", c.getStatus());
        return jdbc.queryForObject(sql, params, ChecklistRowMapper.INSTANCE);
    }

    public Optional<AdministrationChecklist> findChecklistById(UUID id) {
        String sql = "SELECT * FROM administration_checklists WHERE id = :id";
        return jdbc.query(sql, new MapSqlParameterSource("id", id), ChecklistRowMapper.INSTANCE)
                .stream().findFirst();
    }

    public List<AdministrationChecklist> findAllChecklists() {
        String sql = "SELECT * FROM administration_checklists ORDER BY created_at DESC";
        return jdbc.query(sql, ChecklistRowMapper.INSTANCE);
    }

    public List<AdministrationChecklist> findChecklistsByTarget(String recordType, UUID recordId) {
        String sql = """
                SELECT * FROM administration_checklists
                WHERE administration_record_type = :recordType
                  AND administration_record_id = :recordId
                ORDER BY created_at DESC
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("recordType", recordType)
                .addValue("recordId", recordId);
        return jdbc.query(sql, params, ChecklistRowMapper.INSTANCE);
    }

    public List<AdministrationChecklist> findChecklistsByRecordType(String recordType) {
        String sql = """
                SELECT * FROM administration_checklists
                WHERE administration_record_type = :recordType
                ORDER BY created_at DESC
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("recordType", recordType);
        return jdbc.query(sql, params, ChecklistRowMapper.INSTANCE);
    }

    public void deleteChecklistById(UUID id) {
        jdbc.update("DELETE FROM administration_checklists WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    public int countActiveChecklists() {
        String sql = "SELECT COUNT(*) FROM administration_checklists WHERE status = 'ACTIVE'";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource(), Integer.class);
        return count != null ? count : 0;
    }

    public int countPendingItems() {
        String sql = "SELECT COUNT(*) FROM administration_checklist_items WHERE is_completed = false";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource(), Integer.class);
        return count != null ? count : 0;
    }

    // --- Checklist Items Operations ---

    public AdministrationChecklistItem saveItem(AdministrationChecklistItem item) {
        UUID id = item.getId() != null ? item.getId() : UUID.randomUUID();
        item.setId(id);
        String sql = """
                INSERT INTO administration_checklist_items (
                    id, checklist_id, title, description, position, is_completed, completed_at
                ) VALUES (
                    :id, :checklistId, :title, :description, :position, :completed, :completedAt
                ) RETURNING *
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("checklistId", item.getChecklistId())
                .addValue("title", item.getTitle())
                .addValue("description", item.getDescription())
                .addValue("position", item.getPosition())
                .addValue("completed", item.isCompleted())
                .addValue("completedAt", item.getCompletedAt() != null ? Timestamp.from(item.getCompletedAt()) : null);
        return jdbc.queryForObject(sql, params, ItemRowMapper.INSTANCE);
    }

    public AdministrationChecklistItem updateItem(AdministrationChecklistItem item) {
        String sql = """
                UPDATE administration_checklist_items SET
                    title = :title,
                    description = :description,
                    position = :position,
                    is_completed = :completed,
                    completed_at = :completedAt
                WHERE id = :id
                RETURNING *
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", item.getId())
                .addValue("title", item.getTitle())
                .addValue("description", item.getDescription())
                .addValue("position", item.getPosition())
                .addValue("completed", item.isCompleted())
                .addValue("completedAt", item.getCompletedAt() != null ? Timestamp.from(item.getCompletedAt()) : null);
        return jdbc.queryForObject(sql, params, ItemRowMapper.INSTANCE);
    }

    public Optional<AdministrationChecklistItem> findItemById(UUID itemId) {
        String sql = "SELECT * FROM administration_checklist_items WHERE id = :id";
        return jdbc.query(sql, new MapSqlParameterSource("id", itemId), ItemRowMapper.INSTANCE)
                .stream().findFirst();
    }

    public List<AdministrationChecklistItem> findItemsByChecklistId(UUID checklistId) {
        String sql = "SELECT * FROM administration_checklist_items WHERE checklist_id = :checklistId ORDER BY position ASC, created_at ASC";
        return jdbc.query(sql, new MapSqlParameterSource("checklistId", checklistId), ItemRowMapper.INSTANCE);
    }

    public void deleteItemById(UUID itemId) {
        jdbc.update("DELETE FROM administration_checklist_items WHERE id = :id", new MapSqlParameterSource("id", itemId));
    }

    public void setItemCompleted(UUID itemId, boolean completed) {
        String sql = """
                UPDATE administration_checklist_items SET
                    is_completed = :completed,
                    completed_at = :completedAt
                WHERE id = :id
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", itemId)
                .addValue("completed", completed)
                .addValue("completedAt", completed ? Timestamp.from(Instant.now()) : null);
        jdbc.update(sql, params);
    }

    public void reorderItems(UUID checklistId, List<UUID> itemIds) {
        String sql = "UPDATE administration_checklist_items SET position = :pos WHERE id = :id AND checklist_id = :checklistId";
        for (int i = 0; i < itemIds.size(); i++) {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("pos", i)
                    .addValue("id", itemIds.get(i))
                    .addValue("checklistId", checklistId);
            jdbc.update(sql, params);
        }
    }

    public int getNextPosition(UUID checklistId) {
        String sql = "SELECT COALESCE(MAX(position), -1) + 1 FROM administration_checklist_items WHERE checklist_id = :checklistId";
        Integer pos = jdbc.queryForObject(sql, new MapSqlParameterSource("checklistId", checklistId), Integer.class);
        return pos != null ? pos : 0;
    }

    static final class ChecklistRowMapper implements RowMapper<AdministrationChecklist> {
        static final ChecklistRowMapper INSTANCE = new ChecklistRowMapper();

        @Override
        public AdministrationChecklist mapRow(ResultSet rs, int rowNum) throws SQLException {
            String recordIdStr = rs.getString("administration_record_id");
            return new AdministrationChecklist(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getString("administration_record_type"),
                    recordIdStr != null ? UUID.fromString(recordIdStr) : null,
                    rs.getString("status"),
                    rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : Instant.now(),
                    rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toInstant() : Instant.now()
            );
        }
    }

    static final class ItemRowMapper implements RowMapper<AdministrationChecklistItem> {
        static final ItemRowMapper INSTANCE = new ItemRowMapper();

        @Override
        public AdministrationChecklistItem mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new AdministrationChecklistItem(
                    UUID.fromString(rs.getString("id")),
                    UUID.fromString(rs.getString("checklist_id")),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getInt("position"),
                    rs.getBoolean("is_completed"),
                    rs.getTimestamp("completed_at") != null ? rs.getTimestamp("completed_at").toInstant() : null,
                    rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : Instant.now(),
                    rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toInstant() : Instant.now()
            );
        }
    }
}
