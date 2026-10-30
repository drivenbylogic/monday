package com.monday.app.administration.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public record CreateAppointmentRequest(
        @NotBlank(message = "Title is required") String title,
        String description,
        String eventType,
        String appointmentType,
        Instant startAt,
        String startTime,
        Instant endAt,
        String endTime,
        Boolean allDay,
        Boolean isAllDay,
        String location,
        String status,
        Boolean requiresPrep,
        String prepTitle,
        String prepDueText,
        String preparationNotes
) {
    public Instant resolveStartAt() {
        if (startAt != null) return startAt;
        if (startTime != null && !startTime.isBlank()) {
            try {
                return Instant.parse(startTime.endsWith("Z") ? startTime : startTime + "Z");
            } catch (Exception ex) {
                try {
                    return java.time.LocalDateTime.parse(startTime).atZone(java.time.ZoneId.systemDefault()).toInstant();
                } catch (Exception ignored) {}
            }
        }
        return Instant.now().plus(1, ChronoUnit.DAYS);
    }

    public Instant resolveEndAt() {
        if (endAt != null) return endAt;
        if (endTime != null && !endTime.isBlank()) {
            try {
                return Instant.parse(endTime.endsWith("Z") ? endTime : endTime + "Z");
            } catch (Exception ex) {
                try {
                    return java.time.LocalDateTime.parse(endTime).atZone(java.time.ZoneId.systemDefault()).toInstant();
                } catch (Exception ignored) {}
            }
        }
        return resolveStartAt().plus(1, ChronoUnit.HOURS);
    }

    public String resolveEventType() {
        if (eventType != null && !eventType.isBlank()) return eventType;
        if (appointmentType != null && !appointmentType.isBlank()) return appointmentType;
        return "APPOINTMENT";
    }

    public Boolean resolveAllDay() {
        if (allDay != null) return allDay;
        if (isAllDay != null) return isAllDay;
        return false;
    }

    public String resolvePrepNotes() {
        if (prepTitle != null && !prepTitle.isBlank()) return prepTitle;
        if (preparationNotes != null && !preparationNotes.isBlank()) return preparationNotes;
        return null;
    }
}
