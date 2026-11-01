package com.monday.app.administration.service;

import com.monday.app.administration.dto.DailyHabitDigestDto;
import com.monday.app.administration.dto.HabitOccurrenceDto;
import com.monday.app.administration.dto.TodayHabitViewDto;
import com.monday.app.administration.entity.AdministrationHabitReminder;
import com.monday.app.administration.repository.AdministrationHabitReminderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
public class AdministrationHabitReminderService {

    // Quiet hours configuration: 22:00 to 07:00
    public static final LocalTime QUIET_HOURS_START = LocalTime.of(22, 0);
    public static final LocalTime QUIET_HOURS_END = LocalTime.of(7, 0);

    private final AdministrationHabitExecutionService executionService;
    private final AdministrationHabitReminderRepository reminderRepository;

    public AdministrationHabitReminderService(
            AdministrationHabitExecutionService executionService,
            AdministrationHabitReminderRepository reminderRepository
    ) {
        this.executionService = executionService;
        this.reminderRepository = reminderRepository;
    }

    public boolean isQuietHours(LocalTime time) {
        if (time == null) time = LocalTime.now();
        return time.isAfter(QUIET_HOURS_START) || time.isBefore(QUIET_HOURS_END);
    }

    @Transactional
    public DailyHabitDigestDto getDailyDigest(LocalDate date) {
        if (date == null) date = LocalDate.now();
        TodayHabitViewDto today = executionService.getTodayView(date);

        List<HabitOccurrenceDto> allScheduled = new ArrayList<>();
        allScheduled.addAll(today.pendingHabits());
        allScheduled.addAll(today.completedHabits());

        List<HabitOccurrenceDto> morning = new ArrayList<>();
        List<HabitOccurrenceDto> evening = new ArrayList<>();
        List<HabitOccurrenceDto> remaining = new ArrayList<>(today.pendingHabits());

        for (HabitOccurrenceDto o : allScheduled) {
            List<AdministrationHabitReminder> reminders = reminderRepository.findByHabitId(o.habitId());
            boolean placed = false;

            for (AdministrationHabitReminder r : reminders) {
                if (r.isEnabled() && r.getReminderTime() != null) {
                    if (r.getReminderTime().isBefore(LocalTime.of(12, 0))) {
                        morning.add(o);
                        placed = true;
                        break;
                    } else if (r.getReminderTime().isAfter(LocalTime.of(18, 0))) {
                        evening.add(o);
                        placed = true;
                        break;
                    }
                }
            }

            if (!placed) {
                String nameLower = o.habitName().toLowerCase();
                if (nameLower.contains("morning") || nameLower.contains("meditat") || nameLower.contains("breakfast")) {
                    morning.add(o);
                } else if (nameLower.contains("night") || nameLower.contains("evening") || nameLower.contains("journal")) {
                    evening.add(o);
                }
            }
        }

        String msg;
        if (allScheduled.isEmpty()) {
            msg = "No habits scheduled for today. Take time to recharge!";
        } else if (today.completedCount() == allScheduled.size()) {
            msg = "Flawless day! You have completed all " + allScheduled.size() + " scheduled habits.";
        } else {
            msg = "You have " + today.pendingCount() + " of " + allScheduled.size() + " habits remaining today.";
        }

        return new DailyHabitDigestDto(
                date,
                allScheduled.size(),
                morning,
                evening,
                remaining,
                today.completionPercentage(),
                msg
        );
    }
}

