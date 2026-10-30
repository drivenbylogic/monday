package com.monday.app.intelligence.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.app.intelligence.dto.ArticleFeedItemDto;
import com.monday.app.intelligence.dto.DashboardStatsDto;
import com.monday.app.intelligence.dto.IntelligenceStatusDto;
import com.monday.app.intelligence.repository.ArticleSummaryRepository;
import com.monday.app.intelligence.repository.DailyArticleSelectionRepository;
import com.monday.app.intelligence.repository.IntelligenceRunRepository;
import com.monday.app.intelligence.repository.InterestCategoryRepository;
import com.monday.app.intelligence.service.ArticleFeedService;
import com.monday.app.intelligence.service.DashboardStatsService;
import com.monday.app.intelligence.service.IntelligenceCoordinator;
import com.monday.app.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class IntelligenceControllerTest {

    private MockMvc mockMvc;
    private IntelligenceCoordinator coordinator;
    private ArticleFeedService articleFeedService;
    private DashboardStatsService statsService;
    private IntelligenceRunRepository runRepository;
    private DailyArticleSelectionRepository selectionRepository;
    private ArticleSummaryRepository summaryRepository;
    private InterestCategoryRepository categoryRepository;
    private NamedParameterJdbcTemplate jdbc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        coordinator = mock(IntelligenceCoordinator.class);
        articleFeedService = mock(ArticleFeedService.class);
        statsService = mock(DashboardStatsService.class);
        runRepository = mock(IntelligenceRunRepository.class);
        selectionRepository = mock(DailyArticleSelectionRepository.class);
        summaryRepository = mock(ArticleSummaryRepository.class);
        categoryRepository = mock(InterestCategoryRepository.class);
        jdbc = mock(NamedParameterJdbcTemplate.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();

        IntelligenceController controller = new IntelligenceController(
                coordinator,
                articleFeedService,
                statsService,
                runRepository,
                selectionRepository,
                summaryRepository,
                categoryRepository,
                jdbc
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getStatus_returnsCooldownAndRunStatus() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();
        IntelligenceStatusDto statusDto = new IntelligenceStatusDto(
                true,
                false,
                now.minusHours(7),
                now,
                0,
                5,
                25,
                7,
                "COMPLETED",
                1200L,
                450,
                BigDecimal.valueOf(0.000135)
        );

        when(coordinator.getStatus()).thenReturn(statusDto);

        mockMvc.perform(get("/api/v1/intelligence/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canGenerate").value(true))
                .andExpect(jsonPath("$.isRunning").value(false))
                .andExpect(jsonPath("$.activeNodesCount").value(5))
                .andExpect(jsonPath("$.totalCachedArticlesCount").value(25))
                .andExpect(jsonPath("$.retentionDays").value(7))
                .andExpect(jsonPath("$.lastRunStatus").value("COMPLETED"));
    }

    @Test
    void refreshIntelligence_triggersPipelineExecution() throws Exception {
        IntelligenceCoordinator.ExecutionOutcome outcome = new IntelligenceCoordinator.ExecutionOutcome(
                true,
                "COMPLETED",
                "Pipeline completed successfully",
                null,
                21600,
                OffsetDateTime.now().plusHours(6)
        );

        when(coordinator.triggerManualGeneration()).thenReturn(outcome);

        mockMvc.perform(post("/api/v1/intelligence/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runExecuted").value(true))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void getArticles_returnsPaginatedFeed() throws Exception {
        UUID articleId = UUID.randomUUID();
        ArticleFeedItemDto item = new ArticleFeedItemDto(
                articleId,
                "Autonomous Agents Scaled Up",
                "Concise AI generated summary preserving all factual details.",
                "https://tech.org/post/1",
                "https://tech.org/post/1",
                "Tech Author",
                "Hacker News",
                UUID.randomUUID(),
                "ARTIFICIAL_INTELLIGENCE",
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                "gpt-4o-mini",
                140,
                250,
                BigDecimal.valueOf(0.000075),
                false,
                false,
                "SUMMARIZED"
        );

        when(articleFeedService.getArticles(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/intelligence/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(articleId.toString()))
                .andExpect(jsonPath("$.content[0].title").value("Autonomous Agents Scaled Up"))
                .andExpect(jsonPath("$.content[0].sourceName").value("Hacker News"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getDashboardStats_returnsMetricsAndCharts() throws Exception {
        DashboardStatsDto stats = new DashboardStatsDto(
                5,
                42,
                38,
                20,
                0,
                0,
                OffsetDateTime.now(),
                12500,
                BigDecimal.valueOf(0.0037),
                "Last 7 Days",
                List.of(new DashboardStatsDto.ProcessingActivityPoint("Oct 28", 12, 2, 8, 0)),
                List.of(new DashboardStatsDto.AreaDistributionPoint("ARTIFICIAL_INTELLIGENCE", "Artificial Intelligence", 15, 60.0)),
                List.of(new DashboardStatsDto.SourcePerformancePoint(UUID.randomUUID(), "MIT Tech Review", 10, 8, 80.0)),
                List.of(new DashboardStatsDto.AiUsagePoint("Oct 28", "run-1234", 1500, BigDecimal.valueOf(0.0004))),
                new DashboardStatsDto.ProcessingOutcomesDto(20, 4, 0, 0, 24)
        );

        when(statsService.getDashboardStatistics("7days")).thenReturn(stats);

        mockMvc.perform(get("/api/v1/intelligence/stats").param("period", "7days"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeNodes").value(5))
                .andExpect(jsonPath("$.articlesDiscovered").value(42))
                .andExpect(jsonPath("$.summariesGenerated").value(20))
                .andExpect(jsonPath("$.period").value("Last 7 Days"));
    }

    @Test
    void toggleArticleBookmark_updatesBookmark() throws Exception {
        UUID articleId = UUID.randomUUID();
        ArticleFeedItemDto item = new ArticleFeedItemDto(
                articleId,
                "Autonomous Agents Scaled Up",
                "Summary",
                "https://tech.org/1",
                "https://tech.org/1",
                "Author",
                "Source",
                null,
                "AI",
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                "gpt-4o-mini",
                100,
                200,
                BigDecimal.ZERO,
                true,
                false,
                "SUMMARIZED"
        );

        when(articleFeedService.toggleBookmark(articleId, true)).thenReturn(item);

        mockMvc.perform(put("/api/v1/intelligence/articles/" + articleId + "/bookmark")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookmarked\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(articleId.toString()))
                .andExpect(jsonPath("$.bookmarked").value(true));
    }

    @Test
    void updateBookmark_legacyEndpoint_persistsBookmarkStatus() throws Exception {
        when(jdbc.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);

        mockMvc.perform(put("/api/v1/intelligence/briefs/brief-1/bookmark")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookmarked\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("brief-1"))
                .andExpect(jsonPath("$.bookmarked").value(true))
                .andExpect(jsonPath("$.bookmarkedAt").isNotEmpty());

        verify(jdbc).update(anyString(), any(MapSqlParameterSource.class));
    }

    @Test
    void updateReadStatus_legacyEndpoint_persistsReadStatus() throws Exception {
        when(jdbc.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);

        mockMvc.perform(put("/api/v1/intelligence/briefs/brief-1/read")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isRead\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("brief-1"))
                .andExpect(jsonPath("$.isRead").value(true))
                .andExpect(jsonPath("$.readAt").isNotEmpty());

        verify(jdbc).update(anyString(), any(MapSqlParameterSource.class));
    }

    @Test
    void getHorizonRadar_returnsSpatialBlipCoordinates() throws Exception {
        Map<String, Object> blip1 = new HashMap<>();
        blip1.put("id", "blip-ai-eval");
        blip1.put("title", "Autonomous Context Distillation");
        blip1.put("category", "AI");
        blip1.put("xPercent", 28.5);
        blip1.put("yPercent", 32.0);
        blip1.put("gravity", "HIGH");
        blip1.put("urgencyScore", 88);
        blip1.put("linkedBriefId", "brief-1");
        blip1.put("color", "cyan");

        when(jdbc.queryForList(anyString(), any(MapSqlParameterSource.class)))
                .thenReturn(List.of(blip1));

        mockMvc.perform(get("/api/v1/intelligence/radar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBlips").value(1))
                .andExpect(jsonPath("$.blips[0].id").value("blip-ai-eval"));
    }

    @Test
    void deleteArticle_softDeletesArticleAndReturnsNoContent() throws Exception {
        UUID articleId = UUID.randomUUID();
        doNothing().when(articleFeedService).deleteArticle(articleId);

        mockMvc.perform(delete("/api/v1/intelligence/articles/" + articleId))
                .andExpect(status().isNoContent());

        verify(articleFeedService).deleteArticle(articleId);
    }

    @Test
    void createArea_createsNewCategoryAndReturnsCreated() throws Exception {
        com.monday.app.intelligence.entity.InterestCategory cat = new com.monday.app.intelligence.entity.InterestCategory();
        cat.setCode("QUANTUM_COMPUTING");
        cat.setName("Quantum Computing");
        cat.setStatus("ACTIVE");

        when(categoryRepository.findByCodeIgnoreCase("QUANTUM_COMPUTING")).thenReturn(java.util.Optional.empty());
        when(categoryRepository.findByNameIgnoreCase("Quantum Computing")).thenReturn(java.util.Optional.empty());
        when(categoryRepository.save(any())).thenReturn(cat);

        mockMvc.perform(post("/api/v1/intelligence/areas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Quantum Computing\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("QUANTUM_COMPUTING"))
                .andExpect(jsonPath("$.name").value("Quantum Computing"));
    }
}
