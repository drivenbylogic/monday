package com.monday.app.administration.service;

import com.monday.app.administration.dto.CalendarDayDto;
import com.monday.app.administration.entity.AdministrationHabit;
import com.monday.app.administration.entity.AdministrationHabitOccurrence;
import com.monday.app.administration.repository.AdministrationHabitLogRepository;
import com.monday.app.administration.repository.AdministrationHabitOccurrenceRepository;
import com.monday.app.administration.repository.AdministrationHabitRepository;
import com.monday.app.auth.entity.User;
import com.monday.app.security.MondayUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdministrationHabitAnalyticsServiceTest {

    @Mock
    private AdministrationHabitRepository habitRepository;

    @Mock
    private AdministrationHabitOccurrenceRepository occurrenceRepository;

    @Mock
    private AdministrationHabitLogRepository logRepository;

    @Mock
    private AdministrationHabitExecutionService executionService;

    @Mock
    private AdministrationHabitService habitService;

    @InjectMocks
    private AdministrationHabitAnalyticsService analyticsService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setUsername("current.user");
        user.setEmail("current@example.com");
        user.setPasswordHash("hashed");
        user.setRole("ROLE_USER");

        MondayUserDetails userDetails = new MondayUserDetails(user);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void testCalendarDays_Isolation_IgnoresOtherUsersOccurrences() {
        LocalDate today = LocalDate.of(2026, 11, 1);

        // Current user has 2 habits: Habit 1 and Habit 2
        UUID habit1Id = UUID.randomUUID();
        UUID habit2Id = UUID.randomUUID();

        AdministrationHabit h1 = new AdministrationHabit();
        h1.setId(habit1Id);
        h1.setUserId(userId);
        h1.setName("Test c 1");

        AdministrationHabit h2 = new AdministrationHabit();
        h2.setId(habit2Id);
        h2.setUserId(userId);
        h2.setName("Second");

        when(habitRepository.findAllForUser(userId)).thenReturn(List.of(h1, h2));

        // Occurrences on 2026-11-01:
        // Current user: 1 completed, 1 pending
        AdministrationHabitOccurrence occ1 = new AdministrationHabitOccurrence();
        occ1.setId(UUID.randomUUID());
        occ1.setHabitId(habit1Id);
        occ1.setScheduledDate(today);
        occ1.setStatus("COMPLETED");

        AdministrationHabitOccurrence occ2 = new AdministrationHabitOccurrence();
        occ2.setId(UUID.randomUUID());
        occ2.setHabitId(habit2Id);
        occ2.setScheduledDate(today);
        occ2.setStatus("PENDING");

        // Other user's occurrence on same date (should never be counted in ratio!)
        UUID otherHabitId = UUID.randomUUID();
        AdministrationHabitOccurrence otherOcc = new AdministrationHabitOccurrence();
        otherOcc.setId(UUID.randomUUID());
        otherOcc.setHabitId(otherHabitId);
        otherOcc.setScheduledDate(today);
        otherOcc.setStatus("COMPLETED");

        when(occurrenceRepository.findByUserIdAndDateRange(eq(userId), eq(today), eq(today)))
                .thenReturn(List.of(occ1, occ2, otherOcc)); // Even if repository returns otherOcc, in-memory filter guards it

        List<CalendarDayDto> days = analyticsService.getCalendarDays(today, today);

        assertNotNull(days);
        assertEquals(1, days.size());

        CalendarDayDto day = days.get(0);
        assertEquals(today, day.date());
        assertEquals(2, day.eligibleCount(), "Eligible count must be 2, ignoring other user's habit");
        assertEquals(1, day.completedCount(), "Completed count must be 1");
        assertEquals(0, day.missedCount());
        assertEquals(0, day.skippedCount());
        assertEquals("1/2", day.ratio(), "Ratio must be 1/2 (50%), NOT 2/5 (40%)");
        assertEquals(50.0, day.completionPercentage(), 0.01);
        assertEquals("MODERATE", day.statusLevel());
    }
}
