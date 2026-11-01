package com.monday.app.administration.dto;

import java.util.List;

public record CalendarMonthDto(
        int year,
        int month,
        List<CalendarDayDto> days
) {}

