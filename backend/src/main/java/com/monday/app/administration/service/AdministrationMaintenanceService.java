package com.monday.app.administration.service;

import com.monday.app.administration.dto.CompleteMaintenanceRequest;
import com.monday.app.administration.dto.CreateMaintenanceRequest;
import com.monday.app.administration.dto.MaintenanceDto;
import com.monday.app.administration.dto.UpdateMaintenanceRequest;
import com.monday.app.administration.entity.AdministrationMaintenance;
import com.monday.app.administration.repository.AdministrationGadgetRepository;
import com.monday.app.administration.repository.AdministrationMaintenanceRepository;
import com.monday.app.administration.util.AdministrationDateUtils;
import com.monday.app.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class AdministrationMaintenanceService {

    private final AdministrationMaintenanceRepository repository;
    private final AdministrationGadgetRepository gadgetRepository;

    public AdministrationMaintenanceService(AdministrationMaintenanceRepository repository,
                                            AdministrationGadgetRepository gadgetRepository) {
        this.repository = repository;
        this.gadgetRepository = gadgetRepository;
    }

    @Transactional(readOnly = true)
    public List<MaintenanceDto> getMaintenanceByGadget(UUID gadgetId) {
        LocalDate today = LocalDate.now();
        return repository.findByGadgetId(gadgetId).stream()
                .map(m -> toDto(m, today))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MaintenanceDto> getAllMaintenance() {
        LocalDate today = LocalDate.now();
        return repository.findAll().stream()
                .map(m -> toDto(m, today))
                .sorted(Comparator.comparing(MaintenanceDto::dueDate))
                .toList();
    }

    @Transactional(readOnly = true)
    public MaintenanceDto getById(UUID id) {
        AdministrationMaintenance m = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance record not found: " + id));
        return toDto(m, LocalDate.now());
    }

    @Transactional
    public MaintenanceDto create(CreateMaintenanceRequest req) {
        gadgetRepository.findById(req.gadgetId())
                .orElseThrow(() -> new ResourceNotFoundException("Gadget not found: " + req.gadgetId()));

        AdministrationMaintenance m = new AdministrationMaintenance();
        m.setGadgetId(req.gadgetId());
        m.setTitle(req.title().trim());
        m.setDescription(req.description());
        m.setMaintenanceType(req.maintenanceType() != null ? req.maintenanceType().toUpperCase().trim() : "SERVICING");
        m.setDueDate(req.dueDate());
        m.setRecurrenceRule(req.recurrenceRule());
        m.setCost(req.cost());
        m.setStatus("UPCOMING");
        m.setServiceProvider(req.serviceProvider());
        m.setServiceNotes(req.serviceNotes());

        AdministrationMaintenance saved = repository.save(m);
        return toDto(saved, LocalDate.now());
    }

    @Transactional
    public MaintenanceDto update(UUID id, UpdateMaintenanceRequest req) {
        AdministrationMaintenance m = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance record not found: " + id));

        if (req.title() != null && !req.title().isBlank()) m.setTitle(req.title().trim());
        if (req.description() != null) m.setDescription(req.description());
        if (req.maintenanceType() != null) m.setMaintenanceType(req.maintenanceType().toUpperCase().trim());
        if (req.dueDate() != null) m.setDueDate(req.dueDate());
        if (req.recurrenceRule() != null) m.setRecurrenceRule(req.recurrenceRule());
        if (req.lastPerformedAt() != null) m.setLastPerformedAt(req.lastPerformedAt());
        if (req.cost() != null) m.setCost(req.cost());
        if (req.status() != null) m.setStatus(req.status().toUpperCase().trim());
        if (req.serviceProvider() != null) m.setServiceProvider(req.serviceProvider());
        if (req.serviceNotes() != null) m.setServiceNotes(req.serviceNotes());

        AdministrationMaintenance updated = repository.update(m);
        return toDto(updated, LocalDate.now());
    }

    @Transactional
    public MaintenanceDto complete(UUID id, CompleteMaintenanceRequest req) {
        AdministrationMaintenance m = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance record not found: " + id));

        LocalDate performedDate = req.performedAt() != null ? req.performedAt() : LocalDate.now();
        m.setStatus("COMPLETED");
        m.setLastPerformedAt(performedDate);
        if (req.cost() != null) m.setCost(req.cost());
        if (req.serviceProvider() != null) m.setServiceProvider(req.serviceProvider());
        if (req.serviceNotes() != null) m.setServiceNotes(req.serviceNotes());

        AdministrationMaintenance updated = repository.update(m);

        // If recurring and scheduleNextOccurrence requested, create a new upcoming occurrence
        if (req.scheduleNextOccurrence() && m.getRecurrenceRule() != null && !m.getRecurrenceRule().isBlank()) {
            LocalDate nextDue = AdministrationDateUtils.calculateNextMaintenanceDueDate(performedDate, m.getRecurrenceRule());
            AdministrationMaintenance nextOccurrence = new AdministrationMaintenance();
            nextOccurrence.setGadgetId(m.getGadgetId());
            nextOccurrence.setTitle(m.getTitle());
            nextOccurrence.setDescription(m.getDescription());
            nextOccurrence.setMaintenanceType(m.getMaintenanceType());
            nextOccurrence.setDueDate(nextDue);
            nextOccurrence.setRecurrenceRule(m.getRecurrenceRule());
            nextOccurrence.setStatus("UPCOMING");
            nextOccurrence.setServiceProvider(m.getServiceProvider());
            repository.save(nextOccurrence);
        }

        return toDto(updated, LocalDate.now());
    }

    @Transactional
    public MaintenanceDto cancel(UUID id) {
        AdministrationMaintenance m = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance record not found: " + id));
        m.setStatus("CANCELLED");
        AdministrationMaintenance updated = repository.update(m);
        return toDto(updated, LocalDate.now());
    }

    @Transactional
    public void delete(UUID id) {
        repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance record not found: " + id));
        repository.deleteById(id);
    }

    public MaintenanceDto toDto(AdministrationMaintenance m, LocalDate today) {
        long daysRemaining = m.getDueDate() != null
                ? AdministrationDateUtils.calculateDaysRemaining(m.getDueDate(), today)
                : 0;
        boolean isOverdue = daysRemaining < 0 && !"COMPLETED".equals(m.getStatus()) && !"CANCELLED".equals(m.getStatus());
        String gadgetName = repository.findGadgetName(m.getGadgetId());

        return new MaintenanceDto(
                m.getId(),
                m.getGadgetId(),
                gadgetName,
                m.getTitle(),
                m.getDescription(),
                m.getMaintenanceType(),
                m.getDueDate(),
                m.getRecurrenceRule(),
                m.getLastPerformedAt(),
                m.getCost(),
                m.getStatus(),
                m.getServiceProvider(),
                m.getServiceNotes(),
                daysRemaining,
                isOverdue,
                m.getCreatedAt(),
                m.getUpdatedAt()
        );
    }
}
