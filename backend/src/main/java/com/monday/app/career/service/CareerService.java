package com.monday.app.career.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.app.career.entity.CareerPlan;
import com.monday.app.career.repository.CareerRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class CareerService {

    private final CareerRepository careerRepository;
    private final ObjectMapper objectMapper;

    public CareerService(CareerRepository careerRepository, ObjectMapper objectMapper) {
        this.careerRepository = careerRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CareerPlan importPlan(JsonNode rootNode) {
        if (rootNode == null || rootNode.isNull()) {
            throw new IllegalArgumentException("Import JSON cannot be empty");
        }

        String id = rootNode.path("id").asText("career-plan");
        String name = rootNode.path("name").asText("Career Transition Plan");
        String description = rootNode.path("description").asText("");

        LocalDate startDate = null;
        if (rootNode.hasNonNull("startDate")) {
            try {
                startDate = LocalDate.parse(rootNode.path("startDate").asText());
            } catch (Exception ignored) {}
        }

        LocalDate targetDate = null;
        if (rootNode.hasNonNull("targetDate")) {
            try {
                targetDate = LocalDate.parse(rootNode.path("targetDate").asText());
            } catch (Exception ignored) {}
        }

        String color = rootNode.path("color").asText("violet");
        String viewStyle = rootNode.path("viewStyle").asText("board");

        String planDataJson;
        try {
            planDataJson = objectMapper.writeValueAsString(rootNode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize plan JSON: " + e.getMessage(), e);
        }

        CareerPlan plan = new CareerPlan();
        plan.setId(id);
        plan.setName(name);
        plan.setDescription(description);
        plan.setStartDate(startDate);
        plan.setTargetDate(targetDate);
        plan.setColor(color);
        plan.setViewStyle(viewStyle);
        plan.setPlanData(planDataJson);

        careerRepository.save(plan);
        careerRepository.syncToCoreTables(plan, rootNode);

        return plan;
    }

    public Optional<CareerPlan> getCurrentPlan() {
        return careerRepository.findCurrentPlan();
    }

    public Optional<CareerPlan> getPlanById(String id) {
        return careerRepository.findById(id);
    }

    @Transactional
    public CareerPlan toggleTask(String taskId, Boolean completed) {
        return careerRepository.updateTaskCompletion(taskId, completed)
                .orElseThrow(() -> new IllegalArgumentException("Task not found or no active career plan: " + taskId));
    }

    @Transactional
    public void clearCareerData() {
        careerRepository.deleteAll();
        careerRepository.deleteCoreCareerData();
    }

    public JsonNode loadSamplePlan() {
        try {
            // Try classpath first
            ClassPathResource resource = new ClassPathResource("data/level_up_project.min.json");
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    return objectMapper.readTree(is);
                }
            }

            // Fallback to development folder on disk
            Path devPath = Paths.get("development", "level_up_project.min.json");
            if (Files.exists(devPath)) {
                return objectMapper.readTree(Files.readAllBytes(devPath));
            }

            Path parentDevPath = Paths.get("..", "development", "level_up_project.min.json");
            if (Files.exists(parentDevPath)) {
                return objectMapper.readTree(Files.readAllBytes(parentDevPath));
            }

            throw new IllegalStateException("Sample file level_up_project.min.json not found");
        } catch (Exception e) {
            throw new RuntimeException("Could not load sample career plan: " + e.getMessage(), e);
        }
    }

    @Transactional
    public CareerPlan importSamplePlan() {
        JsonNode sampleJson = loadSamplePlan();
        return importPlan(sampleJson);
    }
}
