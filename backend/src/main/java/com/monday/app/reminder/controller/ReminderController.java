package com.monday.app.reminder.controller;

import com.monday.app.reminder.entity.Reminder;
import com.monday.app.reminder.service.ReminderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reminders")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @PostMapping
    public ResponseEntity<Reminder> createReminder(@Valid @RequestBody Reminder reminder) {
        Reminder created = reminderService.create(reminder);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<Reminder>> getReminders(
            @RequestParam(required = false) UUID taskId,
            @RequestParam(required = false) UUID eventId) {
        
        if (taskId != null) {
            return ResponseEntity.ok(reminderService.getByTaskId(taskId));
        } else if (eventId != null) {
            return ResponseEntity.ok(reminderService.getByEventId(eventId));
        }
        return ResponseEntity.ok(reminderService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reminder> getReminderById(@PathVariable UUID id) {
        return ResponseEntity.ok(reminderService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Reminder> updateReminder(@PathVariable UUID id, @Valid @RequestBody Reminder reminder) {
        return ResponseEntity.ok(reminderService.update(id, reminder));
    }

    @PatchMapping("/{id}/dismiss")
    public ResponseEntity<Reminder> dismissReminder(@PathVariable UUID id) {
        return ResponseEntity.ok(reminderService.dismiss(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReminder(@PathVariable UUID id) {
        reminderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
