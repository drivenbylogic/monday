package com.monday.app.intelligence.service;

import com.monday.app.intelligence.dto.IntelligenceStatusDto;
import com.monday.app.intelligence.entity.IntelligenceRun;
import com.monday.app.intelligence.entity.RssFeed;
import com.monday.app.intelligence.ingestion.ArticleNormalizer;
import com.monday.app.intelligence.ingestion.FeedFetcher;
import com.monday.app.intelligence.ingestion.RssParser;
import com.monday.app.intelligence.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IntelligenceCoordinatorTest {

    private IntelligenceRunRepository runRepository;
    private RssFeedRepository feedRepository;
    private ArticleRepository articleRepository;
    private ArticleSummaryRepository summaryRepository;
    private ArticleSourceRepository articleSourceRepository;
    private FeedManagementService feedService;
    private FeedFetcher feedFetcher;
    private RssParser rssParser;
    private ArticleNormalizer articleNormalizer;
    private ArticleDeduplicationService deduplicationService;
    private DeterministicArticleSelector articleSelector;
    private IntelligenceSummaryService summaryService;

    private IntelligenceCoordinator coordinator;

    @BeforeEach
    void setUp() {
        runRepository = mock(IntelligenceRunRepository.class);
        feedRepository = mock(RssFeedRepository.class);
        articleRepository = mock(ArticleRepository.class);
        summaryRepository = mock(ArticleSummaryRepository.class);
        articleSourceRepository = mock(ArticleSourceRepository.class);
        feedService = mock(FeedManagementService.class);
        feedFetcher = mock(FeedFetcher.class);
        rssParser = mock(RssParser.class);
        articleNormalizer = mock(ArticleNormalizer.class);
        deduplicationService = mock(ArticleDeduplicationService.class);
        articleSelector = mock(DeterministicArticleSelector.class);
        summaryService = mock(IntelligenceSummaryService.class);

        coordinator = new IntelligenceCoordinator(
                runRepository,
                feedRepository,
                articleRepository,
                summaryRepository,
                articleSourceRepository,
                feedService,
                feedFetcher,
                rssParser,
                articleNormalizer,
                deduplicationService,
                articleSelector,
                summaryService
        );
    }

    @Test
    void getStatus_whenNoPreviousRun_allowsInitialGeneration() {
        when(runRepository.findFirstByOrderByStartedAtDesc()).thenReturn(Optional.empty());
        when(feedRepository.findByIsEnabledTrue()).thenReturn(List.of(new RssFeed()));

        IntelligenceStatusDto status = coordinator.getStatus();

        assertTrue(status.canGenerate());
        assertFalse(status.isRunning());
        assertEquals(0, status.cooldownSecondsRemaining());
    }

    @Test
    void triggerManualGeneration_whenLastRunLessThan6HoursAgo_blocksGeneration() {
        OffsetDateTime recentStart = OffsetDateTime.now().minusHours(2);
        IntelligenceRun recentRun = new IntelligenceRun();
        recentRun.setId(UUID.randomUUID());
        recentRun.setStartedAt(recentStart);
        recentRun.setStatus("COMPLETED");

        when(runRepository.findFirstByOrderByStartedAtDesc()).thenReturn(Optional.of(recentRun));

        IntelligenceCoordinator.ExecutionOutcome outcome = coordinator.triggerManualGeneration();

        assertFalse(outcome.runExecuted());
        assertEquals("COOLDOWN_ACTIVE", outcome.status());
        assertTrue(outcome.cooldownSecondsRemaining() > 0);
        assertTrue(outcome.message().contains("Cooldown active"));
        try {
            verify(feedFetcher, never()).fetchWithCache(any(), any(), any());
        } catch (Exception ignored) {}
    }

    @Test
    void triggerManualGeneration_whenLastRunOlderThan6Hours_allowsGeneration() throws Exception {
        OffsetDateTime oldStart = OffsetDateTime.now().minusHours(7);
        IntelligenceRun oldRun = new IntelligenceRun();
        oldRun.setId(UUID.randomUUID());
        oldRun.setStartedAt(oldStart);
        oldRun.setStatus("COMPLETED");

        when(runRepository.findFirstByOrderByStartedAtDesc()).thenReturn(Optional.of(oldRun));
        when(feedRepository.findByIsEnabledTrue()).thenReturn(List.of());
        when(runRepository.save(any(IntelligenceRun.class))).thenAnswer(inv -> inv.getArgument(0));

        IntelligenceCoordinator.ExecutionOutcome outcome = coordinator.triggerManualGeneration();

        assertTrue(outcome.runExecuted());
        assertEquals("COMPLETED", outcome.status());
        verify(runRepository, atLeastOnce()).save(any(IntelligenceRun.class));
    }
}
