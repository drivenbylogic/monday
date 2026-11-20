package com.monday.app.agenda.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TodayAgendaResponse(
        LocalDate date,
        String status, // "GENERATED", "STALE", "FAILED"
        Instant generatedAt,
        String summaryMarkdown,
        List<AgendaRecommendationDto> recommendations,
        List<SuggestedTimeBlockDto> suggestedFocusBlocks,
        List<AgendaAlertDto> importantAlerts,
        FinancialHeadsUpDto financialHeadsUp,
        ExecutionInsightDto executionInsight,
        boolean isStale,
        String staleReason,
        List<AdministrationAgendaSummaryDto.AdminImportantDateSummaryDto> occasionsToday
) {
    public TodayAgendaResponse(
            LocalDate date,
            String status,
            Instant generatedAt,
            String summaryMarkdown,
            List<AgendaRecommendationDto> recommendations,
            List<SuggestedTimeBlockDto> suggestedFocusBlocks,
            List<AgendaAlertDto> importantAlerts,
            FinancialHeadsUpDto financialHeadsUp,
            ExecutionInsightDto executionInsight,
            boolean isStale,
            String staleReason
    ) {
        this(
                date,
                status,
                generatedAt,
                summaryMarkdown,
                recommendations,
                suggestedFocusBlocks,
                importantAlerts,
                financialHeadsUp,
                executionInsight,
                isStale,
                staleReason,
                List.of()
        );
    }
}
