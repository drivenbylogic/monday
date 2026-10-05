package com.monday.app.event.controller;

import com.monday.app.event.entity.Event;
import com.monday.app.event.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<Event> createEvent(@Valid @RequestBody Event event) {
        Event created = eventService.create(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<Event>> getEvents(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        
        if (from != null && to != null) {
            return ResponseEntity.ok(eventService.getUpcoming(from, to));
        }
        return ResponseEntity.ok(eventService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Event> updateEvent(@PathVariable UUID id, @Valid @RequestBody Event event) {
        return ResponseEntity.ok(eventService.update(id, event));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID id) {
        eventService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // --- Actual API Endpoints from Architecture Doc ---

    @GetMapping("/pulse")
    public ResponseEntity<java.util.Map<String, Object>> getPulse(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String tz) {
        return ResponseEntity.ok(eventService.getPulse(month, tz));
    }

    @GetMapping("/days/{date}")
    public ResponseEntity<List<java.util.Map<String, Object>>> getDayEvents(@PathVariable String date) {
        return ResponseEntity.ok(eventService.getDayEvents(date));
    }

    @GetMapping("/prep-queue")
    public ResponseEntity<List<java.util.Map<String, Object>>> getPrepQueue(
            @RequestParam(required = false, defaultValue = "pending") String status,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        return ResponseEntity.ok(eventService.getPrepQueue(status, limit));
    }

    @PatchMapping("/{eventId}/prep-items/{itemId}")
    public ResponseEntity<java.util.Map<String, Object>> togglePrepItem(
            @PathVariable String eventId,
            @PathVariable String itemId,
            @RequestBody java.util.Map<String, Object> body) {
        boolean completed = Boolean.TRUE.equals(body.get("completed"));
        return ResponseEntity.ok(eventService.togglePrepItem(eventId, itemId, completed));
    }
}
