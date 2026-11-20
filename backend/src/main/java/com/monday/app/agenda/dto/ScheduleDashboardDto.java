package com.monday.app.agenda.dto;

import com.monday.app.reminder.entity.Reminder;

import java.util.List;

public record ScheduleDashboardDto(
        List<EventSummaryDto> eventsToday,
        List<Reminder> remindersToday,
        List<EventPrepItemDto> incompletePrepItems,
        List<ScheduleConflictDto> conflicts,
        int totalEventsCount,
        int allDayEventsCount,
        int scheduledMinutes,
        List<AdministrationAgendaSummaryDto.AdminImportantDateSummaryDto> occasionsToday
) {
    public ScheduleDashboardDto(
            List<EventSummaryDto> eventsToday,
            List<Reminder> remindersToday,
            List<EventPrepItemDto> incompletePrepItems,
            List<ScheduleConflictDto> conflicts,
            int totalEventsCount,
            int allDayEventsCount,
            int scheduledMinutes
    ) {
        this(
                eventsToday,
                remindersToday,
                incompletePrepItems,
                conflicts,
                totalEventsCount,
                allDayEventsCount,
                scheduledMinutes,
                List.of()
        );
    }
}
