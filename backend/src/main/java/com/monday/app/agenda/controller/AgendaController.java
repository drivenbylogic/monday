package com.monday.app.agenda.controller;

import com.monday.app.agenda.dto.*;
import com.monday.app.agenda.service.AgendaEngineService;
import com.monday.app.agenda.service.AgendaGenerationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping({"/api/v1/agenda", "/api/agenda"})
public class AgendaController {

    private final AgendaEngineService agendaEngineService;
    private final AgendaGenerationService agendaGenerationService;

    public AgendaController(AgendaEngineService agendaEngineService,
                            AgendaGenerationService agendaGenerationService) {
        this.agendaEngineService = agendaEngineService;
        this.agendaGenerationService = agendaGenerationService;
    }

    /**
     * Today's Agenda - AI-powered and deterministic validated daily briefing.
     */
    @GetMapping("/today")
    public ResponseEntity<TodayAgendaResponse> getTodayAgenda(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestHeader(value = "X-Timezone", defaultValue = "UTC") String timezone) {

        ZoneId zoneId = parseZoneId(timezone);
        if (date == null) {
            date = LocalDate.now(zoneId);
        }

        TodayAgendaResponse response = agendaGenerationService.getAgenda(date, zoneId);
        return ResponseEntity.ok(response);
    }

    /**
     * Trigger explicit generation or refresh of today's agenda.
     */
    @PostMapping("/generate")
    public ResponseEntity<TodayAgendaResponse> generateAgenda(@RequestBody(required = false) GenerateAgendaRequest request) {
        LocalDate date = request != null && request.date() != null ? request.date() : LocalDate.now();
        ZoneId zoneId = request != null && request.timezone() != null ? parseZoneId(request.timezone()) : ZoneId.of("UTC");
        boolean force = request != null && Boolean.TRUE.equals(request.force());

        TodayAgendaResponse response = agendaGenerationService.generateAgenda(date, zoneId, force);
        return ResponseEntity.ok(response);
    }

    /**
     * Submit user feedback on an agenda priority recommendation (ACCEPTED, DISMISSED, DEFERRED).
     */
    @PostMapping("/feedback")
    public ResponseEntity<TodayAgendaResponse> submitFeedback(@RequestBody AgendaFeedbackRequest request) {
        if (request == null || request.recommendationId() == null || request.action() == null) {
            return ResponseEntity.badRequest().build();
        }

        LocalDate date = request.date() != null ? request.date() : LocalDate.now();
        TodayAgendaResponse response = agendaGenerationService.applyFeedback(date, request.recommendationId(), request.action());
        return ResponseEntity.ok(response);
    }

    /**
     * Preserved for backward compatibility with existing services.
     */
    @GetMapping("/{date}")
    public ResponseEntity<DailyAgenda> getAgendaForDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestHeader(value = "X-Timezone", defaultValue = "UTC") String timezone) {

        ZoneId zoneId = parseZoneId(timezone);
        DailyAgenda agenda = agendaEngineService.getAgendaForDate(date, zoneId);
        return ResponseEntity.ok(agenda);
    }

    private ZoneId parseZoneId(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            return ZoneId.of("UTC");
        }
    }
}
