package com.monday.app.administration.service;

import com.monday.app.administration.dto.*;
import com.monday.app.administration.entity.AdministrationHabit;
import com.monday.app.administration.entity.AdministrationHabitLog;
import com.monday.app.administration.entity.AdministrationHabitOccurrence;
import com.monday.app.administration.repository.AdministrationHabitLogRepository;
import com.monday.app.administration.repository.AdministrationHabitOccurrenceRepository;
import com.monday.app.administration.repository.AdministrationHabitRepository;
import com.monday.app.security.SecurityUtils;
import com.monday.app.shared.cache.CacheNames;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class AdministrationHabitAnalyticsService {

    // Configurable thresholds for calendar visualization
    public static final double HIGH_THRESHOLD = 80.0;
    public static final double MODERATE_THRESHOLD = 50.0;

    private final AdministrationHabitRepository habitRepository;
    private final AdministrationHabitOccurrenceRepository occurrenceRepository;
    private final AdministrationHabitLogRepository logRepository;
    private final AdministrationHabitExecutionService executionService;
    private final AdministrationHabitService habitService;

    public AdministrationHabitAnalyticsService(
            AdministrationHabitRepository habitRepository,
            AdministrationHabitOccurrenceRepository occurrenceRepository,
            AdministrationHabitLogRepository logRepository,
            AdministrationHabitExecutionService executionService,
            AdministrationHabitService habitService
    ) {
        this.habitRepository = habitRepository;
        this.occurrenceRepository = occurrenceRepository;
        this.logRepository = logRepository;
        this.executionService = executionService;
        this.habitService = habitService;
    }

    @Transactional
    public CalendarMonthDto getCalendarMonth(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        return new CalendarMonthDto(year, month, getCalendarDays(start, end));
    }

    @Transactional
    public List<CalendarDayDto> getCalendarDays(LocalDate from, LocalDate to) {
        UUID userId = SecurityUtils.getCurrentUserId().orElse(null);

        // Ensure occurrences exist for requested range without individual N+1 calls
        executionService.ensureOccurrencesForDateRange(userId, from, to);

        List<AdministrationHabitOccurrence> occurrences = occurrenceRepository.findByUserIdAndDateRange(userId, from, to);
        Map<UUID, AdministrationHabit> habitMap = new HashMap<>();
        for (AdministrationHabit h : habitRepository.findAllForUser(userId)) {
            habitMap.put(h.getId(), h);
        }

        Map<LocalDate, List<AdministrationHabitOccurrence>> byDate = new HashMap<>();
        for (AdministrationHabitOccurrence o : occurrences) {
            if (habitMap.containsKey(o.getHabitId())) {
                byDate.computeIfAbsent(o.getScheduledDate(), k -> new ArrayList<>()).add(o);
            }
        }

        LocalDate today = LocalDate.now();
        List<CalendarDayDto> result = new ArrayList<>();
        LocalDate curr = from;
        while (!curr.isAfter(to)) {
            List<AdministrationHabitOccurrence> list = byDate.getOrDefault(curr, Collections.emptyList());

            int total = list.size();
            int comp = 0;
            int part = 0;
            int miss = 0;
            int skip = 0;

            for (AdministrationHabitOccurrence o : list) {
                switch (o.getStatus()) {
                    case "COMPLETED" -> comp++;
                    case "PARTIAL" -> part++;
                    case "MISSED" -> miss++;
                    case "SKIPPED" -> skip++;
                }
            }

            int eligible = total - skip;
            double pct = eligible > 0 ? ((double) comp / eligible) * 100.0 : 0.0;
            String ratio = eligible > 0 ? comp + "/" + eligible : "0/0";

            String level;
            if (curr.isAfter(today) || eligible == 0) {
                level = "NEUTRAL";
            } else if (pct >= HIGH_THRESHOLD) {
                level = "HIGH";
            } else if (pct >= MODERATE_THRESHOLD) {
                level = "MODERATE";
            } else {
                level = "LOW";
            }

            result.add(new CalendarDayDto(
                    curr,
                    eligible,
                    comp,
                    part,
                    miss,
                    skip,
                    ratio,
                    Math.round(pct * 10.0) / 10.0,
                    level
            ));

            curr = curr.plusDays(1);
        }

        return result;
    }

    @Transactional
    public CalendarDateDetailDto getCalendarDateDetail(LocalDate date) {
        if (date == null) date = LocalDate.now();
        UUID userId = SecurityUtils.getCurrentUserId().orElse(null);
        TodayHabitViewDto view = executionService.getTodayView(date);

        List<HabitOccurrenceDto> allOccurrences = new ArrayList<>();
        allOccurrences.addAll(view.pendingHabits());
        allOccurrences.addAll(view.completedHabits());

        List<AdministrationHabitLog> logs = logRepository.findByUserIdAndDate(userId, date);
        Map<UUID, AdministrationHabit> habitMap = new HashMap<>();
        for (AdministrationHabit h : habitRepository.findAllForUser(userId)) {
            habitMap.put(h.getId(), h);
        }

        List<HabitLogDto> logDtos = logs.stream()
                .filter(l -> habitMap.containsKey(l.getHabitId()))
                .map(l -> executionService.toLogDto(l, habitMap.containsKey(l.getHabitId()) ? habitMap.get(l.getHabitId()).getName() : null))
                .toList();

        return new CalendarDateDetailDto(
                date,
                view.totalScheduled() - view.skippedCount(),
                view.completedCount(),
                view.partialCount(),
                view.missedCount(),
                view.skippedCount(),
                view.completionPercentage(),
                allOccurrences,
                logDtos
        );
    }

    @Cacheable(value = CacheNames.HABIT_ANALYTICS, keyGenerator = "userAwareKeyGenerator", sync = true)
    @Transactional(readOnly = true)
    public HabitAnalyticsDto getHabitAnalytics(UUID habitId) {
        HabitDto habit = habitService.getById(habitId);
        LocalDate today = LocalDate.now();

        // Historical occurrences up to 90 days ago
        LocalDate past90 = today.minusDays(90);
        List<AdministrationHabitOccurrence> occurrences90 = occurrenceRepository.findByHabitIdAndDateRange(habitId, past90, today);

        // Completion rates for 7, 30, 90 days
        double rate7 = calculateRate(occurrences90, today.minusDays(7), today);
        double rate30 = calculateRate(occurrences90, today.minusDays(30), today);
        double rate90 = calculateRate(occurrences90, past90, today);

        // Accumulated quantity & averages
        BigDecimal totalQty = BigDecimal.ZERO;
        int completedCount = 0;
        int partialCount = 0;
        int missedCount = 0;
        int skippedCount = 0;

        for (AdministrationHabitOccurrence o : occurrences90) {
            if ("COMPLETED".equals(o.getStatus())) {
                completedCount++;
                if (o.getActualValue() != null) totalQty = totalQty.add(o.getActualValue());
            } else if ("PARTIAL".equals(o.getStatus())) {
                partialCount++;
                if (o.getActualValue() != null) totalQty = totalQty.add(o.getActualValue());
            } else if ("MISSED".equals(o.getStatus())) {
                missedCount++;
            } else if ("SKIPPED".equals(o.getStatus())) {
                skippedCount++;
            }
        }

        // Also add flexible logs if applicable
        List<AdministrationHabitLog> logs90 = logRepository.findByHabitIdAndDateRange(habitId, past90, today);
        for (AdministrationHabitLog l : logs90) {
            if (l.getActualValue() != null) totalQty = totalQty.add(l.getActualValue());
            if ("COMPLETED".equals(l.getStatus())) completedCount++;
            else if ("PARTIAL".equals(l.getStatus())) partialCount++;
        }

        int totalEntries = completedCount + partialCount;
        BigDecimal avgQty = totalEntries > 0
                ? totalQty.divide(BigDecimal.valueOf(totalEntries), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Recent activity
        List<AdministrationHabitOccurrence> recent = occurrenceRepository.findRecentByHabitId(habitId, 15);
        List<HabitOccurrenceDto> recentDtos = recent.stream()
                .map(o -> executionService.toOccurrenceDto(o, null, Collections.emptyMap()))
                .toList();

        // Weekly trends for past 6 weeks
        List<WeekTrendItemDto> weeklyTrend = calculateWeeklyTrend(habitId, today, 6);

        return new HabitAnalyticsDto(
                habit,
                habit.currentStreak(),
                habit.longestStreak(),
                rate7,
                rate30,
                rate90,
                totalQty,
                avgQty,
                completedCount,
                partialCount,
                missedCount,
                skippedCount,
                recentDtos,
                weeklyTrend
        );
    }

    @Cacheable(value = CacheNames.HABIT_REPORTS, keyGenerator = "userAwareKeyGenerator", sync = true)
    @Transactional
    public HabitWeeklyReportDto getWeeklyReport(LocalDate date) {
        if (date == null) date = LocalDate.now();
        UUID userId = SecurityUtils.getCurrentUserId().orElse(null);

        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        LocalDate prevWeekStart = weekStart.minusWeeks(1);
        LocalDate prevWeekEnd = weekEnd.minusWeeks(1);

        executionService.ensureOccurrencesForDateRange(userId, prevWeekStart, weekEnd);

        List<AdministrationHabitOccurrence> thisWeekOccurrences = occurrenceRepository.findByUserIdAndDateRange(userId, weekStart, weekEnd);
        List<AdministrationHabitOccurrence> prevWeekOccurrences = occurrenceRepository.findByUserIdAndDateRange(userId, prevWeekStart, prevWeekEnd);

        List<AdministrationHabit> habits = habitRepository.findAllForUser(userId);
        Map<UUID, AdministrationHabit> habitMap = new HashMap<>();
        for (AdministrationHabit h : habits) habitMap.put(h.getId(), h);

        int eligible = 0;
        int completed = 0;
        int partial = 0;
        int missed = 0;
        int skipped = 0;
        Map<String, BigDecimal> outputByUnit = new HashMap<>();

        Map<UUID, int[]> habitStats = new HashMap<>(); // [scheduled, completed, partial, missed, skipped]
        Map<UUID, BigDecimal> habitQuantities = new HashMap<>();

        // Group by scheduled date for perfect days
        Map<LocalDate, List<AdministrationHabitOccurrence>> byDate = new HashMap<>();

        for (AdministrationHabitOccurrence o : thisWeekOccurrences) {
            if (!habitMap.containsKey(o.getHabitId())) continue;
            byDate.computeIfAbsent(o.getScheduledDate(), k -> new ArrayList<>()).add(o);

            int[] stat = habitStats.computeIfAbsent(o.getHabitId(), k -> new int[5]);
            stat[0]++;

            if ("SKIPPED".equals(o.getStatus())) {
                skipped++;
                stat[4]++;
            } else {
                eligible++;
                if ("COMPLETED".equals(o.getStatus())) {
                    completed++;
                    stat[1]++;
                } else if ("PARTIAL".equals(o.getStatus())) {
                    partial++;
                    stat[2]++;
                } else if ("MISSED".equals(o.getStatus())) {
                    missed++;
                    stat[3]++;
                }
            }

            if (o.getActualValue() != null && ("COMPLETED".equals(o.getStatus()) || "PARTIAL".equals(o.getStatus()))) {
                habitQuantities.merge(o.getHabitId(), o.getActualValue(), BigDecimal::add);
                if (o.getTargetUnitSnapshot() != null) {
                    outputByUnit.merge(o.getTargetUnitSnapshot(), o.getActualValue(), BigDecimal::add);
                }
            }
        }

        // Add flexible habit logs for this week
        List<AdministrationHabitLog> thisWeekLogs = logRepository.findByHabitIdsAndDateRange(
                new ArrayList<>(habitMap.keySet()), weekStart, weekEnd
        );
        for (AdministrationHabitLog l : thisWeekLogs) {
            AdministrationHabit h = habitMap.get(l.getHabitId());
            if (h != null) {
                int[] stat = habitStats.computeIfAbsent(h.getId(), k -> new int[5]);
                stat[0]++;
                if ("COMPLETED".equals(l.getStatus())) {
                    completed++;
                    eligible++;
                    stat[1]++;
                } else if ("PARTIAL".equals(l.getStatus())) {
                    partial++;
                    eligible++;
                    stat[2]++;
                }
                if (l.getActualValue() != null) {
                    habitQuantities.merge(h.getId(), l.getActualValue(), BigDecimal::add);
                    if (h.getTargetUnit() != null) {
                        outputByUnit.merge(h.getTargetUnit(), l.getActualValue(), BigDecimal::add);
                    }
                }
            }
        }

        double pct = eligible > 0 ? ((double) completed / eligible) * 100.0 : 0.0;

        // Previous week percentage
        int prevEligible = 0;
        int prevCompleted = 0;
        for (AdministrationHabitOccurrence o : prevWeekOccurrences) {
            if (!habitMap.containsKey(o.getHabitId())) continue;
            if (!"SKIPPED".equals(o.getStatus())) {
                prevEligible++;
                if ("COMPLETED".equals(o.getStatus())) prevCompleted++;
            }
        }
        double prevPct = prevEligible > 0 ? ((double) prevCompleted / prevEligible) * 100.0 : 0.0;
        double delta = Math.round((pct - prevPct) * 10.0) / 10.0;

        // Perfect days
        int perfectDays = 0;
        for (Map.Entry<LocalDate, List<AdministrationHabitOccurrence>> entry : byDate.entrySet()) {
            List<AdministrationHabitOccurrence> list = entry.getValue();
            boolean perfect = true;
            int count = 0;
            for (AdministrationHabitOccurrence o : list) {
                if (!"SKIPPED".equals(o.getStatus())) {
                    count++;
                    if (!"COMPLETED".equals(o.getStatus())) {
                        perfect = false;
                        break;
                    }
                }
            }
            if (perfect && count > 0) perfectDays++;
        }

        // Build habit summaries
        List<HabitPeriodSummaryDto> summaries = new ArrayList<>();
        String bestHabit = null;
        double bestRate = -1.0;
        String leastHabit = null;
        double leastRate = 101.0;

        for (Map.Entry<UUID, int[]> entry : habitStats.entrySet()) {
            UUID hid = entry.getKey();
            AdministrationHabit h = habitMap.get(hid);
            if (h == null) continue;

            int[] s = entry.getValue();
            int sched = s[0];
            int comp = s[1];
            int part = s[2];
            int miss = s[3];
            int skip = s[4];
            int elig = sched - skip;
            double rate = elig > 0 ? ((double) comp / elig) * 100.0 : 0.0;

            if (rate > bestRate && elig > 0) {
                bestRate = rate;
                bestHabit = h.getName();
            }
            if (rate < leastRate && elig > 0) {
                leastRate = rate;
                leastHabit = h.getName();
            }

            summaries.add(new HabitPeriodSummaryDto(
                    hid,
                    h.getName(),
                    h.getCategory(),
                    sched,
                    comp,
                    part,
                    miss,
                    skip,
                    Math.round(rate * 10.0) / 10.0,
                    habitQuantities.getOrDefault(hid, BigDecimal.ZERO),
                    h.getTargetUnit()
            ));
        }

        return new HabitWeeklyReportDto(
                weekStart,
                weekEnd,
                eligible,
                completed,
                partial,
                missed,
                skipped,
                Math.round(pct * 10.0) / 10.0,
                Math.round(prevPct * 10.0) / 10.0,
                delta,
                bestHabit != null ? bestHabit : "N/A",
                leastHabit != null ? leastHabit : "N/A",
                perfectDays,
                outputByUnit,
                summaries
        );
    }

    @Cacheable(value = CacheNames.HABIT_REPORTS, keyGenerator = "userAwareKeyGenerator", sync = true)
    @Transactional
    public HabitMonthlyReportDto getMonthlyReport(int year, int month) {
        UUID userId = SecurityUtils.getCurrentUserId().orElse(null);
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        YearMonth prevYm = ym.minusMonths(1);
        LocalDate prevStart = prevYm.atDay(1);
        LocalDate prevEnd = prevYm.atEndOfMonth();

        executionService.ensureOccurrencesForDateRange(userId, prevStart, end);

        List<AdministrationHabitOccurrence> monthOccurrences = occurrenceRepository.findByUserIdAndDateRange(userId, start, end);
        List<AdministrationHabitOccurrence> prevMonthOccurrences = occurrenceRepository.findByUserIdAndDateRange(userId, prevStart, prevEnd);

        List<AdministrationHabit> habits = habitRepository.findAllForUser(userId);
        Map<UUID, AdministrationHabit> habitMap = new HashMap<>();
        for (AdministrationHabit h : habits) habitMap.put(h.getId(), h);

        int completed = 0;
        int partial = 0;
        int missed = 0;
        int skipped = 0;
        int eligible = 0;
        Map<String, BigDecimal> outputByUnit = new HashMap<>();

        Map<UUID, int[]> habitStats = new HashMap<>();
        Map<UUID, BigDecimal> habitQuantities = new HashMap<>();
        Map<LocalDate, List<AdministrationHabitOccurrence>> byDate = new HashMap<>();

        for (AdministrationHabitOccurrence o : monthOccurrences) {
            if (!habitMap.containsKey(o.getHabitId())) continue;
            byDate.computeIfAbsent(o.getScheduledDate(), k -> new ArrayList<>()).add(o);

            int[] stat = habitStats.computeIfAbsent(o.getHabitId(), k -> new int[5]);
            stat[0]++;

            if ("SKIPPED".equals(o.getStatus())) {
                skipped++;
                stat[4]++;
            } else {
                eligible++;
                if ("COMPLETED".equals(o.getStatus())) {
                    completed++;
                    stat[1]++;
                } else if ("PARTIAL".equals(o.getStatus())) {
                    partial++;
                    stat[2]++;
                } else if ("MISSED".equals(o.getStatus())) {
                    missed++;
                    stat[3]++;
                }
            }

            if (o.getActualValue() != null && ("COMPLETED".equals(o.getStatus()) || "PARTIAL".equals(o.getStatus()))) {
                habitQuantities.merge(o.getHabitId(), o.getActualValue(), BigDecimal::add);
                if (o.getTargetUnitSnapshot() != null) {
                    outputByUnit.merge(o.getTargetUnitSnapshot(), o.getActualValue(), BigDecimal::add);
                }
            }
        }

        // Add flexible logs for month
        List<AdministrationHabitLog> monthLogs = logRepository.findByHabitIdsAndDateRange(
                new ArrayList<>(habitMap.keySet()), start, end
        );
        for (AdministrationHabitLog l : monthLogs) {
            AdministrationHabit h = habitMap.get(l.getHabitId());
            if (h != null) {
                int[] stat = habitStats.computeIfAbsent(h.getId(), k -> new int[5]);
                stat[0]++;
                if ("COMPLETED".equals(l.getStatus())) {
                    completed++;
                    eligible++;
                    stat[1]++;
                } else if ("PARTIAL".equals(l.getStatus())) {
                    partial++;
                    eligible++;
                    stat[2]++;
                }
                if (l.getActualValue() != null) {
                    habitQuantities.merge(h.getId(), l.getActualValue(), BigDecimal::add);
                    if (h.getTargetUnit() != null) {
                        outputByUnit.merge(h.getTargetUnit(), l.getActualValue(), BigDecimal::add);
                    }
                }
            }
        }

        double pct = eligible > 0 ? ((double) completed / eligible) * 100.0 : 0.0;

        int prevEligible = 0;
        int prevCompleted = 0;
        for (AdministrationHabitOccurrence o : prevMonthOccurrences) {
            if (!habitMap.containsKey(o.getHabitId())) continue;
            if (!"SKIPPED".equals(o.getStatus())) {
                prevEligible++;
                if ("COMPLETED".equals(o.getStatus())) prevCompleted++;
            }
        }
        double prevPct = prevEligible > 0 ? ((double) prevCompleted / prevEligible) * 100.0 : 0.0;
        double delta = Math.round((pct - prevPct) * 10.0) / 10.0;

        // Perfect days
        int perfectDays = 0;
        for (Map.Entry<LocalDate, List<AdministrationHabitOccurrence>> entry : byDate.entrySet()) {
            boolean perfect = true;
            int count = 0;
            for (AdministrationHabitOccurrence o : entry.getValue()) {
                if (!"SKIPPED".equals(o.getStatus())) {
                    count++;
                    if (!"COMPLETED".equals(o.getStatus())) {
                        perfect = false;
                        break;
                    }
                }
            }
            if (perfect && count > 0) perfectDays++;
        }

        // Perfect weeks count (full weeks of 100% completion)
        int perfectWeeks = perfectDays / 7;

        // Summaries
        List<HabitPeriodSummaryDto> summaries = new ArrayList<>();
        String bestHabit = null;
        double bestRate = -1.0;
        String leastHabit = null;
        double leastRate = 101.0;

        for (Map.Entry<UUID, int[]> entry : habitStats.entrySet()) {
            UUID hid = entry.getKey();
            AdministrationHabit h = habitMap.get(hid);
            if (h == null) continue;

            int[] s = entry.getValue();
            int elig = s[0] - s[4];
            double rate = elig > 0 ? ((double) s[1] / elig) * 100.0 : 0.0;

            if (rate > bestRate && elig > 0) {
                bestRate = rate;
                bestHabit = h.getName();
            }
            if (rate < leastRate && elig > 0) {
                leastRate = rate;
                leastHabit = h.getName();
            }

            summaries.add(new HabitPeriodSummaryDto(
                    hid,
                    h.getName(),
                    h.getCategory(),
                    s[0],
                    s[1],
                    s[2],
                    s[3],
                    s[4],
                    Math.round(rate * 10.0) / 10.0,
                    habitQuantities.getOrDefault(hid, BigDecimal.ZERO),
                    h.getTargetUnit()
            ));
        }

        // Weekly breakdown in the month
        List<WeekTrendItemDto> weeklyBreakdown = new ArrayList<>();
        LocalDate wStart = start;
        int weekNum = 1;
        while (!wStart.isAfter(end)) {
            LocalDate wEnd = wStart.plusDays(6).isBefore(end) ? wStart.plusDays(6) : end;
            int wElig = 0;
            int wComp = 0;
            for (AdministrationHabitOccurrence o : monthOccurrences) {
                if (!o.getScheduledDate().isBefore(wStart) && !o.getScheduledDate().isAfter(wEnd)) {
                    if (!"SKIPPED".equals(o.getStatus())) {
                        wElig++;
                        if ("COMPLETED".equals(o.getStatus())) wComp++;
                    }
                }
            }
            double wRate = wElig > 0 ? ((double) wComp / wElig) * 100.0 : 0.0;
            weeklyBreakdown.add(new WeekTrendItemDto("Week " + weekNum, Math.round(wRate * 10.0) / 10.0, wComp, wElig));
            wStart = wStart.plusDays(7);
            weekNum++;
        }

        return new HabitMonthlyReportDto(
                year,
                month,
                Math.round(pct * 10.0) / 10.0,
                Math.round(prevPct * 10.0) / 10.0,
                delta,
                completed,
                partial,
                missed,
                skipped,
                perfectDays,
                perfectWeeks,
                bestHabit != null ? bestHabit : "N/A",
                leastHabit != null ? leastHabit : "N/A",
                bestHabit != null ? bestHabit : "N/A",
                outputByUnit,
                weeklyBreakdown,
                summaries
        );
    }

    private double calculateRate(List<AdministrationHabitOccurrence> list, LocalDate from, LocalDate to) {
        int eligible = 0;
        int completed = 0;
        for (AdministrationHabitOccurrence o : list) {
            if (!o.getScheduledDate().isBefore(from) && !o.getScheduledDate().isAfter(to)) {
                if (!"SKIPPED".equals(o.getStatus())) {
                    eligible++;
                    if ("COMPLETED".equals(o.getStatus())) {
                        completed++;
                    }
                }
            }
        }
        return eligible > 0 ? Math.round(((double) completed / eligible * 100.0) * 10.0) / 10.0 : 0.0;
    }

    private List<WeekTrendItemDto> calculateWeeklyTrend(UUID habitId, LocalDate date, int weeksBack) {
        List<WeekTrendItemDto> result = new ArrayList<>();
        LocalDate currentWeekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        for (int i = weeksBack - 1; i >= 0; i--) {
            LocalDate wStart = currentWeekStart.minusWeeks(i);
            LocalDate wEnd = wStart.plusDays(6);

            List<AdministrationHabitOccurrence> weekOccurrences = occurrenceRepository.findByHabitIdAndDateRange(habitId, wStart, wEnd);
            int eligible = 0;
            int completed = 0;
            for (AdministrationHabitOccurrence o : weekOccurrences) {
                if (!"SKIPPED".equals(o.getStatus())) {
                    eligible++;
                    if ("COMPLETED".equals(o.getStatus())) completed++;
                }
            }
            double rate = eligible > 0 ? ((double) completed / eligible) * 100.0 : 0.0;
            String label = "Wk " + wStart.getMonthValue() + "/" + wStart.getDayOfMonth();
            result.add(new WeekTrendItemDto(label, Math.round(rate * 10.0) / 10.0, completed, eligible));
        }
        return result;
    }
}

