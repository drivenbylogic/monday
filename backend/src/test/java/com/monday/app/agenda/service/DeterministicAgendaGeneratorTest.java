package com.monday.app.agenda.service;

import com.monday.app.agenda.dto.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DeterministicAgendaGeneratorTest {

    private final DeterministicAgendaGenerator generator = new DeterministicAgendaGenerator();

    @Test
    void generate_producesGroundedAgendaWithTopPrioritiesAndAlerts() {
        LocalDate date = LocalDate.of(2026, 10, 26);
        UUID taskId = UUID.randomUUID();

        TaskSummaryDto overdueTask = new TaskSummaryDto(
                taskId,
                "Critical API Integration",
                "Complete the OpenAPI spec and endpoint tests",
                UUID.randomUUID(),
                "Monday Core",
                null,
                null,
                "IN_PROGRESS",
                "HIGH",
                Instant.parse("2026-10-25T17:00:00Z"),
                90,
                false
        );

        CommitmentSummaryDto commitment = new CommitmentSummaryDto(
                UUID.randomUUID(),
                "American Express Centurion",
                date,
                BigDecimal.valueOf(145000.00),
                null,
                "HDFC Private Banking",
                true,
                "PENDING"
        );

        AgendaContextPayload context = new AgendaContextPayload(
                date,
                "UTC",
                new AgendaContextPayload.ScheduleContext(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList()),
                new AgendaContextPayload.ExecutionContext(List.of(overdueTask), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), new DeepWorkSummaryDto(180, 3.0, 5.0, 2, 95)),
                new AgendaContextPayload.FinancesContext(List.of(commitment), Collections.emptyList(), new CreditUtilizationSummaryDto(BigDecimal.ZERO, BigDecimal.ZERO, null, false), null, Collections.emptyList()),
                new WorkCapacityDto(480, 0, 480, 90, false)
        );

        TodayAgendaResponse response = generator.generate(context);

        assertNotNull(response);
        assertEquals(date, response.date());
        assertEquals("GENERATED", response.status());
        assertFalse(response.recommendations().isEmpty());
        assertTrue(response.recommendations().size() <= 3);

        // First recommendation is grounded in overdue task
        AgendaRecommendationDto rec1 = response.recommendations().get(0);
        assertTrue(rec1.title().contains("Critical API Integration"));
        assertEquals("HIGH", rec1.priority());
        assertEquals(taskId, rec1.sourceReferences().get(0).id());

        // Alerts contain the overdue task alert
        assertFalse(response.importantAlerts().isEmpty());
        assertTrue(response.importantAlerts().stream().anyMatch(a -> a.type().equals("DEADLINE")));

        // Financial heads-up contains commitment
        assertNotNull(response.financialHeadsUp());
        assertTrue(response.financialHeadsUp().impactAmount().compareTo(BigDecimal.ZERO) > 0);

        // Summary markdown is generated
        assertNotNull(response.summaryMarkdown());
        assertTrue(response.summaryMarkdown().contains("Critical API Integration"));
    }
}
