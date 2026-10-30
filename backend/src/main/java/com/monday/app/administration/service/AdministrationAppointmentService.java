package com.monday.app.administration.service;

import com.monday.app.administration.dto.AppointmentDto;
import com.monday.app.administration.dto.ChecklistDto;
import com.monday.app.administration.dto.CreateAppointmentRequest;
import com.monday.app.administration.dto.UpdateAppointmentRequest;
import com.monday.app.event.entity.Event;
import com.monday.app.event.repository.EventRepository;
import com.monday.app.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class AdministrationAppointmentService {

    private final EventRepository eventRepository;
    private final AdministrationChecklistService checklistService;

    public AdministrationAppointmentService(EventRepository eventRepository,
                                            AdministrationChecklistService checklistService) {
        this.eventRepository = eventRepository;
        this.checklistService = checklistService;
    }

    @Transactional(readOnly = true)
    public List<AppointmentDto> getAppointments(String query, String eventType, String status, Instant from, Instant to) {
        Instant now = Instant.now();
        List<Event> events;

        if (from != null && to != null) {
            events = eventRepository.findUpcoming(from, to);
        } else {
            events = eventRepository.findAll();
        }

        return events.stream()
                .filter(e -> {
                    if (query != null && !query.isBlank()) {
                        String q = query.toLowerCase().trim();
                        boolean matchTitle = e.getTitle() != null && e.getTitle().toLowerCase().contains(q);
                        boolean matchDesc = e.getDescription() != null && e.getDescription().toLowerCase().contains(q);
                        boolean matchLoc = e.getLocation() != null && e.getLocation().toLowerCase().contains(q);
                        if (!matchTitle && !matchDesc && !matchLoc) return false;
                    }
                    if (eventType != null && !eventType.isBlank() && !"all".equalsIgnoreCase(eventType)) {
                        if (e.getEventType() == null || !e.getEventType().equalsIgnoreCase(eventType)) {
                            return false;
                        }
                    }
                    if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
                        if (e.getStatus() == null || !e.getStatus().equalsIgnoreCase(status)) {
                            return false;
                        }
                    }
                    return true;
                })
                .map(e -> toDto(e, now))
                .sorted(Comparator.comparing(AppointmentDto::startAt))
                .toList();
    }

    @Transactional(readOnly = true)
    public AppointmentDto getById(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment/event not found: " + id));
        return toDto(event, Instant.now());
    }

    @Transactional
    public AppointmentDto create(CreateAppointmentRequest req) {
        Event e = new Event();
        e.setTitle(req.title().trim());
        e.setDescription(req.description());
        e.setEventType(req.resolveEventType().toUpperCase().trim());
        e.setStartAt(req.resolveStartAt());
        e.setEndAt(req.resolveEndAt());
        e.setAllDay(req.resolveAllDay());
        e.setLocation(req.location());
        e.setStatus(req.status() != null ? req.status().toUpperCase().trim() : "SCHEDULED");
        e.setRequiresPrep(req.requiresPrep() != null ? req.requiresPrep() : (req.resolvePrepNotes() != null));
        e.setPrepTitle(req.resolvePrepNotes());
        e.setPrepDueText(req.prepDueText());

        Event saved = eventRepository.save(e);
        return toDto(saved, Instant.now());
    }

    @Transactional
    public AppointmentDto update(UUID id, UpdateAppointmentRequest req) {
        Event e = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment/event not found: " + id));

        if (req.title() != null && !req.title().isBlank()) e.setTitle(req.title().trim());
        if (req.description() != null) e.setDescription(req.description());
        if (req.eventType() != null) e.setEventType(req.eventType().toUpperCase().trim());
        if (req.startAt() != null) e.setStartAt(req.startAt());
        if (req.endAt() != null) e.setEndAt(req.endAt());
        if (req.allDay() != null) e.setAllDay(req.allDay());
        if (req.location() != null) e.setLocation(req.location());
        if (req.status() != null) e.setStatus(req.status().toUpperCase().trim());
        if (req.requiresPrep() != null) e.setRequiresPrep(req.requiresPrep());
        if (req.prepTitle() != null) e.setPrepTitle(req.prepTitle());
        if (req.prepDueText() != null) e.setPrepDueText(req.prepDueText());

        Event updated = eventRepository.update(e);
        return toDto(updated, Instant.now());
    }

    @Transactional
    public AppointmentDto updateStatus(UUID id, String status) {
        Event e = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment/event not found: " + id));
        e.setStatus(status.toUpperCase().trim());
        return toDto(eventRepository.update(e), Instant.now());
    }

    @Transactional
    public void delete(UUID id) {
        eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment/event not found: " + id));
        eventRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<AppointmentDto> getUpcomingAppointments(int limit) {
        Instant now = Instant.now();
        Instant endWindow = now.plus(30, ChronoUnit.DAYS);
        return eventRepository.findUpcoming(now, endWindow).stream()
                .map(e -> toDto(e, now))
                .sorted(Comparator.comparing(AppointmentDto::startAt))
                .limit(limit)
                .toList();
    }

    public AppointmentDto toDto(Event e, Instant now) {
        long daysRemaining = e.getStartAt() != null ? ChronoUnit.DAYS.between(now, e.getStartAt()) : 0;
        boolean isUpcoming = e.getStartAt() != null && e.getStartAt().isAfter(now) && !"CANCELLED".equals(e.getStatus()) && !"COMPLETED".equals(e.getStatus());

        ChecklistDto linkedChecklist = null;
        List<ChecklistDto> checklists = checklistService.getChecklistsForRecord("APPOINTMENT", e.getId());
        if (!checklists.isEmpty()) {
            linkedChecklist = checklists.get(0);
        }

        return new AppointmentDto(
                e.getId(),
                e.getTitle(),
                e.getDescription(),
                e.getEventType(),
                e.getStartAt(),
                e.getEndAt(),
                e.isAllDay(),
                e.getLocation(),
                e.getStatus(),
                e.isRequiresPrep(),
                e.getPrepTitle(),
                e.getPrepDueText(),
                daysRemaining,
                isUpcoming,
                linkedChecklist,
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
