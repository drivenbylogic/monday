package com.monday.app.project.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final MilestoneRepository milestoneRepository;
    private final TaskRepository taskRepository;

    public ProjectService(ProjectRepository projectRepository, MilestoneRepository milestoneRepository, TaskRepository taskRepository) {
        this.projectRepository = projectRepository;
        this.milestoneRepository = milestoneRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional
    public Project create(Project project) {
        if (project.getStatus() == null) {
            project.setStatus("PLANNING");
        }
        if (project.getCategory() == null) {
            project.setCategory("GENERAL");
        }
        if (project.getPriority() == null) {
            project.setPriority("MEDIUM");
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
        return projectRepository.findAll().stream().map(p -> {
            ProjectBasicResponse resp = new ProjectBasicResponse();
            resp.setId(p.getId());
            resp.setName(p.getName());
            resp.setDescription(p.getDescription());
            resp.setTags(p.getTags());
            
            List<Task> tasks = taskRepository.findByProjectId(p.getId());
            int total = tasks.size();
            long completed = tasks.stream().filter(t -> "COMPLETED".equals(t.getStatus())).count();
            resp.setPercentageToCompletion(total == 0 ? 0 : (int) ((completed * 100) / total));
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
}
