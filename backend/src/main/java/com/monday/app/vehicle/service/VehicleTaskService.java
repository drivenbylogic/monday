package com.monday.app.vehicle.service;

import com.monday.app.shared.exception.ResourceNotFoundException;
import com.monday.app.vehicle.dto.*;
import com.monday.app.vehicle.entity.VehicleObligation;
import com.monday.app.vehicle.entity.VehicleTask;
import com.monday.app.vehicle.repository.VehicleObligationRepository;
import com.monday.app.vehicle.repository.VehicleRepository;
import com.monday.app.vehicle.repository.VehicleTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VehicleTaskService {

    private final VehicleTaskRepository taskRepository;
    private final VehicleObligationRepository obligationRepository;
    private final VehicleRepository vehicleRepository;

    public VehicleTaskService(VehicleTaskRepository taskRepository,
                              VehicleObligationRepository obligationRepository,
                              VehicleRepository vehicleRepository) {
        this.taskRepository = taskRepository;
        this.obligationRepository = obligationRepository;
        this.vehicleRepository = vehicleRepository;
    }

    // --- Tasks ---

    public List<VehicleTaskDto> getTasks(UUID vehicleId) {
        vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));
        return taskRepository.findByVehicleId(vehicleId).stream()
                .map(this::toTaskDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public VehicleTaskDto createTask(UUID vehicleId, CreateVehicleTaskRequest req) {
        vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));

        VehicleTask task = new VehicleTask(
                null,
                vehicleId,
                req.title() != null ? req.title().trim() : "New Task",
                req.metaText(),
                false,
                req.priority() != null ? req.priority().toUpperCase() : "MEDIUM",
                req.linkedModule() != null ? req.linkedModule() : "VEHICLES",
                null,
                null,
                null
        );

        VehicleTask saved = taskRepository.save(task);
        return toTaskDto(saved);
    }

    @Transactional
    public VehicleTaskDto toggleTask(UUID vehicleId, UUID taskId, boolean completed) {
        VehicleTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("VehicleTask", taskId));

        task.setCompleted(completed);
        task.setCompletedAt(completed ? Instant.now() : null);

        VehicleTask updated = taskRepository.update(task);
        return toTaskDto(updated);
    }

    @Transactional
    public void deleteTask(UUID taskId) {
        taskRepository.deleteById(taskId);
    }

    // --- Obligations ---

    public List<VehicleObligationDto> getObligations(UUID vehicleId) {
        vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));

        LocalDate today = LocalDate.now();
        return obligationRepository.findByVehicleId(vehicleId).stream()
                .map(o -> toObligationDto(o, today))
                .collect(Collectors.toList());
    }

    @Transactional
    public VehicleObligationDto createObligation(UUID vehicleId, CreateVehicleObligationRequest req) {
        vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));

        VehicleObligation obligation = new VehicleObligation(
                null,
                vehicleId,
                req.obligationType() != null ? req.obligationType() : "REGISTRATION",
                req.dueDate() != null ? req.dueDate() : LocalDate.now().plusMonths(6),
                req.amount(),
                req.status() != null ? req.status() : "PENDING",
                req.notes(),
                null,
                null
        );

        VehicleObligation saved = obligationRepository.save(obligation);
        return toObligationDto(saved, LocalDate.now());
    }

    @Transactional
    public VehicleObligationDto updateObligation(UUID id, CreateVehicleObligationRequest req) {
        VehicleObligation existing = obligationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VehicleObligation", id));

        if (req.obligationType() != null) existing.setObligationType(req.obligationType());
        if (req.dueDate() != null) existing.setDueDate(req.dueDate());
        if (req.amount() != null) existing.setAmount(req.amount());
        if (req.status() != null) existing.setStatus(req.status());
        if (req.notes() != null) existing.setNotes(req.notes());

        VehicleObligation updated = obligationRepository.update(existing);
        return toObligationDto(updated, LocalDate.now());
    }

    @Transactional
    public void deleteObligation(UUID id) {
        obligationRepository.deleteById(id);
    }

    private VehicleTaskDto toTaskDto(VehicleTask t) {
        return new VehicleTaskDto(
                t.getId(),
                t.getVehicleId(),
                t.getTitle(),
                t.getMetaText(),
                t.getCompleted(),
                t.getPriority(),
                t.getLinkedModule(),
                t.getCompletedAt(),
                t.getCreatedAt()
        );
    }

    private VehicleObligationDto toObligationDto(VehicleObligation o, LocalDate today) {
        Long daysRemaining = null;
        boolean isDueSoon = false;
        boolean isOverdue = false;

        if (o.getDueDate() != null) {
            daysRemaining = ChronoUnit.DAYS.between(today, o.getDueDate());
            if (daysRemaining < 0) {
                isOverdue = true;
            } else if (daysRemaining <= 30) {
                isDueSoon = true;
            }
        }

        return new VehicleObligationDto(
                o.getId(),
                o.getVehicleId(),
                o.getObligationType(),
                o.getDueDate(),
                o.getAmount(),
                o.getStatus(),
                o.getNotes(),
                daysRemaining,
                isDueSoon,
                isOverdue
        );
    }
}
