package com.monday.app.agenda.service;

import com.monday.app.agenda.dto.DailyAgenda;
import com.monday.app.event.entity.Event;
import com.monday.app.event.repository.EventRepository;
import com.monday.app.project.entity.Task;
import com.monday.app.project.repository.TaskRepository;
import com.monday.app.reminder.entity.Reminder;
import com.monday.app.reminder.repository.ReminderRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AgendaEngineService {

    private final TaskRepository taskRepository;
    private final EventRepository eventRepository;
    private final ReminderRepository reminderRepository;

    public AgendaEngineService(TaskRepository taskRepository,
                               EventRepository eventRepository,
                               ReminderRepository reminderRepository) {
        this.taskRepository = taskRepository;
        this.eventRepository = eventRepository;
        this.reminderRepository = reminderRepository;
    }

    /**
     * Aggregates tasks, events, and reminders for a specific date.
     */
    public DailyAgenda getAgendaForDate(LocalDate date, ZoneId userZoneId) {
        // Convert LocalDate to UTC Instants for start and end of the day in user's timezone
        Instant startOfDay = date.atStartOfDay(userZoneId).toInstant();
        Instant endOfDay = startOfDay.plus(1, ChronoUnit.DAYS).minusMillis(1);

        // Fetch events for the day
        List<Event> events = eventRepository.findUpcoming(startOfDay, endOfDay);

        // Fetch tasks due on or before the end of this day (but not overdue from previous days, unless we want to include them in the due list. Let's keep them separate).
        // For tasks due today, we can filter findDueByDate
        List<Task> allDueOrOverdue = taskRepository.findDueByDate(endOfDay);
        List<Task> tasksDueToday = allDueOrOverdue.stream()
                .filter(t -> t.getDueAt() != null && !t.getDueAt().isBefore(startOfDay))
                .toList();
        
        List<Task> tasksOverdue = taskRepository.findOverdue();

        // Fetch pending reminders due on or before the end of the day
        List<Reminder> pendingReminders = reminderRepository.findPendingDueBefore(endOfDay);

        return new DailyAgenda(date, events, tasksDueToday, tasksOverdue, pendingReminders);
    }
}
