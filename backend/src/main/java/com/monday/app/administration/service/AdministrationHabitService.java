package com.monday.app.administration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.app.administration.dto.*;
import com.monday.app.administration.entity.*;
import com.monday.app.administration.repository.*;
import com.monday.app.security.SecurityUtils;
import com.monday.app.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
public class AdministrationHabitService {

    private final AdministrationHabitRepository habitRepository;
    private final AdministrationHabitGroupRepository groupRepository;
    private final AdministrationHabitScheduleVersionRepository scheduleVersionRepository;
    private final AdministrationHabitOccurrenceRepository occurrenceRepository;
    private final AdministrationHabitPausePeriodRepository pausePeriodRepository;
    private final AdministrationHabitReminderRepository reminderRepository;
    private final ObjectMapper objectMapper;

    public AdministrationHabitService(
            AdministrationHabitRepository habitRepository,
            AdministrationHabitGroupRepository groupRepository,
            AdministrationHabitScheduleVersionRepository scheduleVersionRepository,
            AdministrationHabitOccurrenceRepository occurrenceRepository,
            AdministrationHabitPausePeriodRepository pausePeriodRepository,
            AdministrationHabitReminderRepository reminderRepository,
            ObjectMapper objectMapper
    ) {
        this.habitRepository = habitRepository;
        this.groupRepository = groupRepository;
        this.scheduleVersionRepository = scheduleVersionRepository;
        this.occurrenceRepository = occurrenceRepository;
        this.pausePeriodRepository = pausePeriodRepository;
        this.reminderRepository = reminderRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<HabitDto> getHabits(String status, String category, UUID groupId, String search) {
        UUID userId = SecurityUtils.getCurrentUserId().orElse(null);
        List<AdministrationHabit> habits = habitRepository.search(userId, status, category, groupId, search);

        // Preload groups for name mapping
        Map<UUID, String> groupNames = loadGroupNames(userId);

        return habits.stream()
                .map(h -> toDto(h, groupNames.get(h.getGroupId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public HabitDto getById(UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId().orElse(null);
        AdministrationHabit habit = habitRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found with id: " + id));

        String groupName = null;
        if (habit.getGroupId() != null) {
            groupName = groupRepository.findById(habit.getGroupId())
                    .map(AdministrationHabitGroup::getName)
                    .orElse(null);
        }

        return toDto(habit, groupName);
    }

    @Transactional
    public HabitDto create(CreateHabitRequest req) {
        UUID userId = SecurityUtils.getCurrentUserId().orElse(null);

        AdministrationHabit h = new AdministrationHabit();
        h.setUserId(userId);
        h.setName(req.name().trim());
        h.setDescription(req.description() != null ? req.description().trim() : null);
        h.setCategory(req.category().trim());
        h.setFrequencyType(req.frequencyType().trim().toUpperCase());
        h.setTargetValue(req.targetValue());
        h.setMinimumTargetValue(req.minimumTargetValue());
        h.setTargetUnit(req.targetUnit() != null ? req.targetUnit().trim() : null);
        h.setPriority(req.priority() != null ? req.priority().trim().toUpperCase() : "MEDIUM");
        h.setStartDate(req.startDate());
        h.setEndDate(req.endDate());
        h.setStatus("ACTIVE");
        h.setDisplayOrder(req.displayOrder() != null ? req.displayOrder() : 0);
        h.setEstimatedDuration(req.estimatedDuration());
        h.setGroupId(req.groupId());

        AdministrationHabit saved = habitRepository.save(h);

        // Create initial effective schedule version
        AdministrationHabitScheduleVersion version = new AdministrationHabitScheduleVersion();
        version.setHabitId(saved.getId());
        version.setScheduleConfig(normalizeScheduleConfig(req.scheduleConfig(), req.frequencyType()));
        version.setTargetValue(req.targetValue());
        version.setMinimumTargetValue(req.minimumTargetValue());
        version.setTargetUnit(req.targetUnit());
        version.setEffectiveFrom(req.startDate());
        scheduleVersionRepository.save(version);

        // Create reminders if provided
        if (req.reminderTimes() != null && !req.reminderTimes().isEmpty()) {
            for (LocalTime time : req.reminderTimes()) {
                AdministrationHabitReminder rem = new AdministrationHabitReminder();
                rem.setHabitId(saved.getId());
                rem.setReminderTime(time);
                rem.setEnabled(true);
                reminderRepository.save(rem);
            }
        }

        String groupName = null;
        if (saved.getGroupId() != null) {
            groupName = groupRepository.findById(saved.getGroupId())
                    .map(AdministrationHabitGroup::getName)
                    .orElse(null);
        }

        return toDto(saved, groupName);
    }

    @Transactional
    public HabitDto update(UUID id, UpdateHabitRequest req) {
        UUID userId = SecurityUtils.getCurrentUserId().orElse(null);
        AdministrationHabit habit = habitRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found with id: " + id));

        boolean scheduleOrTargetChanged = false;

        if (req.name() != null && !req.name().isBlank()) habit.setName(req.name().trim());
        if (req.description() != null) habit.setDescription(req.description().trim());
        if (req.category() != null && !req.category().isBlank()) habit.setCategory(req.category().trim());
        if (req.priority() != null && !req.priority().isBlank()) habit.setPriority(req.priority().trim().toUpperCase());
        if (req.startDate() != null) habit.setStartDate(req.startDate());
        if (req.endDate() != null) habit.setEndDate(req.endDate());
        if (req.groupId() != null) habit.setGroupId(req.groupId());
        if (req.displayOrder() != null) habit.setDisplayOrder(req.displayOrder());
        if (req.estimatedDuration() != null) habit.setEstimatedDuration(req.estimatedDuration());

        // Check if frequency or schedule configuration changed
        if (req.frequencyType() != null && !req.frequencyType().equalsIgnoreCase(habit.getFrequencyType())) {
            habit.setFrequencyType(req.frequencyType().trim().toUpperCase());
            scheduleOrTargetChanged = true;
        }

        if (req.targetValue() != null && !Objects.equals(req.targetValue(), habit.getTargetValue())) {
            habit.setTargetValue(req.targetValue());
            scheduleOrTargetChanged = true;
        }

        if (req.minimumTargetValue() != null && !Objects.equals(req.minimumTargetValue(), habit.getMinimumTargetValue())) {
            habit.setMinimumTargetValue(req.minimumTargetValue());
            scheduleOrTargetChanged = true;
        }

        if (req.targetUnit() != null && !Objects.equals(req.targetUnit(), habit.getTargetUnit())) {
            habit.setTargetUnit(req.targetUnit().trim());
            scheduleOrTargetChanged = true;
        }

        if (req.scheduleConfig() != null) {
            scheduleOrTargetChanged = true;
        }

        AdministrationHabit updated = habitRepository.update(habit);

        // Effective-dated scheduling version update:
        // Do NOT overwrite past versions. Close the prior version and open a new one effective from today.
        if (scheduleOrTargetChanged) {
            LocalDate today = LocalDate.now();
            scheduleVersionRepository.closeActiveVersions(id, today.minusDays(1));

            AdministrationHabitScheduleVersion newVersion = new AdministrationHabitScheduleVersion();
            newVersion.setHabitId(id);
            newVersion.setScheduleConfig(normalizeScheduleConfig(req.scheduleConfig(), updated.getFrequencyType()));
            newVersion.setTargetValue(updated.getTargetValue());
            newVersion.setMinimumTargetValue(updated.getMinimumTargetValue());
            newVersion.setTargetUnit(updated.getTargetUnit());
            newVersion.setEffectiveFrom(today);
            scheduleVersionRepository.save(newVersion);
        }

        // Reminders update
        if (req.reminderTimes() != null) {
            reminderRepository.deleteByHabitId(id);
            for (LocalTime time : req.reminderTimes()) {
                AdministrationHabitReminder rem = new AdministrationHabitReminder();
                rem.setHabitId(id);
                rem.setReminderTime(time);
                rem.setEnabled(true);
                reminderRepository.save(rem);
            }
        }

        String groupName = null;
        if (updated.getGroupId() != null) {
            groupName = groupRepository.findById(updated.getGroupId())
                    .map(AdministrationHabitGroup::getName)
                    .orElse(null);
        }

        return toDto(updated, groupName);
    }

    @Transactional
    public void pause(UUID id, HabitPauseRequest req) {
        UUID userId = SecurityUtils.getCurrentUserId().orElse(null);
        AdministrationHabit habit = habitRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found with id: " + id));

        LocalDate startDate = (req != null && req.startDate() != null) ? req.startDate() : LocalDate.now();
        LocalDate endDate = (req != null) ? req.endDate() : null;
        String reason = (req != null) ? req.reason() : "User paused habit";

        habitRepository.updateStatus(id, "PAUSED");

        AdministrationHabitPausePeriod pause = new AdministrationHabitPausePeriod();
        pause.setHabitId(id);
        pause.setStartDate(startDate);
        pause.setEndDate(endDate);
        pause.setReason(reason);
        pausePeriodRepository.save(pause);
    }

    @Transactional
    public void resume(UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId().orElse(null);
        habitRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found with id: " + id));

        LocalDate today = LocalDate.now();
        habitRepository.updateStatus(id, "ACTIVE");
        pausePeriodRepository.closeOpenPauses(id, today.minusDays(1));
    }

    @Transactional
    public void archive(UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId().orElse(null);
        habitRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found with id: " + id));
        habitRepository.updateStatus(id, "ARCHIVED");
    }

    @Transactional
    public void unarchive(UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId().orElse(null);
        habitRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found with id: " + id));
        habitRepository.updateStatus(id, "ACTIVE");
    }

    @Transactional
    public void delete(UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId().orElse(null);
        habitRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found with id: " + id));
        habitRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<HabitReminderDto> getReminders(UUID habitId) {
        return reminderRepository.findByHabitId(habitId).stream()
                .map(r -> new HabitReminderDto(r.getId(), r.getHabitId(), r.getReminderTime(), r.isEnabled()))
                .toList();
    }

    @Transactional
    public List<HabitReminderDto> saveReminders(UUID habitId, SaveHabitRemindersRequest req) {
        UUID userId = SecurityUtils.getCurrentUserId().orElse(null);
        habitRepository.findByIdAndUserId(habitId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found with id: " + habitId));

        reminderRepository.deleteByHabitId(habitId);
        if (req != null && req.reminderTimes() != null) {
            for (LocalTime time : req.reminderTimes()) {
                AdministrationHabitReminder rem = new AdministrationHabitReminder();
                rem.setHabitId(habitId);
                rem.setReminderTime(time);
                rem.setEnabled(true);
                reminderRepository.save(rem);
            }
        }
        return getReminders(habitId);
    }

    public HabitDto toDto(AdministrationHabit h, String groupName) {
        AdministrationHabitScheduleVersion version = scheduleVersionRepository.findLatestVersion(h.getId()).orElse(null);
        String scheduleConfig = version != null ? version.getScheduleConfig() : null;

        List<HabitReminderDto> reminders = reminderRepository.findByHabitId(h.getId()).stream()
                .map(r -> new HabitReminderDto(r.getId(), r.getHabitId(), r.getReminderTime(), r.isEnabled()))
                .toList();

        // Calculate 30-day completion rate
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(30);
        List<AdministrationHabitOccurrence> occurrences = occurrenceRepository.findByHabitIdAndDateRange(h.getId(), thirtyDaysAgo, today);

        int eligible = 0;
        int completed = 0;
        for (AdministrationHabitOccurrence o : occurrences) {
            if (!"SKIPPED".equals(o.getStatus())) {
                eligible++;
                if ("COMPLETED".equals(o.getStatus())) {
                    completed++;
                }
            }
        }
        double rate30Days = eligible > 0 ? (double) completed / eligible * 100.0 : 0.0;

        // Current & longest streak
        int currentStreak = calculateStreak(h.getId());
        int longestStreak = Math.max(currentStreak, calculateLongestStreak(h.getId()));

        return new HabitDto(
                h.getId(),
                h.getUserId(),
                h.getGroupId(),
                groupName,
                h.getName(),
                h.getDescription(),
                h.getCategory(),
                h.getFrequencyType(),
                scheduleConfig,
                h.getTargetValue(),
                h.getMinimumTargetValue(),
                h.getTargetUnit(),
                h.getPriority(),
                h.getStartDate(),
                h.getEndDate(),
                h.getStatus(),
                h.getDisplayOrder(),
                h.getEstimatedDuration(),
                reminders,
                currentStreak,
                longestStreak,
                Math.round(rate30Days * 10.0) / 10.0,
                h.getCreatedAt(),
                h.getUpdatedAt()
        );
    }

    private int calculateStreak(UUID habitId) {
        LocalDate today = LocalDate.now();
        List<AdministrationHabitOccurrence> recent = occurrenceRepository.findRecentByHabitId(habitId, 60);
        int streak = 0;
        for (AdministrationHabitOccurrence o : recent) {
            if ("COMPLETED".equals(o.getStatus())) {
                streak++;
            } else if ("SKIPPED".equals(o.getStatus())) {
                // Skipped days do not increment streak, but do not break it
                continue;
            } else if (o.getScheduledDate().isBefore(today)) {
                // If past date was missed or pending, streak broke
                break;
            }
        }
        return streak;
    }

    private int calculateLongestStreak(UUID habitId) {
        List<AdministrationHabitOccurrence> all = occurrenceRepository.findRecentByHabitId(habitId, 365);
        int maxStreak = 0;
        int current = 0;
        for (AdministrationHabitOccurrence o : all) {
            if ("COMPLETED".equals(o.getStatus())) {
                current++;
                if (current > maxStreak) maxStreak = current;
            } else if ("SKIPPED".equals(o.getStatus())) {
                continue;
            } else {
                current = 0;
            }
        }
        return maxStreak;
    }

    private Map<UUID, String> loadGroupNames(UUID userId) {
        Map<UUID, String> map = new HashMap<>();
        for (AdministrationHabitGroup g : groupRepository.findAllByUserId(userId)) {
            map.put(g.getId(), g.getName());
        }
        return map;
    }

    private String normalizeScheduleConfig(String scheduleConfig, String frequencyType) {
        if (scheduleConfig != null && !scheduleConfig.isBlank()) {
            return scheduleConfig;
        }
        if ("SELECTED_DAYS".equalsIgnoreCase(frequencyType)) {
            return "{\"type\": \"SELECTED_DAYS\", \"daysOfWeek\": [1, 3, 5]}";
        }
        if ("WEEKLY_TARGET".equalsIgnoreCase(frequencyType)) {
            return "{\"type\": \"WEEKLY_TARGET\", \"targetCount\": 3}";
        }
        if ("INTERVAL_DAYS".equalsIgnoreCase(frequencyType)) {
            return "{\"type\": \"INTERVAL_DAYS\", \"intervalDays\": 3}";
        }
        if ("MONTHLY".equalsIgnoreCase(frequencyType)) {
            return "{\"type\": \"MONTHLY\", \"monthlyType\": \"DAY_OF_MONTH\", \"dayOfMonth\": 1}";
        }
        return "{\"type\": \"DAILY\"}";
    }
}

