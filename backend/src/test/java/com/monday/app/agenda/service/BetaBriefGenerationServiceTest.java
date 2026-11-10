package com.monday.app.agenda.service;

import com.monday.app.agenda.dto.BetaDailyBriefDto;
import com.monday.app.agenda.dto.BetaDailyBriefResponse;
import com.monday.app.agenda.dto.DailyBriefSnapshotDto;
import com.monday.app.agenda.entity.BetaDailyBrief;
import com.monday.app.agenda.repository.BetaDailyBriefRepository;
import com.monday.app.ai.gateway.AiGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BetaBriefGenerationServiceTest {

    private DailyBriefSnapshotService snapshotService;
    private DeterministicBetaBriefGenerator deterministicGenerator;
    private BetaDailyBriefRepository briefRepository;
    private AiGateway aiGateway;
    private BetaBriefGenerationService service;

    @BeforeEach
    void setUp() {
        snapshotService = mock(DailyBriefSnapshotService.class);
        deterministicGenerator = mock(DeterministicBetaBriefGenerator.class);
        briefRepository = mock(BetaDailyBriefRepository.class);
        aiGateway = mock(AiGateway.class);

        service = new BetaBriefGenerationService(
                snapshotService,
                deterministicGenerator,
                briefRepository,
                aiGateway
        );
    }

    @Test
    void getBrief_whenCachedAndFresh_returnsCached() {
        LocalDate date = LocalDate.of(2026, 11, 10);
        String briefingJson = """
                {
                  "todayAtAGlance": {
                    "headline": "Cached Briefing",
                    "summary": "Summary",
                    "urgencyLevel": "NORMAL",
                    "keyFocus": "Focus"
                  },
                  "priorities": [],
                  "schedule": [],
                  "projectsAndTasks": [],
                  "financesAndVehicles": [],
                  "intelligence": [],
                  "nextActions": []
                }
                """;

        BetaDailyBrief entity = new BetaDailyBrief();
        entity.setBriefDate(date);
        entity.setGenerationMode("AI");
        entity.setStatus("GENERATED");
        entity.setBriefingJson(briefingJson);
        entity.setSnapshotJson("{}");
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        when(briefRepository.findByDate(date)).thenReturn(Optional.of(entity));

        BetaDailyBriefResponse response = service.getBrief(date, ZoneId.of("UTC"));

        assertNotNull(response);
        assertEquals(date, response.date());
        assertEquals("GENERATED", response.status());
        assertEquals("Cached Briefing", response.briefing().todayAtAGlance().headline());
        verify(aiGateway, never()).generate(any());
    }

    @Test
    void generateBrief_whenAiFails_usesDeterministicFallback() {
        LocalDate date = LocalDate.of(2026, 11, 10);
        DailyBriefSnapshotDto snapshot = new DailyBriefSnapshotDto(
                date, Instant.now(), "UTC",
                new DailyBriefSnapshotDto.ModulesSnapshot(null, null, null, null, null, null)
        );

        when(snapshotService.buildSnapshot(any(), any())).thenReturn(snapshot);
        when(aiGateway.generate(any())).thenThrow(new RuntimeException("OpenAI connection timeout"));

        BetaDailyBriefDto deterministicBriefing = new BetaDailyBriefDto(
                new BetaDailyBriefDto.TodayAtAGlanceSection("Deterministic Overview", "Summary", "NORMAL", "Execution"),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
        when(deterministicGenerator.generate(snapshot)).thenReturn(deterministicBriefing);

        BetaDailyBriefResponse response = service.generateBrief(date, ZoneId.of("UTC"), true);

        assertNotNull(response);
        assertEquals("DETERMINISTIC", response.generationMode());
        assertEquals("Deterministic Overview", response.briefing().todayAtAGlance().headline());
        verify(briefRepository).save(any());
    }
}

