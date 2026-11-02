package com.monday.app.administration.service;

import com.monday.app.administration.dto.*;
import com.monday.app.administration.entity.AdministrationHabit;
import com.monday.app.administration.entity.AdministrationHabitGroup;
import com.monday.app.administration.entity.AdministrationHabitOccurrence;
import com.monday.app.administration.repository.AdministrationHabitGroupRepository;
import com.monday.app.administration.repository.AdministrationHabitOccurrenceRepository;
import com.monday.app.administration.repository.AdministrationHabitRepository;
import com.monday.app.security.SecurityUtils;
import com.monday.app.shared.cache.CacheInvalidationService;
import com.monday.app.shared.cache.CacheNames;
import com.monday.app.shared.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdministrationHabitGroupService {

    private final AdministrationHabitGroupRepository groupRepository;
    private final AdministrationHabitRepository habitRepository;
    private final AdministrationHabitOccurrenceRepository occurrenceRepository;
    private final AdministrationHabitService habitService;
    private final AdministrationHabitExecutionService executionService;
    private final CacheInvalidationService cacheInvalidationService;

    public AdministrationHabitGroupService(
            AdministrationHabitGroupRepository groupRepository,
            AdministrationHabitRepository habitRepository,
            AdministrationHabitOccurrenceRepository occurrenceRepository,
            AdministrationHabitService habitService,
            AdministrationHabitExecutionService executionService
    ) {
        this(groupRepository, habitRepository, occurrenceRepository, habitService, executionService, null);
    }

    @Autowired
    public AdministrationHabitGroupService(
            AdministrationHabitGroupRepository groupRepository,
            AdministrationHabitRepository habitRepository,
            AdministrationHabitOccurrenceRepository occurrenceRepository,
            AdministrationHabitService habitService,
            AdministrationHabitExecutionService executionService,
            @Nullable CacheInvalidationService cacheInvalidationService
    ) {
        this.groupRepository = groupRepository;
        this.habitRepository = habitRepository;
        this.occurrenceRepository = occurrenceRepository;
        this.habitService = habitService;
        this.executionService = executionService;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    private void evictHabitCaches() {
        if (cacheInvalidationService != null) {
            cacheInvalidationService.evictAllAfterCommit(
                    CacheNames.HABIT_ANALYTICS,
                    CacheNames.HABIT_REPORTS,
                    CacheNames.HABIT_TODAY,
                    CacheNames.HABIT_WEEK,
                    CacheNames.HOME_DASHBOARD
            );
        }
    }

    @Transactional(readOnly = true)
    public List<HabitGroupDto> getGroups() {
        UUID userId = SecurityUtils.getCurrentUserId().orElse(null);
        List<AdministrationHabitGroup> groups = groupRepository.findAllByUserId(userId);
        if (groups.isEmpty()) return Collections.emptyList();

        List<HabitDto> allHabitsDtos = habitService.getHabits(null, null, null, null);
        Map<UUID, List<HabitDto>> habitsByGroup = allHabitsDtos.stream()
                .filter(h -> h.groupId() != null)
                .collect(Collectors.groupingBy(HabitDto::groupId));

        List<HabitGroupDto> result = new ArrayList<>();
        for (AdministrationHabitGroup g : groups) {
            List<HabitDto> groupHabits = habitsByGroup.getOrDefault(g.getId(), Collections.emptyList());

            result.add(new HabitGroupDto(
                    g.getId(),
                    g.getUserId(),
                    g.getName(),
                    g.getDescription(),
                    g.getDisplayOrder(),
                    groupHabits.size(),
                    groupHabits,
                    g.getCreatedAt(),
                    g.getUpdatedAt()
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public HabitGroupDto getGroupById(UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId().orElse(null);
        AdministrationHabitGroup g = groupRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Habit group not found with id: " + id));

        List<HabitDto> groupHabits = habitService.getHabits(null, null, id, null);

        return new HabitGroupDto(
                g.getId(),
                g.getUserId(),
                g.getName(),
                g.getDescription(),
                g.getDisplayOrder(),
                groupHabits.size(),
                groupHabits,
                g.getCreatedAt(),
                g.getUpdatedAt()
        );
    }

    @Transactional
    public HabitGroupDto createGroup(CreateHabitGroupRequest req) {
        UUID userId = SecurityUtils.getCurrentUserId().orElse(null);

        AdministrationHabitGroup g = new AdministrationHabitGroup();
        g.setUserId(userId);
        g.setName(req.name().trim());
        g.setDescription(req.description() != null ? req.description().trim() : null);
        g.setDisplayOrder(req.displayOrder() != null ? req.displayOrder() : 0);

        AdministrationHabitGroup saved = groupRepository.save(g);

        // Associate habits if specified
        if (req.habitIds() != null) {
            for (UUID habitId : req.habitIds()) {
                habitRepository.findById(habitId).ifPresent(h -> {
                    h.setGroupId(saved.getId());
                    habitRepository.update(h);
                });
            }
        }

        evictHabitCaches();
        return getGroupById(saved.getId());
    }

    @Transactional
    public HabitGroupDto updateGroup(UUID id, UpdateHabitGroupRequest req) {
        UUID userId = SecurityUtils.getCurrentUserId().orElse(null);
        AdministrationHabitGroup g = groupRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Habit group not found with id: " + id));

        if (req.name() != null && !req.name().isBlank()) g.setName(req.name().trim());
        if (req.description() != null) g.setDescription(req.description().trim());
        if (req.displayOrder() != null) g.setDisplayOrder(req.displayOrder());

        groupRepository.update(g);

        if (req.habitIds() != null) {
            // Detach habits that were removed
            List<AdministrationHabit> current = habitRepository.search(userId, null, null, id, null);
            for (AdministrationHabit h : current) {
                if (!req.habitIds().contains(h.getId())) {
                    h.setGroupId(null);
                    habitRepository.update(h);
                }
            }
            // Attach specified habits
            for (UUID habitId : req.habitIds()) {
                habitRepository.findById(habitId).ifPresent(h -> {
                    h.setGroupId(id);
                    habitRepository.update(h);
                });
            }
        }

        evictHabitCaches();
        return getGroupById(id);
    }

    @Transactional
    public void deleteGroup(UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId().orElse(null);
        groupRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Habit group not found with id: " + id));

        // Detach associated habits
        List<AdministrationHabit> habits = habitRepository.search(userId, null, null, id, null);
        for (AdministrationHabit h : habits) {
            h.setGroupId(null);
            habitRepository.update(h);
        }

        groupRepository.deleteById(id);
        evictHabitCaches();
    }

    /**
     * Executes all scheduled occurrences belonging to this group routine for a date sequentially.
     */
    @Transactional
    public List<HabitOccurrenceDto> executeGroupRoutine(UUID groupId, LocalDate date) {
        if (date == null) date = LocalDate.now();
        UUID userId = SecurityUtils.getCurrentUserId().orElse(null);

        executionService.ensureOccurrencesForDateRange(userId, date, date);

        List<AdministrationHabit> habits = habitRepository.search(userId, "ACTIVE", null, groupId, null);
        List<HabitOccurrenceDto> results = new ArrayList<>();

        for (AdministrationHabit h : habits) {
            occurrenceRepository.findByHabitIdAndScheduledDate(h.getId(), date).ifPresent(occ -> {
                if (!"COMPLETED".equals(occ.getStatus())) {
                    results.add(executionService.completeOccurrence(occ.getId(), new CompleteOccurrenceRequest(null, "Completed via routine execution")));
                }
            });
        }
        evictHabitCaches();
        return results;
    }
}

