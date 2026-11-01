package com.monday.app.administration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.app.administration.dto.*;
import com.monday.app.administration.entity.*;
import com.monday.app.administration.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdministrationHabitServiceTest {

    private AdministrationHabitRepository habitRepository;
    private AdministrationHabitGroupRepository groupRepository;
    private AdministrationHabitScheduleVersionRepository scheduleVersionRepository;
    private AdministrationHabitOccurrenceRepository occurrenceRepository;
    private AdministrationHabitPausePeriodRepository pausePeriodRepository;
    private AdministrationHabitReminderRepository reminderRepository;
    private AdministrationHabitService service;

    @BeforeEach
    void setUp() {
        habitRepository = mock(AdministrationHabitRepository.class);
        groupRepository = mock(AdministrationHabitGroupRepository.class);
        scheduleVersionRepository = mock(AdministrationHabitScheduleVersionRepository.class);
        occurrenceRepository = mock(AdministrationHabitOccurrenceRepository.class);
        pausePeriodRepository = mock(AdministrationHabitPausePeriodRepository.class);
        reminderRepository = mock(AdministrationHabitReminderRepository.class);

        service = new AdministrationHabitService(
                habitRepository,
                groupRepository,
                scheduleVersionRepository,
                occurrenceRepository,
                pausePeriodRepository,
                reminderRepository,
                new ObjectMapper()
        );
    }

    @Test
    void testCreateHabit() {
        CreateHabitRequest req = new CreateHabitRequest(
                "Daily Meditation",
                "10 min breathwork",
                "Mindfulness",
                "DAILY",
                "{\"type\": \"DAILY\"}",
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(2),
                "minutes",
                "HIGH",
                LocalDate.of(2026, 10, 1),
                null,
                null,
                1,
                10,
                List.of(LocalTime.of(7, 30))
        );

        when(habitRepository.save(any(AdministrationHabit.class))).thenAnswer(inv -> {
            AdministrationHabit h = inv.getArgument(0);
            h.setId(UUID.randomUUID());
            return h;
        });

        HabitDto created = service.create(req);

        assertNotNull(created);
        assertEquals("Daily Meditation", created.name());
        assertEquals("Mindfulness", created.category());
        assertEquals("DAILY", created.frequencyType());
        assertEquals(BigDecimal.valueOf(10), created.targetValue());
        assertEquals(BigDecimal.valueOf(2), created.minimumTargetValue());

        verify(habitRepository).save(any(AdministrationHabit.class));
        verify(scheduleVersionRepository).save(any(AdministrationHabitScheduleVersion.class));
        verify(reminderRepository).save(any(AdministrationHabitReminder.class));
    }

    @Test
    void testUpdateHabitCreatesNewEffectiveDatedVersion() {
        UUID id = UUID.randomUUID();
        AdministrationHabit existing = new AdministrationHabit();
        existing.setId(id);
        existing.setName("Workout");
        existing.setFrequencyType("DAILY");
        existing.setTargetValue(BigDecimal.valueOf(30));
        existing.setTargetUnit("minutes");
        existing.setStartDate(LocalDate.of(2026, 9, 1));
        existing.setStatus("ACTIVE");

        when(habitRepository.findByIdAndUserId(eq(id), any())).thenReturn(Optional.of(existing));
        when(habitRepository.update(any(AdministrationHabit.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateHabitRequest req = new UpdateHabitRequest(
                "Workout Harder",
                null,
                null,
                "SELECTED_DAYS",
                "{\"type\": \"SELECTED_DAYS\", \"daysOfWeek\": [1, 3, 5]}",
                BigDecimal.valueOf(45), // target increased
                null,
                "minutes",
                "HIGH",
                null,
                null,
                null,
                null,
                null,
                null
        );

        HabitDto updated = service.update(id, req);

        assertNotNull(updated);
        assertEquals("Workout Harder", updated.name());
        assertEquals(BigDecimal.valueOf(45), updated.targetValue());

        // Verifies that prior versions are closed and a new version is created
        verify(scheduleVersionRepository).closeActiveVersions(eq(id), any(LocalDate.class));
        verify(scheduleVersionRepository).save(any(AdministrationHabitScheduleVersion.class));
    }

    @Test
    void testPauseAndResumeHabit() {
        UUID id = UUID.randomUUID();
        AdministrationHabit habit = new AdministrationHabit();
        habit.setId(id);
        habit.setName("Reading");
        habit.setStatus("ACTIVE");

        when(habitRepository.findByIdAndUserId(eq(id), any())).thenReturn(Optional.of(habit));

        HabitPauseRequest pauseReq = new HabitPauseRequest(
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 7),
                "Vacation in Kyoto"
        );

        service.pause(id, pauseReq);

        verify(habitRepository).updateStatus(id, "PAUSED");
        verify(pausePeriodRepository).save(argThat(p ->
                p.getHabitId().equals(id) &&
                p.getStartDate().equals(LocalDate.of(2026, 11, 1)) &&
                "Vacation in Kyoto".equals(p.getReason())
        ));

        service.resume(id);

        verify(habitRepository).updateStatus(id, "ACTIVE");
        verify(pausePeriodRepository).closeOpenPauses(eq(id), any(LocalDate.class));
    }

    @Test
    void testArchiveAndUnarchiveHabit() {
        UUID id = UUID.randomUUID();
        AdministrationHabit habit = new AdministrationHabit();
        habit.setId(id);
        habit.setStatus("ACTIVE");

        when(habitRepository.findByIdAndUserId(eq(id), any())).thenReturn(Optional.of(habit));

        service.archive(id);
        verify(habitRepository).updateStatus(id, "ARCHIVED");

        service.unarchive(id);
        verify(habitRepository).updateStatus(id, "ACTIVE");
    }
}

