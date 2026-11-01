package com.monday.app.administration.dto;

import java.time.LocalTime;
import java.util.UUID;

public record HabitReminderDto(
        UUID id,
        UUID habitId,
        LocalTime reminderTime,
        boolean enabled
) {}

