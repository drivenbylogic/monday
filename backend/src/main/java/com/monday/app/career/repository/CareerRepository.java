package com.monday.app.career.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.monday.app.career.entity.CareerPlan;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CareerRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public CareerRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    private static final RowMapper<CareerPlan> ROW_MAPPER = new RowMapper<>() {
        @Override
        public CareerPlan mapRow(ResultSet rs, int rowNum) throws SQLException {
            CareerPlan plan = new CareerPlan();
            plan.setId(rs.getString("id"));
            plan.setName(rs.getString("name"));
            plan.setDescription(rs.getString("description"));
            
            java.sql.Date start = rs.getDate("start_date");
            if (start != null) {
                plan.setStartDate(start.toLocalDate());
            }
            java.sql.Date target = rs.getDate("target_date");
            if (target != null) {
                plan.setTargetDate(target.toLocalDate());
            }
            plan.setColor(rs.getString("color"));
            plan.setViewStyle(rs.getString("view_style"));
            plan.setPlanData(rs.getString("plan_data"));
            
            Timestamp created = rs.getTimestamp("created_at");
            if (created != null) {
                plan.setCreatedAt(created.toInstant());
            }
            Timestamp updated = rs.getTimestamp("updated_at");
            if (updated != null) {
                plan.setUpdatedAt(updated.toInstant());
            }
            return plan;
        }
    };

    @Transactional
    public void save(CareerPlan plan) {
        String sql = """
            INSERT INTO career_plans (id, name, description, start_date, target_date, color, view_style, plan_data, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                description = EXCLUDED.description,
                start_date = EXCLUDED.start_date,
                target_date = EXCLUDED.target_date,
                color = EXCLUDED.color,
                view_style = EXCLUDED.view_style,
                plan_data = EXCLUDED.plan_data,
                updated_at = CURRENT_TIMESTAMP
            """;
        
        jdbcTemplate.update(sql,
                plan.getId(),
                plan.getName(),
                plan.getDescription(),
                plan.getStartDate() != null ? java.sql.Date.valueOf(plan.getStartDate()) : null,
                plan.getTargetDate() != null ? java.sql.Date.valueOf(plan.getTargetDate()) : null,
                plan.getColor(),
                plan.getViewStyle(),
                plan.getPlanData()
        );
    }

    public Optional<CareerPlan> findCurrentPlan() {
        String sql = "SELECT * FROM career_plans ORDER BY updated_at DESC LIMIT 1";
        try {
            CareerPlan plan = jdbcTemplate.queryForObject(sql, ROW_MAPPER);
            return Optional.ofNullable(plan);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<CareerPlan> findById(String id) {
        String sql = "SELECT * FROM career_plans WHERE id = ?";
        try {
            CareerPlan plan = jdbcTemplate.queryForObject(sql, ROW_MAPPER, id);
            return Optional.ofNullable(plan);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Transactional
    public void deleteById(String id) {
        jdbcTemplate.update("DELETE FROM career_plans WHERE id = ?", id);
    }

    @Transactional
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM career_plans");
    }

    @Transactional
    public void deleteCoreCareerData() {
        // Delete any tasks linked to career projects
        jdbcTemplate.update("DELETE FROM tasks WHERE project_id IN (SELECT id FROM projects WHERE category = 'CAREER')");
        // Delete any milestones linked to career projects
        jdbcTemplate.update("DELETE FROM milestones WHERE project_id IN (SELECT id FROM projects WHERE category = 'CAREER')");
        // Delete career projects
        jdbcTemplate.update("DELETE FROM projects WHERE category = 'CAREER'");
    }

    /**
     * Projects and synchronizes the career plan JSON into the core relational tables
     * (projects, milestones, tasks) for unified querying across the Monday system.
     */
    @Transactional
    public void syncToCoreTables(CareerPlan plan, JsonNode rootNode) {
        // First clean previous career project data from core tables
        deleteCoreCareerData();

        String planId = plan.getId();
        UUID projectUuid = UUID.nameUUIDFromBytes(("career:project:" + planId).getBytes(StandardCharsets.UTF_8));

        Timestamp startAt = plan.getStartDate() != null 
                ? Timestamp.from(plan.getStartDate().atStartOfDay().toInstant(ZoneOffset.UTC)) 
                : null;
        Timestamp dueAt = plan.getTargetDate() != null 
                ? Timestamp.from(plan.getTargetDate().atTime(23, 59, 59).toInstant(ZoneOffset.UTC)) 
                : null;

        String insertProjectSql = """
            INSERT INTO projects (id, name, description, category, status, priority, tags, start_at, due_at, created_at, updated_at)
            VALUES (?, ?, ?, 'CAREER', 'ACTIVE', 'HIGH', '["career", "transition", "engineering"]'::jsonb, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                description = EXCLUDED.description,
                category = 'CAREER',
                status = 'ACTIVE',
                priority = 'HIGH',
                start_at = EXCLUDED.start_at,
                due_at = EXCLUDED.due_at,
                updated_at = CURRENT_TIMESTAMP
            """;

        jdbcTemplate.update(insertProjectSql, projectUuid, plan.getName(), plan.getDescription(), startAt, dueAt);

        JsonNode milestonesNode = rootNode.path("milestones");
        if (milestonesNode.isArray()) {
            for (JsonNode mNode : milestonesNode) {
                String mId = mNode.path("id").asText();
                UUID milestoneUuid = UUID.nameUUIDFromBytes(("career:milestone:" + mId).getBytes(StandardCharsets.UTF_8));
                String mName = mNode.path("name").asText("Unnamed Milestone");
                String mDesc = mNode.hasNonNull("description") ? mNode.path("description").asText() : null;
                String mDueDateStr = mNode.path("dueDate").asText(null);
                Timestamp mDueAt = parseDateToTimestamp(mDueDateStr);
                String mPriority = mNode.path("priority").asText("p1").toUpperCase();

                String insertMilestoneSql = """
                    INSERT INTO milestones (id, project_id, name, description, status, priority, due_at, created_at, updated_at)
                    VALUES (?, ?, ?, ?, 'PENDING', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    ON CONFLICT (id) DO UPDATE SET
                        name = EXCLUDED.name,
                        description = EXCLUDED.description,
                        due_at = EXCLUDED.due_at,
                        updated_at = CURRENT_TIMESTAMP
                    """;
                jdbcTemplate.update(insertMilestoneSql, milestoneUuid, projectUuid, mName, mDesc, mPriority, mDueAt);

                JsonNode tasksNode = mNode.path("tasks");
                if (tasksNode.isArray()) {
                    for (JsonNode tNode : tasksNode) {
                        String tId = tNode.path("id").asText();
                        UUID taskUuid = UUID.nameUUIDFromBytes(("career:task:" + tId).getBytes(StandardCharsets.UTF_8));
                        String tTitle = tNode.path("title").asText("Unnamed Task");
                        String tDesc = tNode.hasNonNull("description") ? tNode.path("description").asText() : null;
                        boolean tCompleted = tNode.path("isCompleted").asBoolean(false);
                        String tDueDateStr = tNode.path("dueDate").asText(null);
                        Timestamp tDueAt = parseDateToTimestamp(tDueDateStr);
                        String tPriority = tNode.path("priority").asText("p1").toUpperCase();
                        boolean isOngoing = mNode.path("isOngoing").asBoolean(false);

                        String insertTaskSql = """
                            INSERT INTO tasks (id, title, description, project_id, milestone_id, parent_task_id, status, priority, due_at, is_recurring, created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?, NULL, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                            ON CONFLICT (id) DO UPDATE SET
                                title = EXCLUDED.title,
                                description = EXCLUDED.description,
                                status = EXCLUDED.status,
                                priority = EXCLUDED.priority,
                                due_at = EXCLUDED.due_at,
                                is_recurring = EXCLUDED.is_recurring,
                                updated_at = CURRENT_TIMESTAMP
                            """;
                        jdbcTemplate.update(insertTaskSql, taskUuid, tTitle, tDesc, projectUuid, milestoneUuid,
                                tCompleted ? "COMPLETED" : "PENDING", tPriority, tDueAt, isOngoing);

                        JsonNode subtasksNode = tNode.path("subtasks");
                        if (subtasksNode.isArray()) {
                            for (JsonNode sNode : subtasksNode) {
                                String sId = sNode.path("id").asText();
                                UUID subtaskUuid = UUID.nameUUIDFromBytes(("career:task:" + sId).getBytes(StandardCharsets.UTF_8));
                                String sTitle = sNode.path("title").asText("Unnamed Subtask");
                                String sDesc = sNode.hasNonNull("description") ? sNode.path("description").asText() : null;
                                boolean sCompleted = sNode.path("isCompleted").asBoolean(false);
                                String sDueDateStr = sNode.path("dueDate").asText(null);
                                Timestamp sDueAt = parseDateToTimestamp(sDueDateStr);
                                String sPriority = sNode.path("priority").asText("p2").toUpperCase();

                                String insertSubtaskSql = """
                                    INSERT INTO tasks (id, title, description, project_id, milestone_id, parent_task_id, status, priority, due_at, is_recurring, created_at, updated_at)
                                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                                    ON CONFLICT (id) DO UPDATE SET
                                        title = EXCLUDED.title,
                                        description = EXCLUDED.description,
                                        status = EXCLUDED.status,
                                        priority = EXCLUDED.priority,
                                        due_at = EXCLUDED.due_at,
                                        updated_at = CURRENT_TIMESTAMP
                                    """;
                                jdbcTemplate.update(insertSubtaskSql, subtaskUuid, sTitle, sDesc, projectUuid, milestoneUuid,
                                        taskUuid, sCompleted ? "COMPLETED" : "PENDING", sPriority, sDueAt);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Updates the completion status of a task or subtask in both the JSON plan data
     * and the core tasks table.
     */
    @Transactional
    public Optional<CareerPlan> updateTaskCompletion(String taskId, Boolean forcedCompleted) {
        Optional<CareerPlan> planOpt = findCurrentPlan();
        if (planOpt.isEmpty()) {
            return Optional.empty();
        }

        CareerPlan plan = planOpt.get();
        try {
            JsonNode rootNode = objectMapper.readTree(plan.getPlanData());
            boolean targetStatus = false;
            boolean found = false;

            JsonNode milestonesNode = rootNode.path("milestones");
            if (milestonesNode.isArray()) {
                for (JsonNode mNode : milestonesNode) {
                    JsonNode tasksNode = mNode.path("tasks");
                    if (tasksNode.isArray()) {
                        for (JsonNode tNode : tasksNode) {
                            if (taskId.equals(tNode.path("id").asText())) {
                                boolean current = tNode.path("isCompleted").asBoolean(false);
                                targetStatus = forcedCompleted != null ? forcedCompleted : !current;
                                ((ObjectNode) tNode).put("isCompleted", targetStatus);
                                found = true;
                                break;
                            }
                            JsonNode subtasksNode = tNode.path("subtasks");
                            if (subtasksNode.isArray()) {
                                for (JsonNode sNode : subtasksNode) {
                                    if (taskId.equals(sNode.path("id").asText())) {
                                        boolean current = sNode.path("isCompleted").asBoolean(false);
                                        targetStatus = forcedCompleted != null ? forcedCompleted : !current;
                                        ((ObjectNode) sNode).put("isCompleted", targetStatus);
                                        found = true;
                                        break;
                                    }
                                }
                            }
                            if (found) break;
                        }
                    }
                    if (found) break;
                }
            }

            if (found) {
                plan.setPlanData(objectMapper.writeValueAsString(rootNode));
                save(plan);

                // Update core tasks table
                UUID taskUuid = UUID.nameUUIDFromBytes(("career:task:" + taskId).getBytes(StandardCharsets.UTF_8));
                Timestamp completedAt = targetStatus ? Timestamp.from(Instant.now()) : null;
                String statusStr = targetStatus ? "COMPLETED" : "PENDING";
                jdbcTemplate.update("UPDATE tasks SET status = ?, completed_at = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                        statusStr, completedAt, taskUuid);
            }

            return Optional.of(plan);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update task completion status: " + e.getMessage(), e);
        }
    }

    private Timestamp parseDateToTimestamp(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            LocalDate ld = LocalDate.parse(dateStr.trim());
            return Timestamp.from(ld.atTime(23, 59, 59).toInstant(ZoneOffset.UTC));
        } catch (Exception e) {
            return null;
        }
    }
}
