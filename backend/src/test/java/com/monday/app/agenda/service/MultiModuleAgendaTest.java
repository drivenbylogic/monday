package com.monday.app.agenda.service;

import com.monday.app.administration.repository.AdministrationImportantDateRepository;
import com.monday.app.administration.repository.AdministrationObligationRepository;
import com.monday.app.agenda.dto.*;
import com.monday.app.event.repository.EventRepository;
import com.monday.app.intelligence.repository.ArticleRepository;
import com.monday.app.project.repository.MilestoneRepository;
import com.monday.app.project.repository.ProjectRepository;
import com.monday.app.project.repository.TaskRepository;
import com.monday.app.vehicle.entity.Vehicle;
import com.monday.app.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MultiModuleAgendaTest {

    private DeterministicAgendaGenerator generator;
    private AgendaValidationService validationService;

    private TaskRepository taskRepository;
    private ProjectRepository projectRepository;
    private MilestoneRepository milestoneRepository;
    private EventRepository eventRepository;
    private VehicleRepository vehicleRepository;
    private AdministrationObligationRepository adminObligationRepository;
    private AdministrationImportantDateRepository adminDateRepository;
    private ArticleRepository articleRepository;
    private NamedParameterJdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        generator = new DeterministicAgendaGenerator();

        taskRepository = mock(TaskRepository.class);
        projectRepository = mock(ProjectRepository.class);
        milestoneRepository = mock(MilestoneRepository.class);
        eventRepository = mock(EventRepository.class);
        vehicleRepository = mock(VehicleRepository.class);
        adminObligationRepository = mock(AdministrationObligationRepository.class);
        adminDateRepository = mock(AdministrationImportantDateRepository.class);
        articleRepository = mock(ArticleRepository.class);
        jdbc = mock(NamedParameterJdbcTemplate.class);

        validationService = new AgendaValidationService(
                taskRepository,
                projectRepository,
                milestoneRepository,
                eventRepository,
                vehicleRepository,
                adminObligationRepository,
                adminDateRepository,
                articleRepository,
                jdbc
        );
    }

    @Test
    void testDeterministicAgendaGenerator_includesAllSevenModules() {
        LocalDate date = LocalDate.of(2026, 10, 29);

        // Vehicles context
        UUID vId = UUID.randomUUID();
        UUID maintId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        AgendaContextPayload.VehiclesContext vehicles = new AgendaContextPayload.VehiclesContext(
                1,
                "Tesla Model 3",
                45000,
                List.of(new VehicleAgendaSummaryDto.VehicleMaintenanceItemDto(maintId, vId, "Tesla Model 3", "Brake Pad Inspection", date.minusDays(2), 44000, true)),
                List.of(new VehicleAgendaSummaryDto.VehicleDocExpiryDto(docId, vId, "Tesla Model 3", "INSURANCE", "Comprehensive Policy", date.plusDays(5), 5, false)),
                Collections.emptyList(),
                Collections.emptyList()
        );

        // Administration context
        UUID obId = UUID.randomUUID();
        UUID dateId = UUID.randomUUID();
        AgendaContextPayload.AdministrationContext administration = new AgendaContextPayload.AdministrationContext(
                1,
                1,
                1,
                List.of(new AdministrationAgendaSummaryDto.AdminObligationSummaryDto(obId, "Cloud Server Renewal", "SUBSCRIPTION", "Tech", date.minusDays(1), BigDecimal.valueOf(120), "USD", true)),
                List.of(new AdministrationAgendaSummaryDto.AdminImportantDateSummaryDto(dateId, "Mom's Birthday", date, "BIRTHDAY", "Mom", 0)),
                Collections.emptyList(),
                Collections.emptyList(),
                2
        );

        // Career context
        AgendaContextPayload.CareerContext career = new AgendaContextPayload.CareerContext(
                true,
                "Staff AI Architect Roadmap",
                "Career plan for 2026",
                date.plusDays(60),
                60L,
                70.0,
                List.of(new CareerAgendaSummaryDto.CareerMilestoneSummaryDto("m-1", "Complete LLM Systems Spec", date.plusDays(10), "P1", 75.0, 4)),
                List.of(new CareerAgendaSummaryDto.CareerTaskSummaryDto("t-1", "m-1", "Spec milestone", "Draft architecture diagram", "P1", date, false))
        );

        // Intelligence context
        UUID artId = UUID.randomUUID();
        AgendaContextPayload.IntelligenceContext intelligence = new AgendaContextPayload.IntelligenceContext(
                14,
                5,
                List.of(new IntelligenceAgendaSummaryDto.IntelligenceArticleSummaryDto(artId, "Advancements in Autonomous Coding", "AI Weekly", "Engineering", java.time.OffsetDateTime.now(), "Summary of breakthroughs in autonomous workflows.", false))
        );

        // Basic execution & schedule
        AgendaContextPayload.ScheduleContext schedule = new AgendaContextPayload.ScheduleContext(
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList()
        );
        AgendaContextPayload.ExecutionContext execution = new AgendaContextPayload.ExecutionContext(
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null
        );
        AgendaContextPayload.FinancesContext finances = new AgendaContextPayload.FinancesContext(
                Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );
        WorkCapacityDto capacity = new WorkCapacityDto(480, 0, 480, 60, false);

        AgendaContextPayload payload = new AgendaContextPayload(
                date, "UTC", schedule, execution, finances, capacity,
                vehicles, administration, career, intelligence
        );

        // Run generator
        TodayAgendaResponse response = generator.generate(payload);

        assertNotNull(response);
        assertNotNull(response.summaryMarkdown());
        assertTrue(response.summaryMarkdown().contains("Mobility Readiness"), "Markdown should have Mobility Readiness (Vehicles)");
        assertTrue(response.summaryMarkdown().contains("Administration & Household"), "Markdown should have Administration & Household");
        assertTrue(response.summaryMarkdown().contains("Career Trajectory"), "Markdown should have Career Trajectory");
        assertTrue(response.summaryMarkdown().contains("Industry Intelligence"), "Markdown should have Industry Intelligence");

        // Verify alerts generated for vehicle overdue and admin overdue
        assertNotNull(response.importantAlerts());
        assertTrue(response.importantAlerts().stream().anyMatch(a -> a.type().equals("VEHICLE")), "Should have vehicle alert");
        assertTrue(response.importantAlerts().stream().anyMatch(a -> a.type().equals("ADMINISTRATIVE")), "Should have administrative alert");

        // Verify execution insights include career progress
        assertNotNull(response.executionInsight());
        assertTrue(response.executionInsight().message().toLowerCase().contains("career roadmap") || response.executionInsight().title().toLowerCase().contains("career"), "Should have career roadmap insight");
    }

    @Test
    void testAgendaValidationService_validatesNewModuleSourceReferences() {
        UUID vId = UUID.randomUUID();
        UUID obId = UUID.randomUUID();

        Vehicle vehicle = new Vehicle();
        vehicle.setId(vId);
        vehicle.setName("Tesla Model 3");
        vehicle.setStatus("ACTIVE");
        when(vehicleRepository.findById(vId)).thenReturn(Optional.of(vehicle));

        AgendaRecommendationDto rec = new AgendaRecommendationDto(
                "rec-1",
                "Service Tesla Brakes",
                "Take car to service center",
                "HIGH",
                "Safety critical",
                "Book appointment",
                List.of(
                        new SourceReferenceDto("VEHICLE", vId, "Tesla"),
                        new SourceReferenceDto("ADMIN_OBLIGATION", obId, "Fake Obligation") // Not mocked, should be filtered if repo returns empty
                ),
                60,
                null,
                null,
                0.95,
                null
        );

        when(adminObligationRepository.findById(obId)).thenReturn(Optional.empty());

        List<AgendaRecommendationDto> validated = validationService.validateRecommendations(List.of(rec), Collections.emptyList());

        assertEquals(1, validated.size());
        AgendaRecommendationDto validatedRec = validated.get(0);
        // VEHICLE should be preserved, fabricated ADMIN_OBLIGATION dropped
        assertEquals(1, validatedRec.sourceReferences().size());
        assertEquals("VEHICLE", validatedRec.sourceReferences().get(0).type());
        assertEquals(vId, validatedRec.sourceReferences().get(0).id());
    }
}
