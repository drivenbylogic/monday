package com.monday.app.agenda.dto;

import com.monday.app.event.entity.Event;
import com.monday.app.project.entity.Task;
import com.monday.app.reminder.entity.Reminder;

import java.time.LocalDate;
import java.util.List;

/**
 * Unified view of a single day's agenda.
 */
public record DailyAgenda(
        LocalDate date,
        List<Event> events,
        List<Task> tasksDue,
        List<Task> tasksOverdue,
        List<Reminder> pendingReminders
) {}
