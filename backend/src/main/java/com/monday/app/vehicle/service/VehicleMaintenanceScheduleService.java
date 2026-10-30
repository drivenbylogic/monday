package com.monday.app.vehicle.service;

import com.monday.app.shared.exception.ResourceNotFoundException;
import com.monday.app.vehicle.dto.CreateMaintenanceScheduleRequest;
import com.monday.app.vehicle.dto.MaintenanceScheduleDto;
import com.monday.app.vehicle.dto.UpdateMaintenanceScheduleRequest;
import com.monday.app.vehicle.entity.Vehicle;
import com.monday.app.vehicle.entity.VehicleMaintenanceSchedule;
import com.monday.app.vehicle.repository.VehicleMaintenanceScheduleRepository;
import com.monday.app.vehicle.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VehicleMaintenanceScheduleService {

    private final VehicleMaintenanceScheduleRepository scheduleRepository;
    private final VehicleRepository vehicleRepository;

    public VehicleMaintenanceScheduleService(VehicleMaintenanceScheduleRepository scheduleRepository,
                                             VehicleRepository vehicleRepository) {
        this.scheduleRepository = scheduleRepository;
        this.vehicleRepository = vehicleRepository;
    }

    public List<MaintenanceScheduleDto> getSchedules(UUID vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));

        List<VehicleMaintenanceSchedule> list = scheduleRepository.findByVehicleId(vehicleId);
        LocalDate today = LocalDate.now();

        return list.stream()
                .map(s -> toDto(s, vehicle.getCurrentKm(), today))
                .collect(Collectors.toList());
    }

    public MaintenanceScheduleDto getSchedule(UUID id) {
        VehicleMaintenanceSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VehicleMaintenanceSchedule", id));
        Vehicle vehicle = vehicleRepository.findById(schedule.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", schedule.getVehicleId()));
        return toDto(schedule, vehicle.getCurrentKm(), LocalDate.now());
    }

    @Transactional
    public MaintenanceScheduleDto createSchedule(UUID vehicleId, CreateMaintenanceScheduleRequest req) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));

        LocalDate nextDueAt = req.nextDueAt();
        Integer nextDueKm = req.nextDueKm();

        if (nextDueAt == null && req.intervalMonths() != null && req.intervalMonths() > 0) {
            LocalDate baseDate = req.lastPerformedAt() != null ? req.lastPerformedAt() : LocalDate.now();
            nextDueAt = baseDate.plusMonths(req.intervalMonths());
        }

        if (nextDueKm == null && req.intervalKm() != null && req.intervalKm() > 0) {
            int baseKm = req.lastPerformedKm() != null ? req.lastPerformedKm() : vehicle.getCurrentKm();
            nextDueKm = baseKm + req.intervalKm();
        }

        VehicleMaintenanceSchedule schedule = new VehicleMaintenanceSchedule(
                null,
                vehicleId,
                req.itemName(),
                req.intervalMonths(),
                req.intervalKm(),
                req.lastPerformedAt(),
                req.lastPerformedKm(),
                nextDueAt,
                nextDueKm,
                "OK",
                null,
                null
        );

        VehicleMaintenanceSchedule saved = scheduleRepository.save(schedule);
        return toDto(saved, vehicle.getCurrentKm(), LocalDate.now());
    }

    @Transactional
    public MaintenanceScheduleDto updateSchedule(UUID id, UpdateMaintenanceScheduleRequest req) {
        VehicleMaintenanceSchedule existing = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VehicleMaintenanceSchedule", id));
        Vehicle vehicle = vehicleRepository.findById(existing.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", existing.getVehicleId()));

        if (req.itemName() != null) existing.setItemName(req.itemName());
        if (req.intervalMonths() != null) existing.setIntervalMonths(req.intervalMonths());
        if (req.intervalKm() != null) existing.setIntervalKm(req.intervalKm());
        if (req.lastPerformedAt() != null) existing.setLastPerformedAt(req.lastPerformedAt());
        if (req.lastPerformedKm() != null) existing.setLastPerformedKm(req.lastPerformedKm());
        if (req.nextDueAt() != null) existing.setNextDueAt(req.nextDueAt());
        if (req.nextDueKm() != null) existing.setNextDueKm(req.nextDueKm());
        if (req.status() != null) existing.setStatus(req.status());

        VehicleMaintenanceSchedule updated = scheduleRepository.update(existing);
        return toDto(updated, vehicle.getCurrentKm(), LocalDate.now());
    }

    @Transactional
    public void advanceScheduleAfterService(UUID scheduleId, LocalDate serviceDate, Integer kmAtService) {
        VehicleMaintenanceSchedule s = scheduleRepository.findById(scheduleId).orElse(null);
        if (s == null) return;

        s.setLastPerformedAt(serviceDate);
        if (kmAtService != null && kmAtService > 0) {
            s.setLastPerformedKm(kmAtService);
        }

        if (s.getIntervalMonths() != null && s.getIntervalMonths() > 0) {
            s.setNextDueAt(serviceDate.plusMonths(s.getIntervalMonths()));
        }

        if (s.getIntervalKm() != null && s.getIntervalKm() > 0 && kmAtService != null) {
            s.setNextDueKm(kmAtService + s.getIntervalKm());
        }

        s.setStatus("OK");
        scheduleRepository.update(s);
    }

    @Transactional
    public void deleteSchedule(UUID id) {
        scheduleRepository.deleteById(id);
    }

    private MaintenanceScheduleDto toDto(VehicleMaintenanceSchedule s, int currentKm, LocalDate today) {
        Long daysRemaining = null;
        Integer kmRemaining = null;
        boolean isOverdue = false;
        boolean isDueSoon = false;

        if (s.getNextDueAt() != null) {
            daysRemaining = ChronoUnit.DAYS.between(today, s.getNextDueAt());
            if (daysRemaining < 0) {
                isOverdue = true;
            } else if (daysRemaining <= 30) {
                isDueSoon = true;
            }
        }

        if (s.getNextDueKm() != null) {
            kmRemaining = s.getNextDueKm() - currentKm;
            if (kmRemaining < 0) {
                isOverdue = true;
            } else if (kmRemaining <= 1000) {
                isDueSoon = true;
            }
        }

        String calculatedStatus = isOverdue ? "OVERDUE" : isDueSoon ? "DUE_SOON" : "OK";

        return new MaintenanceScheduleDto(
                s.getId(),
                s.getVehicleId(),
                s.getItemName(),
                s.getIntervalMonths(),
                s.getIntervalKm(),
                s.getLastPerformedAt(),
                s.getLastPerformedKm(),
                s.getNextDueAt(),
                s.getNextDueKm(),
                calculatedStatus,
                isOverdue,
                isDueSoon,
                kmRemaining,
                daysRemaining,
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}
