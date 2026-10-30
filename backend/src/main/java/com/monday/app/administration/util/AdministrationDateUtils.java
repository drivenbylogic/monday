package com.monday.app.administration.util;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

public final class AdministrationDateUtils {

    private AdministrationDateUtils() {}

    /**
     * Computes the next recurring due date for an obligation deterministically,
     * preserving the original anchor day-of-month policy across varying month lengths.
     */
    public static LocalDate calculateNextObligationDueDate(LocalDate startDate, LocalDate currentDueDate, String frequency, String recurrenceRule) {
        if (currentDueDate == null) {
            currentDueDate = startDate != null ? startDate : LocalDate.now();
        }
        if (startDate == null) {
            startDate = currentDueDate;
        }

        int anchorDay = startDate.getDayOfMonth();
        String freq = frequency != null ? frequency.toUpperCase().trim() : "MONTHLY";

        if ("CUSTOM".equals(freq) || freq.startsWith("CUSTOM_") || (recurrenceRule != null && !recurrenceRule.isBlank())) {
            Integer days = extractCustomDays(freq, recurrenceRule);
            if (days != null && days > 0) {
                return currentDueDate.plusDays(days);
            }
        }

        return switch (freq) {
            case "WEEKLY" -> currentDueDate.plusWeeks(1);
            case "MONTHLY" -> {
                YearMonth targetYm = YearMonth.from(currentDueDate).plusMonths(1);
                int day = Math.min(anchorDay, targetYm.lengthOfMonth());
                yield targetYm.atDay(day);
            }
            case "QUARTERLY" -> {
                YearMonth targetYm = YearMonth.from(currentDueDate).plusMonths(3);
                int day = Math.min(anchorDay, targetYm.lengthOfMonth());
                yield targetYm.atDay(day);
            }
            case "HALF_YEARLY", "BI_ANNUAL" -> {
                YearMonth targetYm = YearMonth.from(currentDueDate).plusMonths(6);
                int day = Math.min(anchorDay, targetYm.lengthOfMonth());
                yield targetYm.atDay(day);
            }
            case "YEARLY", "ANNUAL" -> {
                int targetYear = currentDueDate.getYear() + 1;
                Month month = startDate.getMonth();
                YearMonth targetYm = YearMonth.of(targetYear, month);
                int day = Math.min(anchorDay, targetYm.lengthOfMonth());
                yield targetYm.atDay(day);
            }
            default -> currentDueDate.plusMonths(1);
        };
    }

    public static LocalDate calculateNextObligationDueDate(LocalDate startDate, LocalDate currentDueDate, String frequency) {
        return calculateNextObligationDueDate(startDate, currentDueDate, frequency, null);
    }

    private static Integer extractCustomDays(String frequency, String recurrenceRule) {
        if (recurrenceRule != null && !recurrenceRule.isBlank()) {
            String rule = recurrenceRule.trim();
            try {
                if (rule.matches("\\d+")) {
                    return Integer.parseInt(rule);
                }
                String digits = rule.replaceAll("\\D+", "");
                if (!digits.isBlank()) {
                    return Integer.parseInt(digits);
                }
            } catch (Exception ignored) {}
        }
        if (frequency != null && frequency.contains("_")) {
            String digits = frequency.replaceAll("\\D+", "");
            if (!digits.isBlank()) {
                try {
                    return Integer.parseInt(digits);
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    /**
     * Computes the next annual/monthly occurrence for an important date,
     * handling leap day (Feb 29 -> Feb 28 on non-leap years) and past dates deterministically.
     */
    public static LocalDate calculateNextImportantDateOccurrence(LocalDate originalDate, String recurrenceType, LocalDate today) {
        if (originalDate == null) return null;
        if (today == null) today = LocalDate.now();

        String type = recurrenceType != null ? recurrenceType.toUpperCase().trim() : "ANNUAL";

        if ("ONCE".equals(type)) {
            return originalDate;
        }

        if ("MONTHLY".equals(type)) {
            YearMonth ymThisMonth = YearMonth.from(today);
            int dayThisMonth = Math.min(originalDate.getDayOfMonth(), ymThisMonth.lengthOfMonth());
            LocalDate thisMonthDate = ymThisMonth.atDay(dayThisMonth);
            if (!thisMonthDate.isBefore(today)) {
                return thisMonthDate;
            }
            YearMonth ymNextMonth = ymThisMonth.plusMonths(1);
            int dayNextMonth = Math.min(originalDate.getDayOfMonth(), ymNextMonth.lengthOfMonth());
            return ymNextMonth.atDay(dayNextMonth);
        }

        // Default: ANNUAL (Birthdays, Anniversaries)
        YearMonth ymThisYear = YearMonth.of(today.getYear(), originalDate.getMonth());
        int dayThisYear = Math.min(originalDate.getDayOfMonth(), ymThisYear.lengthOfMonth());
        LocalDate thisYearDate = ymThisYear.atDay(dayThisYear);

        if (!thisYearDate.isBefore(today)) {
            return thisYearDate;
        }

        YearMonth ymNextYear = YearMonth.of(today.getYear() + 1, originalDate.getMonth());
        int dayNextYear = Math.min(originalDate.getDayOfMonth(), ymNextYear.lengthOfMonth());
        return ymNextYear.atDay(dayNextYear);
    }

    /**
     * Calculates the days remaining until a target date from reference date (today).
     */
    public static long calculateDaysRemaining(LocalDate targetDate, LocalDate today) {
        if (targetDate == null || today == null) return 0;
        return ChronoUnit.DAYS.between(today, targetDate);
    }

    /**
     * Computes the next scheduled date for a recurring gadget maintenance task.
     */
    public static LocalDate calculateNextMaintenanceDueDate(LocalDate baseDate, String recurrenceRule) {
        if (baseDate == null) baseDate = LocalDate.now();
        if (recurrenceRule == null || recurrenceRule.isBlank()) return baseDate;

        String rule = recurrenceRule.toUpperCase().trim();
        return switch (rule) {
            case "EVERY_MONTH", "MONTHLY" -> baseDate.plusMonths(1);
            case "EVERY_2_MONTHS" -> baseDate.plusMonths(2);
            case "EVERY_3_MONTHS", "QUARTERLY" -> baseDate.plusMonths(3);
            case "EVERY_6_MONTHS", "SEMI_ANNUAL" -> baseDate.plusMonths(6);
            case "EVERY_YEAR", "ANNUAL", "YEARLY" -> baseDate.plusYears(1);
            default -> baseDate.plusMonths(3);
        };
    }

    /**
     * Computes warranty status based on warranty end date and current reference date.
     */
    public static String computeWarrantyStatus(LocalDate warrantyStartDate, LocalDate warrantyEndDate, LocalDate today) {
        if (warrantyEndDate == null) return "UNKNOWN";
        if (today == null) today = LocalDate.now();

        if (today.isAfter(warrantyEndDate)) {
            return "EXPIRED";
        }
        if (!today.plusDays(30).isBefore(warrantyEndDate)) {
            return "EXPIRING_SOON";
        }
        return "ACTIVE";
    }
}
