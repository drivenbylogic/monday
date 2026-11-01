package com.monday.app.administration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.app.administration.dto.*;
import com.monday.app.administration.entity.*;
import com.monday.app.administration.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdministrationHabitExecutionServiceTest {

    private AdministrationHabitRepository habitRepository;
    private AdministrationHabitGroupRepository groupRepository;
    private AdministrationHabitScheduleVersionRepository scheduleVersionRepository;
    private AdministrationHabitOccurrenceRepository occurrenceRepository;
    private AdministrationHabitLogRepository logRepository;
    private AdministrationHabitPausePeriodRepository pausePeriodRepository;
    private AdministrationHabitRecurrenceEngine recurrenceEngine;
    private AdministrationHabitExecutionService service;

    @BeforeEach
    void setUp() {
        habitRepository = mock(AdministrationHabitRepository.class);
        groupRepository = mock(AdministrationHabitGroupRepository.class);
        scheduleVersionRepository = mock(AdministrationHabitScheduleVersionRepository.class);
        occurrenceRepository = mock(AdministrationHabitOccurrenceRepository.class);
        logRepository = mock(AdministrationHabitLogRepository.class);
        pausePeriodRepository = mock(AdministrationHabitPausePeriodRepository.class);
        recurrenceEngine = new AdministrationHabitRecurrenceEngine(new ObjectMapper());

        service = new AdministrationHabitExecutionService(
                habitRepository,
                groupRepository,
                scheduleVersionRepository,
                occurrenceRepository,
                logRepository,
                pausePeriodRepository,
                recurrenceEngine
        );
    }

    @Test
    void testCompleteOccurrenceFullTarget() {
        UUID id = UUID.randomUUID();
        UUID habitId = UUID.randomUUID();

        AdministrationHabitOccurrence occ = new AdministrationHabitOccurrence();
        occ.setId(id);
        occ.setHabitId(habitId);
        occ.setScheduledDate(LocalDate.now());
        occ.setTargetValueSnapshot(BigDecimal.valueOf(10));
        occ.setMinimumTargetSnapshot(BigDecimal.valueOf(2));
        occ.setTargetUnitSnapshot("pages");
        occ.setStatus("PENDING");

        AdministrationHabit habit = new AdministrationHabit();
        habit.setId(habitId);
        habit.setName("Reading");
        habit.setCategory("Learning");

        when(occurrenceRepository.findById(id)).thenReturn(Optional.of(occ));
        when(occurrenceRepository.update(any(AdministrationHabitOccurrence.class))).thenAnswer(inv -> inv.getArgument(0));
        when(habitRepository.findById(habitId)).thenReturn(Optional.of(habit));

        CompleteOccurrenceRequest req = new CompleteOccurrenceRequest(BigDecimal.valueOf(12), "Great chapter");
        HabitOccurrenceDto result = service.completeOccurrence(id, req);

        assertNotNull(result);
        assertEquals("COMPLETED", result.status());
        assertEquals(BigDecimal.valueOf(12), result.actualValue());
        assertEquals("Great chapter", result.notes());
        assertNotNull(result.completedAt());
    }

    @Test
    void testPartialOccurrenceMinimumTarget() {
        UUID id = UUID.randomUUID();
        UUID habitId = UUID.randomUUID();

        AdministrationHabitOccurrence occ = new AdministrationHabitOccurrence();
        occ.setId(id);
        occ.setHabitId(habitId);
        occ.setScheduledDate(LocalDate.now());
        occ.setTargetValueSnapshot(BigDecimal.valueOf(10));
        occ.setMinimumTargetSnapshot(BigDecimal.valueOf(2));
        occ.setTargetUnitSnapshot("pages");
        occ.setStatus("PENDING");

        AdministrationHabit habit = new AdministrationHabit();
        habit.setId(habitId);
        habit.setName("Reading");

        when(occurrenceRepository.findById(id)).thenReturn(Optional.of(occ));
        when(occurrenceRepository.update(any(AdministrationHabitOccurrence.class))).thenAnswer(inv -> inv.getArgument(0));
        when(habitRepository.findById(habitId)).thenReturn(Optional.of(habit));

        PartialOccurrenceRequest req = new PartialOccurrenceRequest(BigDecimal.valueOf(3), "Busy day, read 3 pages");
        HabitOccurrenceDto result = service.partialOccurrence(id, req);

        assertNotNull(result);
        assertEquals("PARTIAL", result.status());
        assertEquals(BigDecimal.valueOf(3), result.actualValue());
        assertEquals("Busy day, read 3 pages", result.notes());
    }

    @Test
    void testSkipOccurrence() {
        UUID id = UUID.randomUUID();
        UUID habitId = UUID.randomUUID();

        AdministrationHabitOccurrence occ = new AdministrationHabitOccurrence();
        occ.setId(id);
        occ.setHabitId(habitId);
        occ.setScheduledDate(LocalDate.now());
        occ.setStatus("PENDING");

        AdministrationHabit habit = new AdministrationHabit();
        habit.setId(habitId);
        habit.setName("Running");

        when(occurrenceRepository.findById(id)).thenReturn(Optional.of(occ));
        when(occurrenceRepository.update(any(AdministrationHabitOccurrence.class))).thenAnswer(inv -> inv.getArgument(0));
        when(habitRepository.findById(habitId)).thenReturn(Optional.of(habit));

        SkipOccurrenceRequest req = new SkipOccurrenceRequest("Travel day");
        HabitOccurrenceDto result = service.skipOccurrence(id, req);

        assertNotNull(result);
        assertEquals("SKIPPED", result.status());
        assertEquals("Travel day", result.skipReason());
        assertNull(result.actualValue());
        assertNull(result.completedAt());
    }

    @Test
    void testUndoOccurrence() {
        UUID id = UUID.randomUUID();
        UUID habitId = UUID.randomUUID();

        AdministrationHabitOccurrence occ = new AdministrationHabitOccurrence();
        occ.setId(id);
        occ.setHabitId(habitId);
        occ.setScheduledDate(LocalDate.now());
        occ.setStatus("COMPLETED");
        occ.setActualValue(BigDecimal.valueOf(10));

        AdministrationHabit habit = new AdministrationHabit();
        habit.setId(habitId);
        habit.setName("Meditate");

        when(occurrenceRepository.findById(id)).thenReturn(Optional.of(occ));
        when(occurrenceRepository.update(any(AdministrationHabitOccurrence.class))).thenAnswer(inv -> inv.getArgument(0));
        when(habitRepository.findById(habitId)).thenReturn(Optional.of(habit));

        HabitOccurrenceDto result = service.undoOccurrence(id);

        assertNotNull(result);
        assertEquals("PENDING", result.status());
        assertNull(result.actualValue());
        assertNull(result.completedAt());
    }

    @Test
    void testCreateFlexibleHabitLog() {
        UUID habitId = UUID.randomUUID();
        AdministrationHabit habit = new AdministrationHabit();
        habit.setId(habitId);
        habit.setName("Cardio Session");
        habit.setTargetValue(BigDecimal.valueOf(30));
        habit.setMinimumTargetValue(BigDecimal.valueOf(10));

        when(habitRepository.findById(habitId)).thenReturn(Optional.of(habit));
        when(logRepository.save(any(AdministrationHabitLog.class))).thenAnswer(inv -> {
            AdministrationHabitLog l = inv.getArgument(0);
            l.setId(UUID.randomUUID());
            return l;
        });

        CreateHabitLogRequest req = new CreateHabitLogRequest(
                habitId,
                LocalDate.of(2026, 11, 1),
                BigDecimal.valueOf(35),
                "COMPLETED",
                "Zone 2 Run"
        );

        HabitLogDto log = service.createLog(req);

        assertNotNull(log);
        assertEquals(habitId, log.habitId());
        assertEquals("Cardio Session", log.habitName());
        assertEquals(BigDecimal.valueOf(35), log.actualValue());
        assertEquals("COMPLETED", log.status());
    }
}

