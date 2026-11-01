package com.monday.app.administration.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.app.administration.entity.AdministrationHabit;
import com.monday.app.administration.entity.AdministrationHabitScheduleVersion;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AdministrationHabitRecurrenceEngine {

    private final ObjectMapper objectMapper;

    public AdministrationHabitRecurrenceEngine(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Determines whether a habit is scheduled for a given date according to its frequency type and schedule version.
     * Note: Flexible targets (e.g. WEEKLY_TARGET or flexible monthly) return false for fixed daily scheduling,
     * because flexible habits do not generate arbitrary daily occurrences.
     */
    public boolean isScheduledOn(AdministrationHabit habit, AdministrationHabitScheduleVersion version, LocalDate date) {
        if (date.isBefore(habit.getStartDate())) {
            return false;
        }
        if (habit.getEndDate() != null && date.isAfter(habit.getEndDate())) {
            return false;
        }

        String freq = habit.getFrequencyType() != null ? habit.getFrequencyType().toUpperCase() : "DAILY";
        Map<String, Object> config = parseConfig(version != null ? version.getScheduleConfig() : null);

        return switch (freq) {
            case "DAILY" -> true;
            case "SELECTED_DAYS" -> isSelectedDay(date, config);
            case "INTERVAL_DAYS" -> isIntervalDay(date, habit.getStartDate(), config);
            case "MONTHLY" -> isMonthlyScheduledDay(date, config);
            case "CUSTOM" -> isCustomScheduledDay(date, habit.getStartDate(), config);
            case "WEEKLY_TARGET" -> false; // Flexible target, not fixed daily
            default -> true;
        };
    }

    /**
     * Checks if a frequency type represents a flexible period target (e.g. weekly or monthly target).
     */
    public boolean isFlexibleTarget(String frequencyType, String scheduleConfig) {
        if (frequencyType == null) return false;
        String freq = frequencyType.toUpperCase();
        if ("WEEKLY_TARGET".equals(freq)) return true;
        if ("MONTHLY".equals(freq)) {
            Map<String, Object> config = parseConfig(scheduleConfig);
            String monthlyType = (String) config.get("monthlyType");
            return "OCCURRENCES_PER_MONTH".equalsIgnoreCase(monthlyType);
        }
        return false;
    }

    /**
     * Retrieves the target completions count for flexible targets.
     */
    public int getFlexibleTargetCount(String frequencyType, String scheduleConfig) {
        Map<String, Object> config = parseConfig(scheduleConfig);
        if ("WEEKLY_TARGET".equalsIgnoreCase(frequencyType)) {
            Object tc = config.get("targetCount");
            if (tc instanceof Number num) return num.intValue();
            Object wt = config.get("weeklyTarget");
            if (wt instanceof Number num) return num.intValue();
            return 3; // Default 3 times per week
        }
        if ("MONTHLY".equalsIgnoreCase(frequencyType)) {
            Object tc = config.get("targetCount");
            if (tc instanceof Number num) return num.intValue();
            return 2; // Default 2 times per month
        }
        return 1;
    }

    private boolean isSelectedDay(LocalDate date, Map<String, Object> config) {
        int dayOfWeek = date.getDayOfWeek().getValue(); // 1 = Monday, 7 = Sunday
        Object daysObj = config.get("daysOfWeek");
        if (daysObj instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Number num && num.intValue() == dayOfWeek) {
                    return true;
                }
                if (item instanceof String s) {
                    try {
                        int parsed = Integer.parseInt(s.trim());
                        if (parsed == dayOfWeek) return true;
                    } catch (NumberFormatException ignored) {
                        if (date.getDayOfWeek().name().equalsIgnoreCase(s.trim())) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        return true;
    }

    private boolean isIntervalDay(LocalDate date, LocalDate habitStartDate, Map<String, Object> config) {
        int interval = 3; // Default every 3 days
        Object intervalObj = config.get("intervalDays");
        if (intervalObj instanceof Number num) {
            interval = Math.max(1, num.intValue());
        }

        LocalDate anchor = habitStartDate;
        Object anchorObj = config.get("anchorDate");
        if (anchorObj instanceof String str && !str.isBlank()) {
            try {
                anchor = LocalDate.parse(str.trim());
            } catch (Exception ignored) {}
        }

        if (date.isBefore(anchor)) {
            return false;
        }

        long daysBetween = ChronoUnit.DAYS.between(anchor, date);
        return daysBetween % interval == 0;
    }

    private boolean isMonthlyScheduledDay(LocalDate date, Map<String, Object> config) {
        String monthlyType = (String) config.get("monthlyType");
        if ("OCCURRENCES_PER_MONTH".equalsIgnoreCase(monthlyType)) {
            return false; // Flexible, calculated by logs
        }

        // Fixed day of month
        int targetDay = 1;
        Object dayObj = config.get("dayOfMonth");
        if (dayObj instanceof Number num) {
            targetDay = num.intValue();
        }

        // Handle end-of-month clamping (e.g. 31st on April 30 or Feb 28/29)
        int lengthOfMonth = date.lengthOfMonth();
        int effectiveDay = Math.min(targetDay, lengthOfMonth);
        return date.getDayOfMonth() == effectiveDay;
    }

    private boolean isCustomScheduledDay(LocalDate date, LocalDate habitStartDate, Map<String, Object> config) {
        if (config.containsKey("daysOfWeek")) {
            return isSelectedDay(date, config);
        }
        if (config.containsKey("intervalDays")) {
            return isIntervalDay(date, habitStartDate, config);
        }
        return true;
    }

    public Map<String, Object> parseConfig(String scheduleConfig) {
        if (scheduleConfig == null || scheduleConfig.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(scheduleConfig, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}

