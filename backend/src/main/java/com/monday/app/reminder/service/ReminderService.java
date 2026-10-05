package com.monday.app.reminder.service;

import com.monday.app.reminder.entity.Reminder;
import com.monday.app.reminder.repository.ReminderRepository;
import com.monday.app.shared.exception.BusinessRuleException;
import com.monday.app.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);
    private final ReminderRepository reminderRepository;

    public ReminderService(ReminderRepository reminderRepository) {
        this.reminderRepository = reminderRepository;
    }

    @Transactional
    public Reminder create(Reminder reminder) {
        validateExclusiveTarget(reminder);
        if (reminder.getStatus() == null) {
            reminder.setStatus("PENDING");
        }
        return reminderRepository.save(reminder);
    }

    @Transactional
    public Reminder update(UUID id, Reminder updated) {
        Reminder existing = reminderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder", id));

        existing.setRemindAt(updated.getRemindAt());
        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setStatus(updated.getStatus());
        return reminderRepository.update(existing);
    }

    public Reminder getById(UUID id) {
        return reminderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder", id));
    }

    public List<Reminder> getAll() {
        return reminderRepository.findAll();
    }

    public List<Reminder> getByTaskId(UUID taskId) {
        return reminderRepository.findByTaskId(taskId);
    }

    public List<Reminder> getByEventId(UUID eventId) {
        return reminderRepository.findByEventId(eventId);
    }

    @Transactional
    public Reminder dismiss(UUID id) {
        Reminder reminder = reminderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder", id));
        reminder.setStatus("DISMISSED");
        return reminderRepository.update(reminder);
    }

    @Transactional
    public void delete(UUID id) {
        reminderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder", id));
        reminderRepository.deleteById(id);
    }

    /**
     * Scheduled job: processes pending reminders that are due.
     * Runs every minute.
     */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void processReminders() {
        List<Reminder> dueReminders = reminderRepository.findPendingDueBefore(Instant.now());
        for (Reminder reminder : dueReminders) {
            reminder.setStatus("TRIGGERED");
            reminderRepository.update(reminder);
            log.info("Triggered reminder: {} ({})", reminder.getId(), reminder.getTitle());
        }
        if (!dueReminders.isEmpty()) {
            log.info("Processed {} due reminder(s)", dueReminders.size());
        }
    }

    private void validateExclusiveTarget(Reminder reminder) {
        boolean hasTask = reminder.getTaskId() != null;
        boolean hasEvent = reminder.getEventId() != null;
        if (hasTask == hasEvent) {
            throw new BusinessRuleException("REMINDER_TARGET_INVALID",
                    "Exactly one of taskId or eventId must be provided");
        }
    }
}
