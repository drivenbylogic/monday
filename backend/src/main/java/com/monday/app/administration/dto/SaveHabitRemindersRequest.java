package com.monday.app.administration.dto;

import java.time.LocalTime;
import java.util.List;

public record SaveHabitRemindersRequest(
        List<LocalTime> reminderTimes
) {}

