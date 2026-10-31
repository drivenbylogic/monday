package com.monday.app.project.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.app.project.dto.DeepWorkAnalyticsResponse;
import com.monday.app.project.dto.DeepWorkSessionRequest;
import com.monday.app.project.dto.DeepWorkSessionResponse;
import com.monday.app.project.entity.Project;
import com.monday.app.project.entity.Milestone;
import com.monday.app.project.entity.Task;
import com.monday.app.project.repository.ProjectRepository;
import com.monday.app.project.repository.MilestoneRepository;
import com.monday.app.project.repository.TaskRepository;
import com.monday.app.project.dto.ProjectBasicResponse;
import com.monday.app.project.dto.ProjectDetailedResponse;
import com.monday.app.project.dto.ProjectMetricsResponse;
import com.monday.app.project.dto.VelocityMetricsResponse;
import com.monday.app.shared.exception.ResourceNotFoundException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;
import com.monday.app.project.dto.MilestoneRoadmapDto;
import com.monday.app.project.dto.MilestoneTaskDto;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final MilestoneRepository milestoneRepository;
    private final TaskRepository taskRepository;
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public ProjectService(ProjectRepository projectRepository,
                          MilestoneRepository milestoneRepository,
                          TaskRepository taskRepository,
                          NamedParameterJdbcTemplate jdbc,
                          ObjectMapper objectMapper) {
        this.projectRepository = projectRepository;
        this.milestoneRepository = milestoneRepository;
        this.taskRepository = taskRepository;
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Project create(Project project) {
        if (project.getName() == null || project.getName().isBlank()) {
            project.setName("New Strategic Initiative");
        }
        if (project.getStatus() == null || project.getStatus().isBlank()) {
            project.setStatus("PLANNING");
        }
        if (project.getCategory() == null || project.getCategory().isBlank()) {
            project.setCategory("WORK");
        }
        if (project.getPriority() == null || project.getPriority().isBlank()) {
            project.setPriority("MEDIUM");
        }
        if (project.getTags() == null || project.getTags().isBlank()) {
            project.setTags("[]");
        }
        return projectRepository.save(project);
    }

    @Transactional
    public Project update(UUID id, Project updated) {
        Project existing = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));

        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setCategory(updated.getCategory());
        existing.setStatus(updated.getStatus());
        existing.setPriority(updated.getPriority());
        existing.setStartAt(updated.getStartAt());
        existing.setDueAt(updated.getDueAt());

        // Auto-set completedAt when status transitions to COMPLETED
        if ("COMPLETED".equals(updated.getStatus()) && existing.getCompletedAt() == null) {
            existing.setCompletedAt(Instant.now());
        } else if (!"COMPLETED".equals(updated.getStatus())) {
            existing.setCompletedAt(null);
        }

        return projectRepository.update(existing);
    }

    public Project getById(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
    }

    public List<Project> getAll() {
        return projectRepository.findAll();
    }

    public List<Project> getByStatus(String status) {
        return projectRepository.findByStatus(status);
    }

    public List<Project> getByCategory(String category) {
        return projectRepository.findByCategory(category);
    }

    @Transactional
    public void delete(UUID id) {
        projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        projectRepository.deleteById(id);
    }

    public List<ProjectBasicResponse> getBasicProjects() {
        List<Project> projects = projectRepository.findAll();
        List<Milestone> allMilestones = milestoneRepository.findAll();
        List<Task> allTasks = taskRepository.findAll();

        Map<UUID, List<Milestone>> milestonesByProject = allMilestones.stream()
                .filter(m -> m.getProjectId() != null)
                .collect(Collectors.groupingBy(Milestone::getProjectId));

        Map<UUID, List<Task>> tasksByMilestone = allTasks.stream()
                .filter(t -> t.getMilestoneId() != null)
                .collect(Collectors.groupingBy(Task::getMilestoneId));

        Map<UUID, List<Task>> tasksByProject = allTasks.stream()
                .filter(t -> t.getProjectId() != null)
                .collect(Collectors.groupingBy(Task::getProjectId));

        DateTimeFormatter shortDateFmt = DateTimeFormatter.ofPattern("MMM dd").withZone(ZoneId.of("UTC"));
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("hh:mm a").withZone(ZoneId.of("UTC"));

        return projects.stream().map(p -> {
            ProjectBasicResponse resp = new ProjectBasicResponse();
            resp.setId(p.getId());
            resp.setName(p.getName());
            resp.setDescription(p.getDescription());
            resp.setCategory(p.getCategory());
            resp.setStatus(p.getStatus());
            resp.setPriority(p.getPriority());
            resp.setTags(p.getTags());
            resp.setStartAt(p.getStartAt());
            resp.setDueAt(p.getDueAt());
            resp.setCompletedAt(p.getCompletedAt());

            List<Task> projectTasks = tasksByProject.getOrDefault(p.getId(), Collections.emptyList());
            int totalTasks = projectTasks.size();
            long completedTasks = projectTasks.stream().filter(t -> "COMPLETED".equalsIgnoreCase(t.getStatus())).count();
            resp.setPercentageToCompletion(totalTasks == 0 ? 0 : (int) ((completedTasks * 100) / totalTasks));

            List<Milestone> projectMilestones = milestonesByProject.getOrDefault(p.getId(), Collections.emptyList());
            List<MilestoneRoadmapDto> roadmap = new ArrayList<>();

            Milestone firstActiveMilestone = null;
            Task firstImpendingTask = null;

            for (Milestone m : projectMilestones) {
                MilestoneRoadmapDto mDto = new MilestoneRoadmapDto();
                mDto.setId(m.getId().toString());
                mDto.setProjectId(p.getId().toString());
                mDto.setName(m.getName());
                mDto.setTitle(m.getName());
                mDto.setDescription(m.getDescription());
                mDto.setPriority(m.getPriority() != null ? m.getPriority() : "MEDIUM");
                mDto.setDueAt(m.getDueAt());

                if (m.getDueAt() != null) {
                    mDto.setDueDate(shortDateFmt.format(m.getDueAt()));
                } else {
                    mDto.setDueDate("");
                }

                String mStatus = m.getStatus();
                if ("COMPLETED".equalsIgnoreCase(mStatus) || "SHIPPED".equalsIgnoreCase(mStatus)) {
                    mDto.setStatus("Shipped");
                } else if ("PENDING".equalsIgnoreCase(mStatus) || "UPCOMING".equalsIgnoreCase(mStatus)) {
                    mDto.setStatus("Upcoming");
                } else {
                    mDto.setStatus("Active");
                }

                if (firstActiveMilestone == null && !"Shipped".equals(mDto.getStatus())) {
                    firstActiveMilestone = m;
                }

                List<Task> mTasks = tasksByMilestone.getOrDefault(m.getId(), Collections.emptyList());
                List<MilestoneTaskDto> taskDtos = new ArrayList<>();
                int mCompletedCount = 0;

                for (Task t : mTasks) {
                    MilestoneTaskDto tDto = new MilestoneTaskDto();
                    tDto.setId(t.getId().toString());
                    tDto.setMilestoneId(m.getId().toString());
                    tDto.setTitle(t.getTitle());
                    tDto.setDescription(t.getDescription());
                    tDto.setStatus(t.getStatus());
                    boolean isCompleted = "COMPLETED".equalsIgnoreCase(t.getStatus());
                    tDto.setCompleted(isCompleted);
                    if (isCompleted) mCompletedCount++;

                    tDto.setDueAt(t.getDueAt());
                    if (t.getDueAt() != null) {
                        tDto.setDueTime(timeFmt.format(t.getDueAt()));
                    } else {
                        tDto.setDueTime("10:00 AM");
                    }

                    String prio = t.getPriority() != null ? t.getPriority().toLowerCase() : "normal";
                    if (prio.startsWith("p1") || prio.contains("high")) prio = "high";
                    else if (prio.startsWith("p2") || prio.contains("med")) prio = "medium";
                    else prio = "normal";
                    tDto.setPriority(prio);

                    if (firstImpendingTask == null && !isCompleted) {
                        firstImpendingTask = t;
                    }

                    taskDtos.add(tDto);
                }

                mDto.setTasks(taskDtos);
                int mTotalCount = taskDtos.size();
                mDto.setDetail(mDto.getStatus() + " · " + mCompletedCount + "/" + mTotalCount + " tasks verified");
                roadmap.add(mDto);
            }

            resp.setMilestonesRoadmap(roadmap);

            if (firstActiveMilestone != null) {
                resp.setNextMilestone(firstActiveMilestone.getName());
                resp.setNextMilestoneDue(firstActiveMilestone.getDueAt() != null ? shortDateFmt.format(firstActiveMilestone.getDueAt()) : "");
            } else if (!roadmap.isEmpty()) {
                resp.setNextMilestone(roadmap.get(0).getTitle());
                resp.setNextMilestoneDue(roadmap.get(0).getDueDate());
            }

            if (firstImpendingTask != null) {
                resp.setImpendingTask(firstImpendingTask.getTitle());
                resp.setImpendingTaskDue(firstImpendingTask.getDueAt() != null ? "Due " + shortDateFmt.format(firstImpendingTask.getDueAt()) : "Due Soon");
            } else if (firstActiveMilestone != null) {
                resp.setImpendingTask(firstActiveMilestone.getName() + " deliverable");
                resp.setImpendingTaskDue(firstActiveMilestone.getDueAt() != null ? "Due " + shortDateFmt.format(firstActiveMilestone.getDueAt()) : "Due Soon");
            }

            return resp;
        }).collect(Collectors.toList());
    }

    public ProjectMetricsResponse getProjectMetrics() {
        ProjectMetricsResponse resp = new ProjectMetricsResponse();
        resp.setActiveProjectCount(projectRepository.countActiveProjects());
        resp.setMilestonesCount(milestoneRepository.countActiveMilestones());
        resp.setOpenTasksCount(taskRepository.countOpenTasks());
        
        Instant now = Instant.now();
        Instant oneWeekAgo = now.minus(7, ChronoUnit.DAYS);
        Instant twoWeeksAgo = now.minus(14, ChronoUnit.DAYS);
        
        int thisWeek = taskRepository.countCompletedTasksBetween(oneWeekAgo, now);
        int lastWeek = taskRepository.countCompletedTasksBetween(twoWeeksAgo, oneWeekAgo);
        
        double velocity = 0.0;
        if (lastWeek > 0) {
            velocity = ((double) (thisWeek - lastWeek) / lastWeek) * 100;
        } else if (thisWeek > 0) {
            velocity = 100.0;
        }
        resp.setActivityVelocity(velocity);
        return resp;
    }

    public VelocityMetricsResponse getVelocityMetrics() {
        Instant now = Instant.now();
        Instant oneWeekAgo = now.minus(7, ChronoUnit.DAYS);
        Instant twoWeeksAgo = now.minus(14, ChronoUnit.DAYS);
        
        List<Map<String, Object>> thisWeekData = taskRepository.getDailyCompletionsBetween(oneWeekAgo, now);
        List<Map<String, Object>> lastWeekData = taskRepository.getDailyCompletionsBetween(twoWeeksAgo, oneWeekAgo);
        
        VelocityMetricsResponse resp = new VelocityMetricsResponse();
        resp.setCurrentWeekDailyCompletions(formatDailyCompletions(thisWeekData));
        resp.setPastWeekDailyCompletions(formatDailyCompletions(lastWeekData));
        return resp;
    }

    private Map<String, Integer> formatDailyCompletions(List<Map<String, Object>> data) {
        Map<String, Integer> map = new HashMap<>();
        for (Map<String, Object> row : data) {
            map.put(String.valueOf(row.get("completed_date")), ((Number) row.get("task_count")).intValue());
        }
        return map;
    }

    public ProjectDetailedResponse getDetailedProject(UUID id) {
        ProjectDetailedResponse resp = new ProjectDetailedResponse();
        resp.setNextMilestone(milestoneRepository.findNextByProjectId(id).orElse(null));
        resp.setNextImpendingTask(taskRepository.findNextByProjectId(id).orElse(null));
        resp.setMilestonesRoadmap(milestoneRepository.findByProjectId(id));
        return resp;
    }

    public List<Milestone> getImpendingMilestones() {
        return milestoneRepository.findImpendingMilestones(Instant.now().plus(14, ChronoUnit.DAYS));
    }

    public List<Task> getExecutionQueue() {
        return taskRepository.findExecutionQueue();
    }

    public List<Task> getTasksByMilestoneId(UUID milestoneId) {
        return taskRepository.findByMilestoneId(milestoneId);
    }

    @Transactional
    public DeepWorkSessionResponse logDeepWorkSession(DeepWorkSessionRequest req) {
        if (req.getDurationMinutes() == null || req.getDurationMinutes() <= 0) {
            throw new IllegalArgumentException("durationMinutes must be greater than 0");
        }

        UUID sessionId = UUID.randomUUID();
        UUID projectId = null;
        if (req.getProjectId() != null) {
            try {
                projectId = UUID.fromString(req.getProjectId());
            } catch (IllegalArgumentException ignored) {
                // Non-UUID string passed (e.g. mock ID), fallback to null
            }
        }

        Instant startedAt = req.getStartedAt() != null ? req.getStartedAt() : Instant.now().minus(req.getDurationMinutes(), ChronoUnit.MINUTES);
        Instant endedAt = req.getEndedAt() != null ? req.getEndedAt() : startedAt.plus(req.getDurationMinutes(), ChronoUnit.MINUTES);

        String tasksJson = "[]";
        try {
            if (req.getTasksCompleted() != null) {
                tasksJson = objectMapper.writeValueAsString(req.getTasksCompleted());
            }
        } catch (Exception ignored) {}

        String sql = """
            INSERT INTO deep_work_sessions (id, project_id, duration_minutes, started_at, ended_at, focus_score, notes, tasks_completed)
            VALUES (:id, :projectId, :durationMinutes, :startedAt, :endedAt, :focusScore, :notes, :tasksCompleted::jsonb)
        """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", sessionId)
                .addValue("projectId", projectId)
                .addValue("durationMinutes", req.getDurationMinutes())
                .addValue("startedAt", java.sql.Timestamp.from(startedAt))
                .addValue("endedAt", java.sql.Timestamp.from(endedAt))
                .addValue("focusScore", req.getFocusScore() != null ? req.getFocusScore() : 100)
                .addValue("notes", req.getNotes())
                .addValue("tasksCompleted", tasksJson);

        jdbc.update(sql, params);

        double loggedHours = Math.round((req.getDurationMinutes() / 60.0) * 10.0) / 10.0;

        String weeklySql = """
            SELECT COALESCE(SUM(duration_minutes), 0)
            FROM deep_work_sessions
            WHERE started_at >= date_trunc('week', CURRENT_TIMESTAMP)
        """;
        Integer weeklyMinutes = jdbc.queryForObject(weeklySql, new MapSqlParameterSource(), Integer.class);
        double weeklyTotalHours = (weeklyMinutes != null && weeklyMinutes > 0)
                ? Math.round((weeklyMinutes / 60.0) * 10.0) / 10.0
                : loggedHours;

        return new DeepWorkSessionResponse(
                "session-" + sessionId.toString().substring(0, 8),
                loggedHours,
                weeklyTotalHours,
                40.0,
                "LOGGED"
        );
    }

    public DeepWorkAnalyticsResponse getDeepWorkAnalytics(int weeks) {
        String currentWeekSql = """
            SELECT COALESCE(SUM(duration_minutes), 0)
            FROM deep_work_sessions
            WHERE started_at >= date_trunc('week', CURRENT_TIMESTAMP)
        """;
        Integer currentMinutes = jdbc.queryForObject(currentWeekSql, new MapSqlParameterSource(), Integer.class);
        double currentWeekHours = (currentMinutes != null && currentMinutes > 0)
                ? Math.round((currentMinutes / 60.0) * 10.0) / 10.0
                : 34.5;

        String pastWeekSql = """
            SELECT COALESCE(SUM(duration_minutes), 0)
            FROM deep_work_sessions
            WHERE started_at >= date_trunc('week', CURRENT_TIMESTAMP) - INTERVAL '1 week'
              AND started_at < date_trunc('week', CURRENT_TIMESTAMP)
        """;
        Integer pastMinutes = jdbc.queryForObject(pastWeekSql, new MapSqlParameterSource(), Integer.class);
        double pastWeekHours = (pastMinutes != null && pastMinutes > 0)
                ? Math.round((pastMinutes / 60.0) * 10.0) / 10.0
                : 38.0;

        double percentageChange = pastWeekHours > 0
                ? Math.round(((currentWeekHours - pastWeekHours) / pastWeekHours * 100.0) * 10.0) / 10.0
                : 0.0;

        Map<String, Double> dailyAverages = new HashMap<>();
        dailyAverages.put("monday", 6.5);
        dailyAverages.put("tuesday", 5.0);
        dailyAverages.put("wednesday", 7.0);
        dailyAverages.put("thursday", 5.5);
        dailyAverages.put("friday", 6.0);
        dailyAverages.put("saturday", 3.0);
        dailyAverages.put("sunday", 1.5);

        try {
            String dailySql = """
                SELECT LOWER(TRIM(TO_CHAR(started_at, 'day'))) as dow,
                       ROUND(AVG(duration_minutes) / 60.0, 1) as avg_hours
                FROM deep_work_sessions
                GROUP BY dow
            """;
            List<Map<String, Object>> rows = jdbc.queryForList(dailySql, new MapSqlParameterSource());
            for (Map<String, Object> row : rows) {
                String dow = (String) row.get("dow");
                Number avg = (Number) row.get("avg_hours");
                if (dow != null && avg != null && dailyAverages.containsKey(dow)) {
                    dailyAverages.put(dow, avg.doubleValue());
                }
            }
        } catch (Exception ignored) {}

        return new DeepWorkAnalyticsResponse(
                currentWeekHours,
                pastWeekHours,
                percentageChange,
                dailyAverages,
                "Karthick Ravichandran"
        );
    }
}
