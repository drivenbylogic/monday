package com.monday.app.event.service;

import com.monday.app.event.entity.Event;
import com.monday.app.event.repository.EventRepository;
import com.monday.app.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional
    public Event create(Event event) {
        if (event.getStatus() == null) {
            event.setStatus("SCHEDULED");
        }
        if (event.getEventType() == null) {
            event.setEventType("PERSONAL");
        }
        Event saved = eventRepository.save(event);
        if (event.isRequiresPrep() && event.getPrepTasks() != null && !event.getPrepTasks().isEmpty()) {
            eventRepository.savePrepItems(saved.getId(), event.getPrepTasks());
            saved.setPrepChecklist(eventRepository.getPrepItemsForEvent(saved.getId()));
        }
        return saved;
    }

    @Transactional
    public Event update(UUID id, Event updated) {
        Event existing = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));

        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setEventType(updated.getEventType());
        existing.setStartAt(updated.getStartAt());
        existing.setEndAt(updated.getEndAt());
        existing.setAllDay(updated.isAllDay());
        existing.setRecurring(updated.isRecurring());
        existing.setRecurrencePattern(updated.getRecurrencePattern());
        existing.setLocation(updated.getLocation());
        existing.setStatus(updated.getStatus());
        existing.setRequiresPrep(updated.isRequiresPrep());
        existing.setPrepTitle(updated.getPrepTitle());
        existing.setPrepDueText(updated.getPrepDueText());

        Event saved = eventRepository.update(existing);
        if (updated.getPrepTasks() != null) {
            eventRepository.savePrepItems(saved.getId(), updated.getPrepTasks());
        }
        if (saved.isRequiresPrep()) {
            saved.setPrepChecklist(eventRepository.getPrepItemsForEvent(saved.getId()));
        }
        return saved;
    }

    public Event getById(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));
        if (event.isRequiresPrep()) {
            event.setPrepChecklist(eventRepository.getPrepItemsForEvent(event.getId()));
        }
        return event;
    }

    public List<Event> getAll() {
        List<Event> events = eventRepository.findAll();
        for (Event event : events) {
            if (event.isRequiresPrep()) {
                event.setPrepChecklist(eventRepository.getPrepItemsForEvent(event.getId()));
            }
        }
        return events;
    }

    public List<Event> getUpcoming(Instant from, Instant to) {
        List<Event> events = eventRepository.findUpcoming(from, to);
        for (Event event : events) {
            if (event.isRequiresPrep()) {
                event.setPrepChecklist(eventRepository.getPrepItemsForEvent(event.getId()));
            }
        }
        return events;
    }

    @Transactional
    public void delete(UUID id) {
        eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));
        eventRepository.deleteById(id);
    }

    public java.util.Map<String, Object> getPulse(String month, String tz) {
        if (month == null) {
            month = java.time.YearMonth.now().toString();
        }
        return eventRepository.getPulse(month, tz);
    }

    public List<java.util.Map<String, Object>> getDayEvents(String date) {
        return eventRepository.getDayEvents(date);
    }

    public List<java.util.Map<String, Object>> getPrepQueue(String status, int limit) {
        return eventRepository.getPrepQueue(status, limit);
    }

    public java.util.Map<String, Object> togglePrepItem(String eventId, String itemId, boolean completed) {
        return eventRepository.togglePrepItem(eventId, itemId, completed);
    }
}
