package com.monday.app.career.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.app.career.entity.CareerPlan;
import com.monday.app.career.service.CareerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/career")
public class CareerController {

    private final CareerService careerService;
    private final ObjectMapper objectMapper;

    public CareerController(CareerService careerService, ObjectMapper objectMapper) {
        this.careerService = careerService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importPlan(@RequestBody JsonNode planNode) {
        CareerPlan plan = careerService.importPlan(planNode);
        return ResponseEntity.status(HttpStatus.CREATED).body(formatPlanResponse(plan));
    }

    @PostMapping("/import-sample")
    public ResponseEntity<Map<String, Object>> importSamplePlan() {
        CareerPlan plan = careerService.importSamplePlan();
        return ResponseEntity.status(HttpStatus.CREATED).body(formatPlanResponse(plan));
    }

    @GetMapping("/plan")
    public ResponseEntity<Map<String, Object>> getCurrentPlan() {
        return careerService.getCurrentPlan()
                .map(plan -> ResponseEntity.ok(formatPlanResponse(plan)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PutMapping("/tasks/{taskId}/toggle")
    public ResponseEntity<Map<String, Object>> toggleTask(
            @PathVariable String taskId,
            @RequestParam(required = false) Boolean completed) {
        CareerPlan plan = careerService.toggleTask(taskId, completed);
        return ResponseEntity.ok(formatPlanResponse(plan));
    }

    @DeleteMapping("/plan")
    public ResponseEntity<Void> clearCareerData() {
        careerService.clearCareerData();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sample")
    public ResponseEntity<JsonNode> getSamplePlan() {
        return ResponseEntity.ok(careerService.loadSamplePlan());
    }

    private Map<String, Object> formatPlanResponse(CareerPlan plan) {
        try {
            JsonNode dataNode = objectMapper.readTree(plan.getPlanData());
            return Map.of(
                    "id", plan.getId(),
                    "name", plan.getName(),
                    "description", plan.getDescription() != null ? plan.getDescription() : "",
                    "startDate", plan.getStartDate() != null ? plan.getStartDate().toString() : "",
                    "targetDate", plan.getTargetDate() != null ? plan.getTargetDate().toString() : "",
                    "color", plan.getColor() != null ? plan.getColor() : "violet",
                    "viewStyle", plan.getViewStyle() != null ? plan.getViewStyle() : "board",
                    "planData", dataNode,
                    "updatedAt", plan.getUpdatedAt() != null ? plan.getUpdatedAt().toString() : ""
            );
        } catch (Exception e) {
            return Map.of(
                    "id", plan.getId(),
                    "name", plan.getName(),
                    "description", plan.getDescription() != null ? plan.getDescription() : "",
                    "planData", plan.getPlanData()
            );
        }
    }
}
