package com.monday.app.administration.service;

import com.monday.app.administration.dto.CreateObligationRequest;
import com.monday.app.administration.dto.ObligationDto;
import com.monday.app.administration.dto.UpdateObligationRequest;
import com.monday.app.administration.entity.AdministrationObligation;
import com.monday.app.administration.repository.AdministrationObligationRepository;
import com.monday.app.administration.util.AdministrationDateUtils;
import com.monday.app.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class AdministrationObligationService {

    private final AdministrationObligationRepository repository;

    public AdministrationObligationService(AdministrationObligationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ObligationDto> getObligations(String query, String category, String status, Boolean archived) {
        LocalDate today = LocalDate.now();
        return repository.search(query, category, status, archived).stream()
                .map(o -> toDto(o, today))
                .toList();
    }

    @Transactional(readOnly = true)
    public ObligationDto getById(UUID id) {
        AdministrationObligation obligation = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administration obligation not found: " + id));
        return toDto(obligation, LocalDate.now());
    }

    @Transactional
    public ObligationDto create(CreateObligationRequest req) {
        LocalDate startDate = req.startDate() != null ? req.startDate() : LocalDate.now();
        LocalDate nextDueDate = req.nextDueDate() != null ? req.nextDueDate() : startDate;

        AdministrationObligation o = new AdministrationObligation();
        o.setName(req.name().trim());
        o.setDescription(req.description());
        o.setProviderName(req.providerName());
        o.setCategory(req.category() != null ? req.category().toUpperCase().trim() : "OTHER");
        o.setObligationType(req.obligationType() != null ? req.obligationType().toUpperCase().trim() : "BILL");
        o.setExpectedAmount(req.expectedAmount());
        o.setCurrency(req.currency() != null && !req.currency().isBlank() ? req.currency().toUpperCase().trim() : "USD");
        o.setBillingFrequency(req.billingFrequency() != null ? req.billingFrequency().toUpperCase().trim() : "MONTHLY");
        o.setRecurrenceRule(req.recurrenceRule());
        o.setStartDate(startDate);
        o.setNextDueDate(nextDueDate);
        o.setEndDate(req.endDate());
        o.setTrialEndDate(req.trialEndDate());
        o.setCancellationDeadline(req.cancellationDeadline());
        o.setAutoRenew(req.autoRenew() == null || req.autoRenew());
        o.setStatus(req.status() != null ? req.status().toUpperCase().trim() : "ACTIVE");

        AdministrationObligation saved = repository.save(o);
        return toDto(saved, LocalDate.now());
    }

    @Transactional
    public ObligationDto update(UUID id, UpdateObligationRequest req) {
        AdministrationObligation o = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administration obligation not found: " + id));

        if (req.name() != null && !req.name().isBlank()) o.setName(req.name().trim());
        if (req.description() != null) o.setDescription(req.description());
        if (req.providerName() != null) o.setProviderName(req.providerName());
        if (req.category() != null) o.setCategory(req.category().toUpperCase().trim());
        if (req.obligationType() != null) o.setObligationType(req.obligationType().toUpperCase().trim());
        if (req.expectedAmount() != null) o.setExpectedAmount(req.expectedAmount());
        if (req.currency() != null && !req.currency().isBlank()) o.setCurrency(req.currency().toUpperCase().trim());
        if (req.billingFrequency() != null) o.setBillingFrequency(req.billingFrequency().toUpperCase().trim());
        if (req.recurrenceRule() != null) o.setRecurrenceRule(req.recurrenceRule());
        if (req.startDate() != null) o.setStartDate(req.startDate());
        if (req.nextDueDate() != null) o.setNextDueDate(req.nextDueDate());
        if (req.endDate() != null) o.setEndDate(req.endDate());
        if (req.trialEndDate() != null) o.setTrialEndDate(req.trialEndDate());
        if (req.cancellationDeadline() != null) o.setCancellationDeadline(req.cancellationDeadline());
        if (req.autoRenew() != null) o.setAutoRenew(req.autoRenew());
        if (req.status() != null) o.setStatus(req.status().toUpperCase().trim());

        AdministrationObligation updated = repository.update(o);
        return toDto(updated, LocalDate.now());
    }

    @Transactional
    public ObligationDto advanceRecurrence(UUID id) {
        AdministrationObligation o = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administration obligation not found: " + id));

        LocalDate nextDate = AdministrationDateUtils.calculateNextObligationDueDate(
                o.getStartDate(), o.getNextDueDate(), o.getBillingFrequency(), o.getRecurrenceRule());

        if (o.getEndDate() != null && nextDate.isAfter(o.getEndDate())) {
            o.setStatus("EXPIRED");
        } else {
            o.setNextDueDate(nextDate);
            o.setStatus("ACTIVE");
        }

        AdministrationObligation updated = repository.update(o);
        return toDto(updated, LocalDate.now());
    }

    @Transactional
    public ObligationDto markReviewed(UUID id) {
        AdministrationObligation o = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administration obligation not found: " + id));
        o.setStatus("REVIEWED");
        return toDto(repository.update(o), LocalDate.now());
    }

    @Transactional
    public void archive(UUID id) {
        repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administration obligation not found: " + id));
        repository.archive(id);
    }

    @Transactional
    public void unarchive(UUID id) {
        repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administration obligation not found: " + id));
        repository.unarchive(id);
    }

    @Transactional
    public void delete(UUID id) {
        repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administration obligation not found: " + id));
        repository.deleteById(id);
    }

    public ObligationDto toDto(AdministrationObligation o, LocalDate today) {
        long daysRemaining = o.getNextDueDate() != null
                ? AdministrationDateUtils.calculateDaysRemaining(o.getNextDueDate(), today)
                : 0;
        boolean isDueSoon = daysRemaining >= 0 && daysRemaining <= 7 && !"CANCELLED".equals(o.getStatus()) && !"EXPIRED".equals(o.getStatus());
        boolean isOverdue = daysRemaining < 0 && !"PAID".equals(o.getStatus()) && !"CANCELLED".equals(o.getStatus()) && !"EXPIRED".equals(o.getStatus());

        return new ObligationDto(
                o.getId(),
                o.getName(),
                o.getDescription(),
                o.getProviderName(),
                o.getCategory(),
                o.getObligationType(),
                o.getExpectedAmount(),
                o.getCurrency(),
                o.getBillingFrequency(),
                o.getRecurrenceRule(),
                o.getStartDate(),
                o.getNextDueDate(),
                o.getEndDate(),
                o.getTrialEndDate(),
                o.getCancellationDeadline(),
                o.isAutoRenew(),
                o.getStatus(),
                daysRemaining,
                isDueSoon,
                isOverdue,
                o.getArchivedAt(),
                o.getCreatedAt(),
                o.getUpdatedAt()
        );
    }
}
