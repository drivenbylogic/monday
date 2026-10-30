package com.monday.app.administration.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class AdministrationDateUtilsTest {

    @Test
    void testWeeklyObligationRecurrence() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate next = AdministrationDateUtils.calculateNextObligationDueDate(start, start, "WEEKLY");
        assertEquals(LocalDate.of(2026, 10, 8), next);
    }

    @Test
    void testMonthlyObligationRecurrenceMonthEndPolicy() {
        // Anchored on 31st of January:
        LocalDate start = LocalDate.of(2026, 1, 31);

        // Next month: Feb 2026 (non-leap year, 28 days)
        LocalDate feb = AdministrationDateUtils.calculateNextObligationDueDate(start, start, "MONTHLY");
        assertEquals(LocalDate.of(2026, 2, 28), feb);

        // From Feb 28, advancing should target March 31 preserving the original anchor day 31!
        LocalDate march = AdministrationDateUtils.calculateNextObligationDueDate(start, feb, "MONTHLY");
        assertEquals(LocalDate.of(2026, 3, 31), march);

        // Advancing to April: 30 days
        LocalDate april = AdministrationDateUtils.calculateNextObligationDueDate(start, march, "MONTHLY");
        assertEquals(LocalDate.of(2026, 4, 30), april);
    }

    @Test
    void testQuarterlyAndHalfYearlyRecurrence() {
        LocalDate start = LocalDate.of(2026, 1, 31);
        LocalDate q1 = AdministrationDateUtils.calculateNextObligationDueDate(start, start, "QUARTERLY");
        // Jan 31 + 3 months -> April 30
        assertEquals(LocalDate.of(2026, 4, 30), q1);

        LocalDate h1 = AdministrationDateUtils.calculateNextObligationDueDate(start, start, "HALF_YEARLY");
        // Jan 31 + 6 months -> July 31
        assertEquals(LocalDate.of(2026, 7, 31), h1);
    }

    @Test
    void testYearlyLeapYearHandling() {
        // Anchored on Feb 29, 2024 (leap year)
        LocalDate leapStart = LocalDate.of(2024, 2, 29);
        LocalDate nextNonLeap = AdministrationDateUtils.calculateNextObligationDueDate(leapStart, leapStart, "YEARLY");
        // 2025 is not a leap year -> Feb 28
        assertEquals(LocalDate.of(2025, 2, 28), nextNonLeap);
    }

    @Test
    void testImportantDateNextOccurrenceAnnual() {
        LocalDate birthday = LocalDate.of(1992, 6, 14);

        // When today is May 1, 2026 -> next occurrence is June 14, 2026 (same year)
        LocalDate todayBefore = LocalDate.of(2026, 5, 1);
        LocalDate next1 = AdministrationDateUtils.calculateNextImportantDateOccurrence(birthday, "ANNUAL", todayBefore);
        assertEquals(LocalDate.of(2026, 6, 14), next1);

        // When today is July 1, 2026 -> next occurrence is June 14, 2027 (next year)
        LocalDate todayAfter = LocalDate.of(2026, 7, 1);
        LocalDate next2 = AdministrationDateUtils.calculateNextImportantDateOccurrence(birthday, "ANNUAL", todayAfter);
        assertEquals(LocalDate.of(2027, 6, 14), next2);
    }

    @Test
    void testImportantDateLeapDayBirthday() {
        // Born on Feb 29, 2000
        LocalDate leapBirthday = LocalDate.of(2000, 2, 29);

        // In 2026 (non-leap year), birthday should be celebrated on Feb 28
        LocalDate today = LocalDate.of(2026, 1, 1);
        LocalDate next = AdministrationDateUtils.calculateNextImportantDateOccurrence(leapBirthday, "ANNUAL", today);
        assertEquals(LocalDate.of(2026, 2, 28), next);

        // In 2028 (leap year), birthday should fall on Feb 29
        LocalDate today2028 = LocalDate.of(2028, 1, 1);
        LocalDate nextLeap = AdministrationDateUtils.calculateNextImportantDateOccurrence(leapBirthday, "ANNUAL", today2028);
        assertEquals(LocalDate.of(2028, 2, 29), nextLeap);
    }

    @Test
    void testMaintenanceRecurrenceRules() {
        LocalDate base = LocalDate.of(2026, 5, 15);
        assertEquals(LocalDate.of(2026, 6, 15), AdministrationDateUtils.calculateNextMaintenanceDueDate(base, "EVERY_MONTH"));
        assertEquals(LocalDate.of(2026, 8, 15), AdministrationDateUtils.calculateNextMaintenanceDueDate(base, "EVERY_3_MONTHS"));
        assertEquals(LocalDate.of(2026, 11, 15), AdministrationDateUtils.calculateNextMaintenanceDueDate(base, "EVERY_6_MONTHS"));
        assertEquals(LocalDate.of(2027, 5, 15), AdministrationDateUtils.calculateNextMaintenanceDueDate(base, "EVERY_YEAR"));
    }

    @Test
    void testWarrantyStatusComputation() {
        LocalDate today = LocalDate.of(2026, 10, 28);

        // No warranty
        assertEquals("UNKNOWN", AdministrationDateUtils.computeWarrantyStatus(null, null, today));

        // Expired in 2025
        assertEquals("EXPIRED", AdministrationDateUtils.computeWarrantyStatus(
                LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1), today));

        // Expiring in 15 days -> EXPIRING_SOON
        assertEquals("EXPIRING_SOON", AdministrationDateUtils.computeWarrantyStatus(
                LocalDate.of(2024, 1, 1), today.plusDays(15), today));

        // Active (expiring in 1 year)
        assertEquals("ACTIVE", AdministrationDateUtils.computeWarrantyStatus(
                LocalDate.of(2024, 1, 1), today.plusYears(1), today));
    }
}
