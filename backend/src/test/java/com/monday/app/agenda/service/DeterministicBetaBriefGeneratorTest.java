package com.monday.app.agenda.service;

import com.monday.app.agenda.dto.BetaDailyBriefDto;
import com.monday.app.agenda.dto.DailyBriefSnapshotDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeterministicBetaBriefGeneratorTest {

    private final DeterministicBetaBriefGenerator generator = new DeterministicBetaBriefGenerator();

    @Test
    void generate_withFullSnapshot_producesAllSevenSections() {
        LocalDate date = LocalDate.of(2026, 11, 10);

        DailyBriefSnapshotDto.EventBriefItem event = new DailyBriefSnapshotDto.EventBriefItem(
                "evt-1", "Executive Sync", "09:30", "10:30", false, "Boardroom A", false, "Review slides"
        );
        DailyBriefSnapshotDto.CalendarModule calendar = new DailyBriefSnapshotDto.CalendarModule(
                "AVAILABLE", List.of(event), Collections.emptyList()
        );

        DailyBriefSnapshotDto.ActiveProjectBriefItem project = new DailyBriefSnapshotDto.ActiveProjectBriefItem(
                "proj-1", "AI Core Engine", "IN_PROGRESS", "HIGH", 78.5, "Engineering"
        );
        DailyBriefSnapshotDto.UpcomingMilestoneBriefItem milestone = new DailyBriefSnapshotDto.UpcomingMilestoneBriefItem(
                "ms-1", "proj-1", "AI Core Engine", "v2.0 Beta Release", date.plusDays(3), "OPEN"
        );
        DailyBriefSnapshotDto.BlockedBriefItem blocked = new DailyBriefSnapshotDto.BlockedBriefItem(
                "blk-1", "TASK", "Staging cluster provisioning", "AI Core Engine", "Awaiting cloud quota"
        );
        DailyBriefSnapshotDto.ProjectsModule projects = new DailyBriefSnapshotDto.ProjectsModule(
                "AVAILABLE", List.of(project), List.of(milestone), List.of(blocked)
        );

        DailyBriefSnapshotDto.TaskBriefItem overdueTask = new DailyBriefSnapshotDto.TaskBriefItem(
                "task-1", "Audit IAM Roles", "Security", date.minusDays(2), "HIGH", 60, 2L, "IN_PROGRESS"
        );
        DailyBriefSnapshotDto.TaskBriefItem dueTodayTask = new DailyBriefSnapshotDto.TaskBriefItem(
                "task-2", "Submit Release Candidate", "AI Core Engine", date, "HIGH", 45, 0L, "IN_PROGRESS"
        );
        DailyBriefSnapshotDto.TasksModule tasks = new DailyBriefSnapshotDto.TasksModule(
                "AVAILABLE", List.of(overdueTask), List.of(dueTodayTask), Collections.emptyList()
        );

        DailyBriefSnapshotDto.FinancialObligationBriefItem finance = new DailyBriefSnapshotDto.FinancialObligationBriefItem(
                "fin-1", "AWS Cloud Services", "$420.00", date, false, "Infrastructure", "URGENT"
        );
        DailyBriefSnapshotDto.FinancesModule finances = new DailyBriefSnapshotDto.FinancesModule(
                "AVAILABLE", Map.of("totalDueSoonAmount", 420.00), List.of(finance), Collections.emptyList()
        );

        DailyBriefSnapshotDto.VehicleMaintenanceBriefItem maintenance = new DailyBriefSnapshotDto.VehicleMaintenanceBriefItem(
                "veh-1", "BMW M3", "Brake Pad Inspection", date.plusDays(5), 45000, false
        );
        DailyBriefSnapshotDto.VehiclesModule vehicles = new DailyBriefSnapshotDto.VehiclesModule(
                "AVAILABLE", List.of(maintenance), Collections.emptyList(), Collections.emptyList()
        );

        DailyBriefSnapshotDto.IntelligenceArticleBriefItem article = new DailyBriefSnapshotDto.IntelligenceArticleBriefItem(
                "art-1", "Advances in Agentic Architectures", "AI Digest", "https://arxiv.org/abs/2601",
                "ARTIFICIAL_INTELLIGENCE", Instant.now(), "Breakthroughs in grounded enterprise agents."
        );
        DailyBriefSnapshotDto.IntelligenceModule intelligence = new DailyBriefSnapshotDto.IntelligenceModule(
                "AVAILABLE", List.of(article)
        );

        DailyBriefSnapshotDto snapshot = new DailyBriefSnapshotDto(
                date, Instant.now(), "UTC",
                new DailyBriefSnapshotDto.ModulesSnapshot(calendar, projects, tasks, finances, vehicles, intelligence)
        );

        BetaDailyBriefDto briefing = generator.generate(snapshot);

        assertNotNull(briefing);

        // 1. TODAY AT A GLANCE
        assertNotNull(briefing.todayAtAGlance());
        assertNotNull(briefing.todayAtAGlance().headline());
        assertNotNull(briefing.todayAtAGlance().summary());
        assertEquals("CRITICAL", briefing.todayAtAGlance().urgencyLevel()); // Because overdue task exists
        assertNotNull(briefing.todayAtAGlance().keyFocus());

        // 2. PRIORITIES
        assertFalse(briefing.priorities().isEmpty());
        assertTrue(briefing.priorities().stream().anyMatch(p -> p.title().contains("Audit IAM Roles")));

        // 3. SCHEDULE
        assertFalse(briefing.schedule().isEmpty());
        assertEquals("Executive Sync", briefing.schedule().get(0).title());
        assertEquals("09:30", briefing.schedule().get(0).time());

        // 4. PROJECTS AND TASKS
        assertFalse(briefing.projectsAndTasks().isEmpty());
        assertTrue(briefing.projectsAndTasks().stream().anyMatch(pt -> pt.itemType().equals("MILESTONE")));

        // 5. FINANCES AND VEHICLES
        assertFalse(briefing.financesAndVehicles().isEmpty());
        assertTrue(briefing.financesAndVehicles().stream().anyMatch(fv -> fv.type().equals("FINANCE")));
        assertTrue(briefing.financesAndVehicles().stream().anyMatch(fv -> fv.type().equals("VEHICLE")));

        // 6. INTELLIGENCE
        assertFalse(briefing.intelligence().isEmpty());
        assertEquals("Advances in Agentic Architectures", briefing.intelligence().get(0).title());
        assertEquals("https://arxiv.org/abs/2601", briefing.intelligence().get(0).sourceUrl());

        // 7. NEXT ACTIONS
        assertFalse(briefing.nextActions().isEmpty());
        assertTrue(briefing.nextActions().size() <= 3);
    }

    @Test
    void generate_withNullSnapshot_handlesGracefully() {
        BetaDailyBriefDto briefing = generator.generate(null);
        assertNotNull(briefing);
        assertNotNull(briefing.todayAtAGlance());
        assertNotNull(briefing.nextActions());
    }
}

