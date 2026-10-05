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
        return eventRepository.save(event);
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

        return eventRepository.update(existing);
    }

    public Event getById(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));
    }

    public List<Event> getAll() {
        return eventRepository.findAll();
    }

    public List<Event> getUpcoming(Instant from, Instant to) {
        return eventRepository.findUpcoming(from, to);
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
