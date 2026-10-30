package com.monday.app.agenda.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.monday.app.agenda.dto.*;
import com.monday.app.ai.gateway.AiGateway;
import com.monday.app.ai.model.AiProvider;
import com.monday.app.ai.model.AiRequest;
import com.monday.app.ai.model.AiResponse;
import com.monday.app.brief.entity.DailyBrief;
import com.monday.app.brief.repository.DailyBriefRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AgendaGenerationService {

    private static final Logger log = LoggerFactory.getLogger(AgendaGenerationService.class);

    private final AgendaContextService contextService;
    private final AgendaValidationService validationService;
    private final DeterministicAgendaGenerator deterministicGenerator;
    private final DailyBriefRepository dailyBriefRepository;
    private final AiGateway aiGateway;
    private final ObjectMapper objectMapper;

    @Value("${ai.default-model:gpt-4o-mini}")
    private String defaultModel;

    @Value("${agenda.cache.min-refresh-interval-hours:6}")
    private long minRefreshIntervalHours = 6;

    private Clock clock = Clock.systemUTC();

    // Concurrency lock map per date to avoid redundant expensive AI calls
    private final ConcurrentHashMap<LocalDate, Object> dateLocks = new ConcurrentHashMap<>();

    public AgendaGenerationService(AgendaContextService contextService,
                                   AgendaValidationService validationService,
                                   DeterministicAgendaGenerator deterministicGenerator,
                                   DailyBriefRepository dailyBriefRepository,
                                   AiGateway aiGateway) {
        this.contextService = contextService;
        this.validationService = validationService;
        this.deterministicGenerator = deterministicGenerator;
        this.dailyBriefRepository = dailyBriefRepository;
        this.aiGateway = aiGateway;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public void setMinRefreshIntervalHours(long minRefreshIntervalHours) {
        this.minRefreshIntervalHours = minRefreshIntervalHours;
    }

    public TodayAgendaResponse getAgenda(LocalDate date, ZoneId zoneId) {
        if (date == null) {
            date = LocalDate.now(zoneId != null ? zoneId : ZoneId.systemDefault());
        }
        if (zoneId == null) {
            zoneId = ZoneId.of("UTC");
        }

        Optional<DailyBrief> existing = dailyBriefRepository.findByDate(date);
        if (existing.isPresent()) {
            TodayAgendaResponse parsed = parseBrief(existing.get(), date);
            if (parsed != null && "GENERATED".equalsIgnoreCase(parsed.status()) && parsed.summaryMarkdown() != null) {
                return parsed;
            }
        }

        // Generate if not present
        return generateAgenda(date, zoneId, false);
    }

    public TodayAgendaResponse generateAgenda(LocalDate date, ZoneId zoneId, boolean forceRegenerate) {
        if (date == null) {
            date = LocalDate.now(zoneId != null ? zoneId : ZoneId.systemDefault());
        }
        if (zoneId == null) {
            zoneId = ZoneId.of("UTC");
        }

        final LocalDate effectiveDate = date;
        final ZoneId effectiveZoneId = zoneId;

        Object lock = dateLocks.computeIfAbsent(effectiveDate, d -> new Object());
        synchronized (lock) {
            try {
                // Check if an existing response exists for the date in the database
                Optional<DailyBrief> existing = dailyBriefRepository.findByDate(effectiveDate);
                if (existing.isPresent()) {
                    TodayAgendaResponse parsed = parseBrief(existing.get(), effectiveDate);
                    if (parsed != null && "GENERATED".equalsIgnoreCase(parsed.status()) && parsed.summaryMarkdown() != null) {
                        Instant generationTime = parsed.generatedAt() != null
                                ? parsed.generatedAt()
                                : (existing.get().getUpdatedAt() != null ? existing.get().getUpdatedAt() : existing.get().getCreatedAt());

                        boolean isOlderThanThreshold = false;
                        if (generationTime != null) {
                            Duration age = Duration.between(generationTime, clock.instant());
                            isOlderThanThreshold = age.getSeconds() >= (minRefreshIntervalHours * 3600);
                        }

                        // When a refresh occurs, if and only if it is 6 hours or older, another AI response
                        // should be generated, or else the existing response should be served.
                        if (!isOlderThanThreshold) {
                            log.info("Existing AI agenda for date {} was generated at {} (age < {} hours). Serving existing response instead of regenerating.",
                                    effectiveDate, generationTime, minRefreshIntervalHours);
                            return parsed;
                        }

                        log.info("Existing AI agenda for date {} was generated at {} (age >= {} hours). Proceeding with AI regeneration.",
                                effectiveDate, generationTime, minRefreshIntervalHours);
                    }
                }

                // 1. Build bounded context
                AgendaContextPayload context = contextService.buildContext(effectiveDate, effectiveZoneId);

                // 2. Attempt AI generation or fallback to deterministic generator
                TodayAgendaResponse generated = attemptAiGeneration(context);
                if (generated == null) {
                    log.info("AI Gateway returned empty or failed. Using DeterministicAgendaGenerator for {}", effectiveDate);
                    generated = deterministicGenerator.generate(context);
                }

                // 3. Validate recommendations and time blocks
                List<AgendaRecommendationDto> validatedRecs = validationService.validateRecommendations(
                        generated.recommendations(),
                        context.schedule().events()
                );

                List<SuggestedTimeBlockDto> validatedBlocks = validationService.validateSuggestedBlocks(
                        generated.suggestedFocusBlocks(),
                        context.schedule().events()
                );

                TodayAgendaResponse finalAgenda = new TodayAgendaResponse(
                        effectiveDate,
                        "GENERATED",
                        clock.instant(),
                        generated.summaryMarkdown(),
                        validatedRecs,
                        validatedBlocks,
                        generated.importantAlerts(),
                        generated.financialHeadsUp(),
                        generated.executionInsight(),
                        false,
                        null
                );

                // 4. Persist to daily_briefs
                persistAgenda(finalAgenda);

                return finalAgenda;

            } catch (Exception e) {
                log.error("Failed to generate agenda for date {}: {}", effectiveDate, e.getMessage(), e);

                // Check if a previously valid brief exists to preserve it
                Optional<DailyBrief> existing = dailyBriefRepository.findByDate(effectiveDate);
                if (existing.isPresent()) {
                    TodayAgendaResponse fallback = parseBrief(existing.get(), effectiveDate);
                    if (fallback != null) {
                        return new TodayAgendaResponse(
                                fallback.date(),
                                "STALE",
                                fallback.generatedAt(),
                                fallback.summaryMarkdown(),
                                fallback.recommendations(),
                                fallback.suggestedFocusBlocks(),
                                fallback.importantAlerts(),
                                fallback.financialHeadsUp(),
                                fallback.executionInsight(),
                                true,
                                "Generation attempt encountered an error; displaying previously saved brief."
                        );
                    }
                }

                // Fallback to purely deterministic generation
                try {
                    AgendaContextPayload context = contextService.buildContext(effectiveDate, effectiveZoneId);
                    TodayAgendaResponse deterministic = deterministicGenerator.generate(context);
                    persistAgenda(deterministic);
                    return deterministic;
                } catch (Exception ex) {
                    log.error("Fatal failure in deterministic fallback: {}", ex.getMessage(), ex);
                    return new TodayAgendaResponse(
                            effectiveDate,
                            "FAILED",
                            Instant.now(),
                            "Agenda generation is temporarily unavailable.",
                            Collections.emptyList(),
                            Collections.emptyList(),
                            Collections.emptyList(),
                            null,
                            null,
                            true,
                            "System error during agenda generation"
                    );
                }
            } finally {
                dateLocks.remove(effectiveDate);
            }
        }
    }

    private TodayAgendaResponse attemptAiGeneration(AgendaContextPayload context) {
        try {
            String contextJson = objectMapper.writeValueAsString(context);
            String prompt = """
                    You are Monday, a strategic personal intelligence system and executive assistant.
                    Given the following strictly grounded JSON context for the user's day:
                    
                    """ + contextJson + """
                    
                    Synthesize an executive daily agenda across all 7 operational modules:
                    - Daily Schedule & Capacity
                    - Projects & Tasks Execution
                    - Personal Finances & Budgets
                    - Vehicles & Fleet Mobility
                    - Administration, Household & Occasions
                    - Career Transition & Technical Milestones
                    - Industry Intelligence & Article Briefings

                    Generate a structured JSON response matching the following schema.
                    CRITICAL RULES:
                    1. The output MUST be a valid JSON object ONLY. No conversational preambles, no markdown codeblocks surrounding the JSON.
                    2. Recommendations MUST be grounded ONLY in the supplied entity IDs and facts. Do NOT invent new UUIDs or tasks.
                    3. Limit recommendations to at most 3 top priority items, strategically balanced across high-impact commitments.
                    4. For each recommendation, provide a compelling reason, a concrete next action, and accurate source references.
                    5. Allowed sourceReferences.type:
                       - TASK: for core execution tasks
                       - PROJECT: for active projects
                       - MILESTONE: for project milestones
                       - EVENT: for calendar events
                       - COMMITMENT: for financial obligations
                       - VEHICLE: for vehicle fleet actions, maintenance, or tasks
                       - ADMIN_OBLIGATION: for administrative bills, subscriptions, or renewals
                       - ADMIN_DATE: for birthdays, anniversaries, or administrative occasions
                       - CAREER_TASK: for career transition tasks
                       - CAREER_MILESTONE: for career plan milestones
                       - ARTICLE: for curated industry intelligence articles
                    6. Allowed importantAlerts.type: DEADLINE | OVERLOAD | FINANCIAL | CONFLICT | VEHICLE | ADMINISTRATIVE | CAREER | INTELLIGENCE
                    7. JSON structure:
                    {
                      "summaryMarkdown": "string",
                      "recommendations": [
                        {
                          "id": "rec-1",
                          "title": "string",
                          "description": "string",
                          "priority": "HIGH|MEDIUM|LOW",
                          "reason": "string",
                          "nextAction": "string",
                          "sourceReferences": [
                            {"type": "TASK|PROJECT|MILESTONE|EVENT|COMMITMENT|VEHICLE|ADMIN_OBLIGATION|ADMIN_DATE|CAREER_TASK|CAREER_MILESTONE|ARTICLE", "id": "uuid-from-context", "title": "string"}
                          ],
                          "suggestedDurationMinutes": 60,
                          "confidence": 0.95
                        }
                      ],
                      "importantAlerts": [
                        {"type": "DEADLINE|OVERLOAD|FINANCIAL|CONFLICT|VEHICLE|ADMINISTRATIVE|CAREER|INTELLIGENCE", "title": "string", "message": "string", "severity": "DANGER|WARNING|INFO"}
                      ],
                      "financialHeadsUp": {
                        "title": "string", "message": "string", "impactAmount": 0.00, "urgency": "HIGH|MEDIUM|LOW"
                      },
                      "executionInsight": {
                        "title": "string", "message": "string", "metricHighlight": "string"
                      }
                    }
                    """;

            AiRequest request = new AiRequest(AiProvider.OPENAI, defaultModel, prompt);
            AiResponse response = aiGateway.generate(request);

            if (response != null && response.content() != null && !response.content().isBlank()) {
                String cleanJson = extractJson(response.content());
                AiOutputWrapper wrapper = objectMapper.readValue(cleanJson, AiOutputWrapper.class);

                return new TodayAgendaResponse(
                        context.agendaDate(),
                        "GENERATED",
                        Instant.now(),
                        wrapper.summaryMarkdown,
                        wrapper.recommendations != null ? wrapper.recommendations : Collections.emptyList(),
                        Collections.emptyList(),
                        wrapper.importantAlerts != null ? wrapper.importantAlerts : Collections.emptyList(),
                        wrapper.financialHeadsUp,
                        wrapper.executionInsight,
                        false,
                        null
                );
            }
        } catch (Exception e) {
            log.warn("AI generation failed ({}: {}). Resorting to deterministic engine.", e.getClass().getSimpleName(), e.getMessage());
        }
        return null;
    }

    private String extractJson(String text) {
        text = text.trim();
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        return text.trim();
    }

    @Transactional
    public void persistAgenda(TodayAgendaResponse agenda) {
        try {
            String structuredJson = objectMapper.writeValueAsString(agenda);
            Optional<DailyBrief> existing = dailyBriefRepository.findByDate(agenda.date());

            if (existing.isPresent()) {
                DailyBrief b = existing.get();
                b.setStructuredAgendaJson(structuredJson);
                b.setAiSummaryMarkdown(agenda.summaryMarkdown());
                b.setStatus(agenda.status());
                b.setUpdatedAt(clock.instant());
                dailyBriefRepository.update(b);
            } else {
                DailyBrief b = new DailyBrief();
                b.setId(UUID.randomUUID());
                b.setBriefDate(agenda.date());
                b.setStructuredAgendaJson(structuredJson);
                b.setAiSummaryMarkdown(agenda.summaryMarkdown());
                b.setStatus(agenda.status());
                b.setCreatedAt(clock.instant());
                b.setUpdatedAt(clock.instant());
                dailyBriefRepository.save(b);
            }
        } catch (Exception e) {
            log.error("Failed to persist daily brief for {}: {}", agenda.date(), e.getMessage(), e);
        }
    }

    @Transactional
    public TodayAgendaResponse applyFeedback(LocalDate date, String recommendationId, String action) {
        Optional<DailyBrief> existing = dailyBriefRepository.findByDate(date);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("No agenda found for date " + date);
        }

        TodayAgendaResponse agenda = parseBrief(existing.get(), date);
        if (agenda == null) {
            throw new IllegalStateException("Failed to parse existing agenda JSON");
        }

        List<AgendaRecommendationDto> updatedRecs = new ArrayList<>();
        for (AgendaRecommendationDto rec : agenda.recommendations()) {
            if (rec.id().equalsIgnoreCase(recommendationId)) {
                updatedRecs.add(new AgendaRecommendationDto(
                        rec.id(),
                        rec.title(),
                        rec.description(),
                        rec.priority(),
                        rec.reason(),
                        rec.nextAction(),
                        rec.sourceReferences(),
                        rec.suggestedDurationMinutes(),
                        rec.suggestedStartAt(),
                        rec.suggestedEndAt(),
                        rec.confidence(),
                        action.toUpperCase()
                ));
            } else {
                updatedRecs.add(rec);
            }
        }

        TodayAgendaResponse updatedAgenda = new TodayAgendaResponse(
                agenda.date(),
                agenda.status(),
                agenda.generatedAt(),
                agenda.summaryMarkdown(),
                updatedRecs,
                agenda.suggestedFocusBlocks(),
                agenda.importantAlerts(),
                agenda.financialHeadsUp(),
                agenda.executionInsight(),
                agenda.isStale(),
                agenda.staleReason()
        );

        persistAgenda(updatedAgenda);
        return updatedAgenda;
    }

    private TodayAgendaResponse parseBrief(DailyBrief brief, LocalDate date) {
        try {
            if (brief.getStructuredAgendaJson() != null && !brief.getStructuredAgendaJson().isBlank()) {
                return objectMapper.readValue(brief.getStructuredAgendaJson(), TodayAgendaResponse.class);
            }
        } catch (Exception e) {
            log.warn("Could not parse structured_agenda_json for brief {}: {}", brief.getId(), e.getMessage());
        }
        return null;
    }

    private static class AiOutputWrapper {
        public String summaryMarkdown;
        public List<AgendaRecommendationDto> recommendations;
        public List<AgendaAlertDto> importantAlerts;
        public FinancialHeadsUpDto financialHeadsUp;
        public ExecutionInsightDto executionInsight;
    }
}
