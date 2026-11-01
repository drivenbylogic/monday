package com.monday.app.administration.service;

import com.monday.app.administration.dto.*;
import com.monday.app.administration.entity.*;
import com.monday.app.administration.repository.*;
import com.monday.app.security.SecurityUtils;
import com.monday.app.shared.cache.CacheInvalidationService;
import com.monday.app.shared.cache.CacheNames;
import com.monday.app.shared.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class AdministrationHabitExecutionService {

    private final AdministrationHabitRepository habitRepository;
    private final AdministrationHabitGroupRepository groupRepository;
    private final AdministrationHabitScheduleVersionRepository scheduleVersionRepository;
    private final AdministrationHabitOccurrenceRepository occurrenceRepository;
    private final AdministrationHabitLogRepository logRepository;
    private final AdministrationHabitPausePeriodRepository pausePeriodRepository;
    private final AdministrationHabitRecurrenceEngine recurrenceEngine;
    private final CacheInvalidationService cacheInvalidationService;

    public AdministrationHabitExecutionService(
            AdministrationHabitRepository habitRepository,
            AdministrationHabitGroupRepository groupRepository,
            AdministrationHabitScheduleVersionRepository scheduleVersionRepository,
            AdministrationHabitOccurrenceRepository occurrenceRepository,
            AdministrationHabitLogRepository logRepository,
            AdministrationHabitPausePeriodRepository pausePeriodRepository,
            AdministrationHabitRecurrenceEngine recurrenceEngine
    ) {
        this(habitRepository, groupRepository, scheduleVersionRepository, occurrenceRepository,
                logRepository, pausePeriodRepository, recurrenceEngine, null);
    }

    @Autowired
    public AdministrationHabitExecutionService(
            AdministrationHabitRepository habitRepository,
            AdministrationHabitGroupRepository groupRepository,
            AdministrationHabitScheduleVersionRepository scheduleVersionRepository,
            AdministrationHabitOccurrenceRepository occurrenceRepository,
            AdministrationHabitLogRepository logRepository,
            AdministrationHabitPausePeriodRepository pausePeriodRepository,
            AdministrationHabitRecurrenceEngine recurrenceEngine,
            @org.springframework.lang.Nullable CacheInvalidationService cacheInvalidationService
    ) {
        this.habitRepository = habitRepository;
        this.groupRepository = groupRepository;
        this.scheduleVersionRepository = scheduleVersionRepository;
        this.occurrenceRepository = occurrenceRepository;
        this.logRepository = logRepository;
        this.pausePeriodRepository = pausePeriodRepository;
        this.recurrenceEngine = recurrenceEngine;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    private void evictHabitCaches() {
        if (cacheInvalidationService != null) {
            cacheInvalidationService.evictAllAfterCommit(
                    CacheNames.HABIT_ANALYTICS,
                    CacheNames.HABIT_REPORTS,
                    CacheNames.HOME_DASHBOARD
            );
        }
    }

    @Transactional
    public TodayHabitViewDto getTodayView(LocalDate date) {
        if (date == null) date = LocalDate.now();
        UUID userId = SecurityUtils.getCurrentUserId().orElse(null);

        // Reconcile overdue occurrences
        occurrenceRepository.markOverduePendingAsMissed(LocalDate.now());

        // Ensure occurrences are generated for today
        ensureOccurrencesForDateRange(userId, date, date);

        // Fetch occurrences for date
        List<AdministrationHabitOccurrence> occurrences = occurrenceRepository.findByUserIdAndDate(userId, date);
        Map<UUID, AdministrationHabit> habitMap = loadHabitsMap(userId);
        Map<UUID, String> groupNames = loadGroupNames(userId);

        List<HabitOccurrenceDto> dtos = occurrences.stream()
                .filter(o -> habitMap.containsKey(o.getHabitId()))
                .map(o -> toOccurrenceDto(o, habitMap.get(o.getHabitId()), groupNames))
                .toList();

        List<HabitOccurrenceDto> pending = new ArrayList<>();
        List<HabitOccurrenceDto> completed = new ArrayList<>();

        int totalCompleted = 0;
        int totalPartial = 0;
        int totalPending = 0;
        int totalMissed = 0;
        int totalSkipped = 0;

        for (HabitOccurrenceDto o : dtos) {
            switch (o.status()) {
                case "COMPLETED" -> {
                    totalCompleted++;
                    completed.add(o);
                }
                case "PARTIAL" -> {
                    totalPartial++;
                    completed.add(o);
                }
                case "SKIPPED" -> {
                    totalSkipped++;
                    completed.add(o);
                }
                case "MISSED" -> {
                    totalMissed++;
                    completed.add(o);
                }
                default -> {
                    totalPending++;
                    pending.add(o);
                }
            }
        }

        // Flexible habits progress for this week
        List<FlexibleHabitProgressDto> flexible = getFlexibleHabitsProgress(userId, date, habitMap);

        int eligible = dtos.size() - totalSkipped;
        double pct = eligible > 0 ? ((double) totalCompleted / eligible) * 100.0 : 0.0;

        return new TodayHabitViewDto(
                date,
                dtos.size(),
                totalCompleted,
                totalPartial,
                totalPending,
                totalMissed,
                totalSkipped,
                Math.round(pct * 10.0) / 10.0,
                pending,
                completed,
                flexible
        );
    }

    @Transactional
    public WeekHabitViewDto getWeekView(LocalDate date) {
        if (date == null) date = LocalDate.now();
        UUID userId = SecurityUtils.getCurrentUserId().orElse(null);

        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        // Reconcile overdue occurrences
        occurrenceRepository.markOverduePendingAsMissed(LocalDate.now());

        // Ensure occurrences exist for whole week
        ensureOccurrencesForDateRange(userId, weekStart, weekEnd);

        List<AdministrationHabitOccurrence> occurrences = occurrenceRepository.findByUserIdAndDateRange(userId, weekStart, weekEnd);
        Map<UUID, AdministrationHabit> habitMap = loadHabitsMap(userId);
        Map<UUID, String> groupNames = loadGroupNames(userId);

        List<HabitOccurrenceDto> dtos = occurrences.stream()
                .filter(o -> habitMap.containsKey(o.getHabitId()))
                .map(o -> toOccurrenceDto(o, habitMap.get(o.getHabitId()), groupNames))
                .toList();

        // Calculate daily summaries for Mon - Sun
        Map<LocalDate, List<HabitOccurrenceDto>> byDate = new HashMap<>();
        for (HabitOccurrenceDto o : dtos) {
            byDate.computeIfAbsent(o.scheduledDate(), k -> new ArrayList<>()).add(o);
        }

        List<WeekDaySummaryDto> daySummaries = new ArrayList<>();
        LocalDate curr = weekStart;
        while (!curr.isAfter(weekEnd)) {
            List<HabitOccurrenceDto> forDay = byDate.getOrDefault(curr, Collections.emptyList());
            int comp = 0;
            int part = 0;
            int miss = 0;
            int skip = 0;
            for (HabitOccurrenceDto o : forDay) {
                switch (o.status()) {
                    case "COMPLETED" -> comp++;
                    case "PARTIAL" -> part++;
                    case "MISSED" -> miss++;
                    case "SKIPPED" -> skip++;
                }
            }
            int eligible = forDay.size() - skip;
            double pct = eligible > 0 ? ((double) comp / eligible) * 100.0 : 0.0;

            daySummaries.add(new WeekDaySummaryDto(
                    curr,
                    curr.getDayOfWeek().name(),
                    forDay.size(),
                    comp,
                    part,
                    miss,
                    skip,
                    Math.round(pct * 10.0) / 10.0
            ));
            curr = curr.plusDays(1);
        }

        int totalCompleted = 0;
        int totalPartial = 0;
        int totalMissed = 0;
        int totalSkipped = 0;
        for (HabitOccurrenceDto o : dtos) {
            switch (o.status()) {
                case "COMPLETED" -> totalCompleted++;
                case "PARTIAL" -> totalPartial++;
                case "MISSED" -> totalMissed++;
                case "SKIPPED" -> totalSkipped++;
            }
        }
        int eligibleOccurrences = dtos.size() - totalSkipped;
        double overallPct = eligibleOccurrences > 0 ? ((double) totalCompleted / eligibleOccurrences) * 100.0 : 0.0;

        List<FlexibleHabitProgressDto> flexible = getFlexibleHabitsProgress(userId, date, habitMap);

        return new WeekHabitViewDto(
                weekStart,
                weekEnd,
                dtos.size(),
                totalCompleted,
                totalPartial,
                totalMissed,
                totalSkipped,
                Math.round(overallPct * 10.0) / 10.0,
                daySummaries,
                dtos,
                flexible
        );
    }

    @Transactional
    public HabitOccurrenceDto completeOccurrence(UUID id, CompleteOccurrenceRequest req) {
        AdministrationHabitOccurrence o = occurrenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Habit occurrence not found with id: " + id));

        BigDecimal target = o.getTargetValueSnapshot() != null ? o.getTargetValueSnapshot() : BigDecimal.ONE;
        BigDecimal min = o.getMinimumTargetSnapshot();

        BigDecimal actual = (req != null && req.actualValue() != null) ? req.actualValue() : target;

        // Determine if full completion or partial completion
        if (min != null && actual.compareTo(target) < 0 && actual.compareTo(min) >= 0) {
            o.setStatus("PARTIAL");
        } else {
            o.setStatus("COMPLETED");
        }

        o.setActualValue(actual);
        o.setCompletedAt(Instant.now());
        if (req != null && req.notes() != null) {
            o.setNotes(req.notes());
        }

        AdministrationHabitOccurrence updated = occurrenceRepository.update(o);
        AdministrationHabit habit = habitRepository.findById(updated.getHabitId()).orElse(null);
        Map<UUID, String> groupNames = loadGroupNames(habit != null ? habit.getUserId() : null);

        evictHabitCaches();
        return toOccurrenceDto(updated, habit, groupNames);
    }

    @Transactional
    public HabitOccurrenceDto partialOccurrence(UUID id, PartialOccurrenceRequest req) {
        AdministrationHabitOccurrence o = occurrenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Habit occurrence not found with id: " + id));

        BigDecimal min = o.getMinimumTargetSnapshot() != null ? o.getMinimumTargetSnapshot() : BigDecimal.ONE;
        BigDecimal actual = (req != null && req.actualValue() != null) ? req.actualValue() : min;

        o.setStatus("PARTIAL");
        o.setActualValue(actual);
        o.setCompletedAt(Instant.now());
        if (req != null && req.notes() != null) {
            o.setNotes(req.notes());
        }

        AdministrationHabitOccurrence updated = occurrenceRepository.update(o);
        AdministrationHabit habit = habitRepository.findById(updated.getHabitId()).orElse(null);
        Map<UUID, String> groupNames = loadGroupNames(habit != null ? habit.getUserId() : null);

        evictHabitCaches();
        return toOccurrenceDto(updated, habit, groupNames);
    }

    @Transactional
    public HabitOccurrenceDto skipOccurrence(UUID id, SkipOccurrenceRequest req) {
        AdministrationHabitOccurrence o = occurrenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Habit occurrence not found with id: " + id));

        o.setStatus("SKIPPED");
        o.setSkipReason(req != null ? req.reason() : "User skipped habit");
        o.setActualValue(null);
        o.setCompletedAt(null);

        AdministrationHabitOccurrence updated = occurrenceRepository.update(o);
        AdministrationHabit habit = habitRepository.findById(updated.getHabitId()).orElse(null);
        Map<UUID, String> groupNames = loadGroupNames(habit != null ? habit.getUserId() : null);

        evictHabitCaches();
        return toOccurrenceDto(updated, habit, groupNames);
    }

    @Transactional
    public HabitOccurrenceDto undoOccurrence(UUID id) {
        AdministrationHabitOccurrence o = occurrenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Habit occurrence not found with id: " + id));

        // Revert to PENDING, or MISSED if in the past
        LocalDate today = LocalDate.now();
        if (o.getScheduledDate().isBefore(today)) {
            o.setStatus("MISSED");
        } else {
            o.setStatus("PENDING");
        }

        o.setActualValue(null);
        o.setCompletedAt(null);
        o.setSkipReason(null);

        AdministrationHabitOccurrence updated = occurrenceRepository.update(o);
        AdministrationHabit habit = habitRepository.findById(updated.getHabitId()).orElse(null);
        Map<UUID, String> groupNames = loadGroupNames(habit != null ? habit.getUserId() : null);

        evictHabitCaches();
        return toOccurrenceDto(updated, habit, groupNames);
    }

    // Flexible Logs (Weekly/Monthly target completions)
    @Transactional
    public HabitLogDto createLog(CreateHabitLogRequest req) {
        AdministrationHabit habit = habitRepository.findById(req.habitId())
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found with id: " + req.habitId()));

        AdministrationHabitLog log = new AdministrationHabitLog();
        log.setHabitId(req.habitId());
        log.setEntryDate(req.entryDate());
        log.setActualValue(req.actualValue() != null ? req.actualValue() : habit.getTargetValue());

        String status = req.status() != null ? req.status().toUpperCase() : "COMPLETED";
        if (habit.getMinimumTargetValue() != null && log.getActualValue() != null
                && log.getActualValue().compareTo(habit.getTargetValue()) < 0
                && log.getActualValue().compareTo(habit.getMinimumTargetValue()) >= 0) {
            status = "PARTIAL";
        }
        log.setStatus(status);
        log.setNotes(req.notes());

        AdministrationHabitLog saved = logRepository.save(log);
        evictHabitCaches();
        return toLogDto(saved, habit.getName());
    }

    @Transactional
    public HabitLogDto updateLog(UUID id, UpdateHabitLogRequest req) {
        AdministrationHabitLog log = logRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Habit log not found with id: " + id));

        if (req.actualValue() != null) log.setActualValue(req.actualValue());
        if (req.status() != null) log.setStatus(req.status().toUpperCase());
        if (req.notes() != null) log.setNotes(req.notes());

        AdministrationHabitLog updated = logRepository.update(log);
        AdministrationHabit habit = habitRepository.findById(updated.getHabitId()).orElse(null);
        evictHabitCaches();
        return toLogDto(updated, habit != null ? habit.getName() : null);
    }

    @Transactional
    public void deleteLog(UUID id) {
        logRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Habit log not found with id: " + id));
        logRepository.deleteById(id);
        evictHabitCaches();
    }

    public void ensureOccurrencesForDateRange(UUID userId, LocalDate from, LocalDate to) {
        List<AdministrationHabit> activeHabits = habitRepository.findActiveHabits(userId);
        if (activeHabits.isEmpty()) return;

        LocalDate curr = from;
        while (!curr.isAfter(to)) {
            for (AdministrationHabit h : activeHabits) {
                // If paused on this date, skip
                if (pausePeriodRepository.isHabitPausedOnDate(h.getId(), curr)) {
                    continue;
                }

                // Get applicable schedule version for date
                AdministrationHabitScheduleVersion version = scheduleVersionRepository.findActiveVersionOnDate(h.getId(), curr)
                        .orElse(null);

                if (recurrenceEngine.isScheduledOn(h, version, curr)) {
                    // Check if occurrence already exists
                    Optional<AdministrationHabitOccurrence> existing = occurrenceRepository.findByHabitIdAndScheduledDate(h.getId(), curr);
                    if (existing.isEmpty()) {
                        AdministrationHabitOccurrence occ = new AdministrationHabitOccurrence();
                        occ.setHabitId(h.getId());
                        occ.setScheduleVersionId(version != null ? version.getId() : null);
                        occ.setScheduledDate(curr);
                        occ.setTargetValueSnapshot(version != null && version.getTargetValue() != null ? version.getTargetValue() : h.getTargetValue());
                        occ.setMinimumTargetSnapshot(version != null && version.getMinimumTargetValue() != null ? version.getMinimumTargetValue() : h.getMinimumTargetValue());
                        occ.setTargetUnitSnapshot(version != null && version.getTargetUnit() != null ? version.getTargetUnit() : h.getTargetUnit());
                        occ.setStatus(curr.isBefore(LocalDate.now()) ? "MISSED" : "PENDING");
                        occurrenceRepository.save(occ);
                    }
                }
            }
            curr = curr.plusDays(1);
        }
    }

    private List<FlexibleHabitProgressDto> getFlexibleHabitsProgress(UUID userId, LocalDate date, Map<UUID, AdministrationHabit> habitMap) {
        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        List<FlexibleHabitProgressDto> result = new ArrayList<>();
        for (AdministrationHabit h : habitMap.values()) {
            AdministrationHabitScheduleVersion version = scheduleVersionRepository.findLatestVersion(h.getId()).orElse(null);
            String scheduleConfig = version != null ? version.getScheduleConfig() : null;

            if (recurrenceEngine.isFlexibleTarget(h.getFrequencyType(), scheduleConfig)) {
                int targetCount = recurrenceEngine.getFlexibleTargetCount(h.getFrequencyType(), scheduleConfig);
                List<AdministrationHabitLog> logs = logRepository.findByHabitIdAndDateRange(h.getId(), weekStart, weekEnd);

                int completedCount = logs.size();
                int remaining = Math.max(0, targetCount - completedCount);
                double pct = targetCount > 0 ? Math.min(100.0, ((double) completedCount / targetCount) * 100.0) : 0.0;
                boolean loggedToday = logs.stream().anyMatch(l -> l.getEntryDate().equals(date));

                List<HabitLogDto> logDtos = logs.stream()
                        .map(l -> toLogDto(l, h.getName()))
                        .toList();

                result.add(new FlexibleHabitProgressDto(
                        h.getId(),
                        h.getName(),
                        h.getCategory(),
                        targetCount,
                        completedCount,
                        remaining,
                        Math.round(pct * 10.0) / 10.0,
                        h.getTargetUnit(),
                        h.getTargetValue(),
                        loggedToday,
                        logDtos
                ));
            }
        }
        return result;
    }

    public HabitOccurrenceDto toOccurrenceDto(AdministrationHabitOccurrence o, AdministrationHabit h, Map<UUID, String> groupNames) {
        String habitName = h != null ? h.getName() : "Habit";
        String category = h != null ? h.getCategory() : "General";
        String priority = h != null ? h.getPriority() : "MEDIUM";
        Integer duration = h != null ? h.getEstimatedDuration() : null;
        UUID groupId = h != null ? h.getGroupId() : null;
        String groupName = groupId != null ? groupNames.get(groupId) : null;

        return new HabitOccurrenceDto(
                o.getId(),
                o.getHabitId(),
                habitName,
                category,
                o.getScheduledDate(),
                o.getTargetValueSnapshot(),
                o.getMinimumTargetSnapshot(),
                o.getTargetUnitSnapshot(),
                o.getStatus(),
                o.getActualValue(),
                o.getCompletedAt(),
                o.getNotes(),
                o.getSkipReason(),
                priority,
                duration,
                groupId,
                groupName
        );
    }

    public HabitLogDto toLogDto(AdministrationHabitLog l, String habitName) {
        return new HabitLogDto(
                l.getId(),
                l.getHabitId(),
                habitName != null ? habitName : "Flexible Habit",
                l.getEntryDate(),
                l.getActualValue(),
                l.getStatus(),
                l.getNotes(),
                l.getCreatedAt()
        );
    }

    private Map<UUID, AdministrationHabit> loadHabitsMap(UUID userId) {
        Map<UUID, AdministrationHabit> map = new HashMap<>();
        for (AdministrationHabit h : habitRepository.findAllForUser(userId)) {
            map.put(h.getId(), h);
        }
        return map;
    }

    private Map<UUID, String> loadGroupNames(UUID userId) {
        Map<UUID, String> map = new HashMap<>();
        for (AdministrationHabitGroup g : groupRepository.findAllByUserId(userId)) {
            map.put(g.getId(), g.getName());
        }
        return map;
    }
}

