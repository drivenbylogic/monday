package com.monday.app.administration.service;

import com.monday.app.administration.dto.HabitDto;
import com.monday.app.administration.dto.HabitInsightDto;
import com.monday.app.administration.entity.AdministrationHabit;
import com.monday.app.administration.entity.AdministrationHabitOccurrence;
import com.monday.app.administration.repository.AdministrationHabitOccurrenceRepository;
import com.monday.app.administration.repository.AdministrationHabitRepository;
import com.monday.app.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Service
public class AdministrationHabitInsightService {

    private final AdministrationHabitRepository habitRepository;
    private final AdministrationHabitOccurrenceRepository occurrenceRepository;
    private final AdministrationHabitService habitService;

    public AdministrationHabitInsightService(
            AdministrationHabitRepository habitRepository,
            AdministrationHabitOccurrenceRepository occurrenceRepository,
            AdministrationHabitService habitService
    ) {
        this.habitRepository = habitRepository;
        this.occurrenceRepository = occurrenceRepository;
        this.habitService = habitService;
    }

    @Transactional(readOnly = true)
    public List<HabitInsightDto> generateInsights() {
        UUID userId = SecurityUtils.getCurrentUserId().orElse(null);
        List<AdministrationHabit> habits = habitRepository.findActiveHabits(userId);
        if (habits.isEmpty()) return Collections.emptyList();

        LocalDate today = LocalDate.now();
        LocalDate past60 = today.minusDays(60);
        List<AdministrationHabitOccurrence> occurrences = occurrenceRepository.findByHabitIdsAndDateRange(
                habits.stream().map(AdministrationHabit::getId).toList(), past60, today
        );

        Map<UUID, List<AdministrationHabitOccurrence>> byHabit = new HashMap<>();
        for (AdministrationHabitOccurrence o : occurrences) {
            byHabit.computeIfAbsent(o.getHabitId(), k -> new ArrayList<>()).add(o);
        }

        List<HabitInsightDto> insights = new ArrayList<>();

        for (AdministrationHabit h : habits) {
            List<AdministrationHabitOccurrence> list = byHabit.getOrDefault(h.getId(), Collections.emptyList());
            if (list.size() < 7) continue;

            // 1. Streak milestones
            HabitDto habitDto = habitService.getById(h.getId());
            if (habitDto.currentStreak() >= 30) {
                insights.add(new HabitInsightDto(
                        "streak-30-" + h.getId(),
                        "STREAK",
                        "Elite 30-Day Milestone",
                        h.getName() + " has maintained an incredible " + habitDto.currentStreak() + "-day streak!",
                        "POSITIVE",
                        h.getId()
                ));
            } else if (habitDto.currentStreak() >= 7) {
                insights.add(new HabitInsightDto(
                        "streak-7-" + h.getId(),
                        "STREAK",
                        "Consistency Streak",
                        h.getName() + " is on a solid " + habitDto.currentStreak() + "-day streak. Keep it going!",
                        "POSITIVE",
                        h.getId()
                ));
            }

            // 2. Day of week drop-off analysis (e.g. Fridays or Weekends)
            Map<DayOfWeek, Integer> scheduledByDow = new EnumMap<>(DayOfWeek.class);
            Map<DayOfWeek, Integer> completedByDow = new EnumMap<>(DayOfWeek.class);
            Map<DayOfWeek, Integer> missedByDow = new EnumMap<>(DayOfWeek.class);

            for (AdministrationHabitOccurrence o : list) {
                if ("SKIPPED".equals(o.getStatus())) continue;
                DayOfWeek dow = o.getScheduledDate().getDayOfWeek();
                scheduledByDow.merge(dow, 1, Integer::sum);
                if ("COMPLETED".equals(o.getStatus())) completedByDow.merge(dow, 1, Integer::sum);
                else if ("MISSED".equals(o.getStatus())) missedByDow.merge(dow, 1, Integer::sum);
            }

            // Weekend drop-off check
            int weekendScheduled = scheduledByDow.getOrDefault(DayOfWeek.SATURDAY, 0) + scheduledByDow.getOrDefault(DayOfWeek.SUNDAY, 0);
            int weekendMissed = missedByDow.getOrDefault(DayOfWeek.SATURDAY, 0) + missedByDow.getOrDefault(DayOfWeek.SUNDAY, 0);
            if (weekendScheduled >= 4 && ((double) weekendMissed / weekendScheduled) >= 0.5) {
                insights.add(new HabitInsightDto(
                        "weekend-miss-" + h.getId(),
                        "PATTERN",
                        "Weekend Drop-Off Detected",
                        h.getName() + " was missed more frequently on weekends. Consider planning a lighter schedule on Saturdays & Sundays.",
                        "WARNING",
                        h.getId()
                ));
            }

            // Friday drop-off check
            int fridayScheduled = scheduledByDow.getOrDefault(DayOfWeek.FRIDAY, 0);
            int fridayCompleted = completedByDow.getOrDefault(DayOfWeek.FRIDAY, 0);
            if (fridayScheduled >= 3 && ((double) fridayCompleted / fridayScheduled) <= 0.4) {
                insights.add(new HabitInsightDto(
                        "friday-drop-" + h.getId(),
                        "DROP_OFF",
                        "Friday Dip",
                        h.getName() + " consistency is noticeably lower on Fridays (" +
                                Math.round(((double) fridayCompleted / fridayScheduled) * 100) + "%). Try an earlier morning slot.",
                        "WARNING",
                        h.getId()
                ));
            }

            // 3. Month-over-month improvement check
            LocalDate thirtyDaysAgo = today.minusDays(30);
            int recentCompleted = 0;
            int recentEligible = 0;
            int olderCompleted = 0;
            int olderEligible = 0;

            for (AdministrationHabitOccurrence o : list) {
                if ("SKIPPED".equals(o.getStatus())) continue;
                if (!o.getScheduledDate().isBefore(thirtyDaysAgo)) {
                    recentEligible++;
                    if ("COMPLETED".equals(o.getStatus())) recentCompleted++;
                } else {
                    olderEligible++;
                    if ("COMPLETED".equals(o.getStatus())) olderCompleted++;
                }
            }

            if (recentEligible >= 10 && olderEligible >= 10) {
                double recentRate = (double) recentCompleted / recentEligible;
                double olderRate = (double) olderCompleted / olderEligible;
                if (recentRate - olderRate >= 0.15) {
                    insights.add(new HabitInsightDto(
                            "improvement-" + h.getId(),
                            "IMPROVEMENT",
                            "Significant Momentum",
                            h.getName() + " consistency improved by " +
                                    Math.round((recentRate - olderRate) * 100) + "% compared to last month!",
                            "POSITIVE",
                            h.getId()
                    ));
                }
            }
        }

        // 4. Cross-habit Morning vs Evening completion analysis
        int morningCompleted = 0;
        int morningTotal = 0;
        int eveningCompleted = 0;
        int eveningTotal = 0;

        for (AdministrationHabit h : habits) {
            String nameLower = (h.getName() + " " + (h.getDescription() != null ? h.getDescription() : "")).toLowerCase();
            boolean isMorning = nameLower.contains("morning") || nameLower.contains("breakfast") || (h.getEstimatedDuration() != null && h.getEstimatedDuration() <= 15 && nameLower.contains("meditat"));
            boolean isEvening = nameLower.contains("evening") || nameLower.contains("night") || nameLower.contains("journal") || nameLower.contains("wind");

            List<AdministrationHabitOccurrence> list = byHabit.getOrDefault(h.getId(), Collections.emptyList());
            for (AdministrationHabitOccurrence o : list) {
                if ("SKIPPED".equals(o.getStatus())) continue;
                if (isMorning) {
                    morningTotal++;
                    if ("COMPLETED".equals(o.getStatus())) morningCompleted++;
                } else if (isEvening) {
                    eveningTotal++;
                    if ("COMPLETED".equals(o.getStatus())) eveningCompleted++;
                }
            }
        }

        if (morningTotal >= 10 && eveningTotal >= 10) {
            double morningRate = (double) morningCompleted / morningTotal;
            double eveningRate = (double) eveningCompleted / eveningTotal;
            if (morningRate - eveningRate >= 0.20) {
                insights.add(new HabitInsightDto(
                        "morning-vs-evening",
                        "TIME_OF_DAY",
                        "Morning vs Evening Completion",
                        "Evening habits have a lower completion rate (" + Math.round(eveningRate * 100) +
                                "%) than morning habits (" + Math.round(morningRate * 100) + "%). Consider shifting reflective habits earlier.",
                        "NEUTRAL",
                        null
                ));
            }
        }

        return insights;
    }
}

