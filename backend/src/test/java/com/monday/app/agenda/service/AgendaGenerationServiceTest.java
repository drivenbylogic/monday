package com.monday.app.agenda.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.monday.app.agenda.dto.*;
import com.monday.app.ai.gateway.AiGateway;
import com.monday.app.ai.model.AiRequest;
import com.monday.app.ai.model.AiResponse;
import com.monday.app.brief.entity.DailyBrief;
import com.monday.app.brief.repository.DailyBriefRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgendaGenerationServiceTest {

    @Mock
    private AgendaContextService contextService;

    @Mock
    private AgendaValidationService validationService;

    @Mock
    private DeterministicAgendaGenerator deterministicGenerator;

    @Mock
    private DailyBriefRepository dailyBriefRepository;

    @Mock
    private AiGateway aiGateway;

    private AgendaGenerationService service;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final LocalDate testDate = LocalDate.of(2026, 10, 26);
    private final ZoneId zoneId = ZoneId.of("UTC");
    private final Instant baseTime = Instant.parse("2026-10-26T08:00:00Z");

    @BeforeEach
    void setUp() {
        service = new AgendaGenerationService(
                contextService,
                validationService,
                deterministicGenerator,
                dailyBriefRepository,
                aiGateway
        );
        service.setClock(Clock.fixed(baseTime, zoneId));
        service.setMinRefreshIntervalHours(6);
    }

    @Test
    void generateAgenda_whenNoExistingBrief_generatesAndPersists() {
        when(dailyBriefRepository.findByDate(testDate)).thenReturn(Optional.empty());

        AgendaContextPayload context = createEmptyContext(testDate);
        when(contextService.buildContext(eq(testDate), eq(zoneId))).thenReturn(context);

        TodayAgendaResponse generatedBrief = new TodayAgendaResponse(
                testDate, "GENERATED", baseTime, "Fresh summary",
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, false, null
        );
        when(deterministicGenerator.generate(context)).thenReturn(generatedBrief);
        when(validationService.validateRecommendations(any(), any())).thenReturn(Collections.emptyList());
        when(validationService.validateSuggestedBlocks(any(), any())).thenReturn(Collections.emptyList());

        TodayAgendaResponse result = service.generateAgenda(testDate, zoneId, true);

        assertNotNull(result);
        assertEquals("GENERATED", result.status());
        assertEquals("Fresh summary", result.summaryMarkdown());
        verify(dailyBriefRepository, times(1)).save(any(DailyBrief.class));
    }

    @Test
    void generateAgenda_whenExistingBriefLessThan6HoursOld_servesExistingResponseWithoutRegenerating() throws Exception {
        // Existing brief generated at baseTime (08:00:00Z)
        TodayAgendaResponse existingAgenda = new TodayAgendaResponse(
                testDate, "GENERATED", baseTime, "Initial 8 AM briefing",
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, false, null
        );

        DailyBrief existingDbBrief = new DailyBrief();
        existingDbBrief.setId(UUID.randomUUID());
        existingDbBrief.setBriefDate(testDate);
        existingDbBrief.setStatus("GENERATED");
        existingDbBrief.setStructuredAgendaJson(objectMapper.writeValueAsString(existingAgenda));
        existingDbBrief.setCreatedAt(baseTime);
        existingDbBrief.setUpdatedAt(baseTime);

        when(dailyBriefRepository.findByDate(testDate)).thenReturn(Optional.of(existingDbBrief));

        // Advance clock by 3 hours (11:00:00Z) - less than 6 hours
        Instant threeHoursLater = baseTime.plus(Duration.ofHours(3));
        service.setClock(Clock.fixed(threeHoursLater, zoneId));

        // When a refresh occurs (forceRegenerate = true)
        TodayAgendaResponse result = service.generateAgenda(testDate, zoneId, true);

        // Should return existing response without calling AI Gateway or context service
        assertNotNull(result);
        assertEquals("Initial 8 AM briefing", result.summaryMarkdown());
        assertEquals(baseTime, result.generatedAt());

        verify(contextService, never()).buildContext(any(), any());
        verify(aiGateway, never()).generate(any());
        verify(deterministicGenerator, never()).generate(any());
        verify(dailyBriefRepository, never()).update(any());
    }

    @Test
    void generateAgenda_whenExistingBrief6HoursOrOlder_generatesNewResponseAndPersists() throws Exception {
        // Existing brief generated at baseTime (08:00:00Z)
        TodayAgendaResponse existingAgenda = new TodayAgendaResponse(
                testDate, "GENERATED", baseTime, "Old morning briefing",
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, false, null
        );

        DailyBrief existingDbBrief = new DailyBrief();
        existingDbBrief.setId(UUID.randomUUID());
        existingDbBrief.setBriefDate(testDate);
        existingDbBrief.setStatus("GENERATED");
        existingDbBrief.setStructuredAgendaJson(objectMapper.writeValueAsString(existingAgenda));
        existingDbBrief.setCreatedAt(baseTime);
        existingDbBrief.setUpdatedAt(baseTime);

        when(dailyBriefRepository.findByDate(testDate)).thenReturn(Optional.of(existingDbBrief));

        // Advance clock by 6 hours and 15 minutes (14:15:00Z) - >= 6 hours
        Instant sixHoursLater = baseTime.plus(Duration.ofHours(6)).plus(Duration.ofMinutes(15));
        service.setClock(Clock.fixed(sixHoursLater, zoneId));

        AgendaContextPayload context = createEmptyContext(testDate);
        when(contextService.buildContext(eq(testDate), eq(zoneId))).thenReturn(context);

        TodayAgendaResponse freshBrief = new TodayAgendaResponse(
                testDate, "GENERATED", sixHoursLater, "New afternoon briefing",
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, false, null
        );
        when(deterministicGenerator.generate(context)).thenReturn(freshBrief);
        when(validationService.validateRecommendations(any(), any())).thenReturn(Collections.emptyList());
        when(validationService.validateSuggestedBlocks(any(), any())).thenReturn(Collections.emptyList());

        // When refresh occurs
        TodayAgendaResponse result = service.generateAgenda(testDate, zoneId, true);

        assertNotNull(result);
        assertEquals("New afternoon briefing", result.summaryMarkdown());
        assertEquals(sixHoursLater, result.generatedAt());

        // Context, generation, and DB update must have been invoked
        verify(contextService, times(1)).buildContext(eq(testDate), eq(zoneId));
        verify(deterministicGenerator, times(1)).generate(context);
        verify(dailyBriefRepository, times(1)).update(any(DailyBrief.class));
    }

    @Test
    void generateAgenda_whenExistingBriefHasFailedStatus_regeneratesImmediately() throws Exception {
        // Corrupted or failed brief
        DailyBrief failedDbBrief = new DailyBrief();
        failedDbBrief.setId(UUID.randomUUID());
        failedDbBrief.setBriefDate(testDate);
        failedDbBrief.setStatus("FAILED");
        failedDbBrief.setStructuredAgendaJson("{}");
        failedDbBrief.setCreatedAt(baseTime);
        failedDbBrief.setUpdatedAt(baseTime);

        when(dailyBriefRepository.findByDate(testDate)).thenReturn(Optional.of(failedDbBrief));

        AgendaContextPayload context = createEmptyContext(testDate);
        when(contextService.buildContext(eq(testDate), eq(zoneId))).thenReturn(context);

        TodayAgendaResponse freshBrief = new TodayAgendaResponse(
                testDate, "GENERATED", baseTime, "Recovered briefing",
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, false, null
        );
        when(deterministicGenerator.generate(context)).thenReturn(freshBrief);
        when(validationService.validateRecommendations(any(), any())).thenReturn(Collections.emptyList());
        when(validationService.validateSuggestedBlocks(any(), any())).thenReturn(Collections.emptyList());

        TodayAgendaResponse result = service.generateAgenda(testDate, zoneId, true);

        assertNotNull(result);
        assertEquals("Recovered briefing", result.summaryMarkdown());
        verify(contextService, times(1)).buildContext(eq(testDate), eq(zoneId));
    }

    private AgendaContextPayload createEmptyContext(LocalDate date) {
        return new AgendaContextPayload(
                date,
                "UTC",
                new AgendaContextPayload.ScheduleContext(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList()),
                new AgendaContextPayload.ExecutionContext(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null),
                new AgendaContextPayload.FinancesContext(Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()),
                new WorkCapacityDto(480, 0, 480, 0, false)
        );
    }
}
