package com.monday.app.administration.service;

import com.monday.app.administration.dto.CreateImportantDateRequest;
import com.monday.app.administration.dto.ImportantDateDto;
import com.monday.app.administration.dto.UpdateImportantDateRequest;
import com.monday.app.administration.entity.AdministrationImportantDate;
import com.monday.app.administration.repository.AdministrationImportantDateRepository;
import com.monday.app.administration.util.AdministrationDateUtils;
import com.monday.app.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class AdministrationImportantDateService {

    private final AdministrationImportantDateRepository repository;

    public AdministrationImportantDateService(AdministrationImportantDateRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ImportantDateDto> getDates(String query, String dateType, Boolean archived) {
        LocalDate today = LocalDate.now();
        return repository.search(query, dateType, archived).stream()
                .map(d -> toDto(d, today))
                .sorted(Comparator.comparingLong(ImportantDateDto::daysRemaining))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ImportantDateDto> getByPersonId(UUID personId, boolean includeArchived) {
        LocalDate today = LocalDate.now();
        return repository.findByPersonId(personId, includeArchived).stream()
                .map(d -> toDto(d, today))
                .sorted(Comparator.comparingLong(ImportantDateDto::daysRemaining))
                .toList();
    }

    @Transactional(readOnly = true)
    public ImportantDateDto getById(UUID id) {
        AdministrationImportantDate date = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administration important date not found: " + id));
        return toDto(date, LocalDate.now());
    }

    @Transactional
    public ImportantDateDto create(CreateImportantDateRequest req) {
        AdministrationImportantDate d = new AdministrationImportantDate();
        d.setTitle(req.title().trim());
        d.setDescription(req.description());
        d.setDate(req.resolveDate());
        d.setDateType(req.dateType() != null ? req.dateType().toUpperCase().trim() : "ANNUAL");
        d.setPersonId(req.personId());
        d.setRecurrenceType(req.recurrenceType() != null ? req.recurrenceType().toUpperCase().trim() : "ANNUAL");
        d.setRecurrenceRule(req.recurrenceRule());
        d.setReminderLeadDays(req.reminderLeadDays() != null ? req.reminderLeadDays() : 1);

        AdministrationImportantDate saved = repository.save(d);
        return toDto(saved, LocalDate.now());
    }

    @Transactional
    public ImportantDateDto update(UUID id, UpdateImportantDateRequest req) {
        AdministrationImportantDate d = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administration important date not found: " + id));

        if (req.title() != null && !req.title().isBlank()) d.setTitle(req.title().trim());
        if (req.description() != null) d.setDescription(req.description());
        if (req.date() != null) d.setDate(req.date());
        if (req.dateType() != null) d.setDateType(req.dateType().toUpperCase().trim());
        if (req.personId() != null) d.setPersonId(req.personId());
        if (req.recurrenceType() != null) d.setRecurrenceType(req.recurrenceType().toUpperCase().trim());
        if (req.recurrenceRule() != null) d.setRecurrenceRule(req.recurrenceRule());
        if (req.reminderLeadDays() != null) d.setReminderLeadDays(req.reminderLeadDays());

        AdministrationImportantDate updated = repository.update(d);
        return toDto(updated, LocalDate.now());
    }

    @Transactional
    public void archive(UUID id) {
        repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administration important date not found: " + id));
        repository.archive(id);
    }

    @Transactional
    public void unarchive(UUID id) {
        repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administration important date not found: " + id));
        repository.unarchive(id);
    }

    @Transactional
    public void delete(UUID id) {
        repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administration important date not found: " + id));
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ImportantDateDto> getUpcomingOccasions(int limit) {
        LocalDate today = LocalDate.now();
        return repository.findAll(false).stream()
                .map(d -> toDto(d, today))
                .sorted(Comparator.comparingLong(ImportantDateDto::daysRemaining))
                .limit(limit)
                .toList();
    }

    public ImportantDateDto toDto(AdministrationImportantDate d, LocalDate today) {
        LocalDate nextOccurrence = AdministrationDateUtils.calculateNextImportantDateOccurrence(
                d.getDate(), d.getRecurrenceType(), today);
        long daysRemaining = nextOccurrence != null
                ? AdministrationDateUtils.calculateDaysRemaining(nextOccurrence, today)
                : 0;
        boolean isUpcomingSoon = daysRemaining >= 0 && daysRemaining <= d.getReminderLeadDays();
        String personName = repository.findPersonName(d.getPersonId());

        return new ImportantDateDto(
                d.getId(),
                d.getTitle(),
                d.getDescription(),
                d.getDate(),
                d.getDateType(),
                d.getPersonId(),
                personName,
                d.getRecurrenceType(),
                d.getRecurrenceRule(),
                d.getReminderLeadDays(),
                nextOccurrence,
                daysRemaining,
                isUpcomingSoon,
                d.getArchivedAt(),
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }
}
