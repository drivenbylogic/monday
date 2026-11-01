package com.monday.app.administration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.app.administration.entity.AdministrationHabit;
import com.monday.app.administration.entity.AdministrationHabitScheduleVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class AdministrationHabitRecurrenceEngineTest {

    private AdministrationHabitRecurrenceEngine engine;

    @BeforeEach
    void setUp() {
        engine = new AdministrationHabitRecurrenceEngine(new ObjectMapper());
    }

    @Test
    void testDailyRecurrence() {
        AdministrationHabit habit = new AdministrationHabit();
        habit.setFrequencyType("DAILY");
        habit.setStartDate(LocalDate.of(2026, 10, 1));

        AdministrationHabitScheduleVersion version = new AdministrationHabitScheduleVersion();
        version.setScheduleConfig("{\"type\": \"DAILY\"}");

        // Any date on or after start date is scheduled
        assertTrue(engine.isScheduledOn(habit, version, LocalDate.of(2026, 10, 1)));
        assertTrue(engine.isScheduledOn(habit, version, LocalDate.of(2026, 10, 15)));
        assertTrue(engine.isScheduledOn(habit, version, LocalDate.of(2026, 11, 1)));

        // Date before start date is not scheduled
        assertFalse(engine.isScheduledOn(habit, version, LocalDate.of(2026, 9, 30)));
    }

    @Test
    void testSelectedWeekdaysRecurrence() {
        AdministrationHabit habit = new AdministrationHabit();
        habit.setFrequencyType("SELECTED_DAYS");
        habit.setStartDate(LocalDate.of(2026, 10, 1)); // Oct 1, 2026 is Thursday

        // Scheduled on Monday (1), Wednesday (3), Friday (5)
        AdministrationHabitScheduleVersion version = new AdministrationHabitScheduleVersion();
        version.setScheduleConfig("{\"type\": \"SELECTED_DAYS\", \"daysOfWeek\": [1, 3, 5]}");

        // Oct 2, 2026 is Friday -> true
        assertTrue(engine.isScheduledOn(habit, version, LocalDate.of(2026, 10, 2)));
        // Oct 3, 2026 is Saturday -> false
        assertFalse(engine.isScheduledOn(habit, version, LocalDate.of(2026, 10, 3)));
        // Oct 4, 2026 is Sunday -> false
        assertFalse(engine.isScheduledOn(habit, version, LocalDate.of(2026, 10, 4)));
        // Oct 5, 2026 is Monday -> true
        assertTrue(engine.isScheduledOn(habit, version, LocalDate.of(2026, 10, 5)));
        // Oct 6, 2026 is Tuesday -> false
        assertFalse(engine.isScheduledOn(habit, version, LocalDate.of(2026, 10, 6)));
        // Oct 7, 2026 is Wednesday -> true
        assertTrue(engine.isScheduledOn(habit, version, LocalDate.of(2026, 10, 7)));
    }

    @Test
    void testIntervalDaysRecurrence() {
        AdministrationHabit habit = new AdministrationHabit();
        habit.setFrequencyType("INTERVAL_DAYS");
        habit.setStartDate(LocalDate.of(2026, 10, 1));

        // Every 3 days starting from anchor Oct 1, 2026
        AdministrationHabitScheduleVersion version = new AdministrationHabitScheduleVersion();
        version.setScheduleConfig("{\"type\": \"INTERVAL_DAYS\", \"intervalDays\": 3, \"anchorDate\": \"2026-10-01\"}");

        // Oct 1 (day 0) -> true
        assertTrue(engine.isScheduledOn(habit, version, LocalDate.of(2026, 10, 1)));
        // Oct 2 (day 1) -> false
        assertFalse(engine.isScheduledOn(habit, version, LocalDate.of(2026, 10, 2)));
        // Oct 3 (day 2) -> false
        assertFalse(engine.isScheduledOn(habit, version, LocalDate.of(2026, 10, 3)));
        // Oct 4 (day 3) -> true
        assertTrue(engine.isScheduledOn(habit, version, LocalDate.of(2026, 10, 4)));
        // Oct 7 (day 6) -> true
        assertTrue(engine.isScheduledOn(habit, version, LocalDate.of(2026, 10, 7)));
    }

    @Test
    void testMonthlyDayOfMonthRecurrenceWithClamping() {
        AdministrationHabit habit = new AdministrationHabit();
        habit.setFrequencyType("MONTHLY");
        habit.setStartDate(LocalDate.of(2026, 1, 1));

        // Scheduled on 31st of every month
        AdministrationHabitScheduleVersion version = new AdministrationHabitScheduleVersion();
        version.setScheduleConfig("{\"type\": \"MONTHLY\", \"monthlyType\": \"DAY_OF_MONTH\", \"dayOfMonth\": 31}");

        // Jan 31 -> true
        assertTrue(engine.isScheduledOn(habit, version, LocalDate.of(2026, 1, 31)));
        // Feb 28 (non-leap year, last day clamped) -> true
        assertTrue(engine.isScheduledOn(habit, version, LocalDate.of(2026, 2, 28)));
        // Feb 27 -> false
        assertFalse(engine.isScheduledOn(habit, version, LocalDate.of(2026, 2, 27)));
        // April 30 (30-day month clamped) -> true
        assertTrue(engine.isScheduledOn(habit, version, LocalDate.of(2026, 4, 30)));
    }

    @Test
    void testFlexibleTargetIdentification() {
        assertTrue(engine.isFlexibleTarget("WEEKLY_TARGET", "{\"targetCount\": 3}"));
        assertEquals(3, engine.getFlexibleTargetCount("WEEKLY_TARGET", "{\"targetCount\": 3}"));

        assertTrue(engine.isFlexibleTarget("MONTHLY", "{\"monthlyType\": \"OCCURRENCES_PER_MONTH\", \"targetCount\": 2}"));
        assertEquals(2, engine.getFlexibleTargetCount("MONTHLY", "{\"monthlyType\": \"OCCURRENCES_PER_MONTH\", \"targetCount\": 2}"));

        assertFalse(engine.isFlexibleTarget("DAILY", "{}"));
        assertFalse(engine.isFlexibleTarget("SELECTED_DAYS", "{}"));
    }

    @Test
    void testEndDateEnforcement() {
        AdministrationHabit habit = new AdministrationHabit();
        habit.setFrequencyType("DAILY");
        habit.setStartDate(LocalDate.of(2026, 10, 1));
        habit.setEndDate(LocalDate.of(2026, 10, 10));

        AdministrationHabitScheduleVersion version = new AdministrationHabitScheduleVersion();
        version.setScheduleConfig("{\"type\": \"DAILY\"}");

        assertTrue(engine.isScheduledOn(habit, version, LocalDate.of(2026, 10, 10)));
        assertFalse(engine.isScheduledOn(habit, version, LocalDate.of(2026, 10, 11)));
    }
}

