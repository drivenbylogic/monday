package com.monday.app.agenda.controller;

import com.monday.app.agenda.dto.DailyAgenda;
import com.monday.app.agenda.service.AgendaEngineService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/v1/agenda")
public class AgendaController {

    private final AgendaEngineService agendaEngineService;

    public AgendaController(AgendaEngineService agendaEngineService) {
        this.agendaEngineService = agendaEngineService;
    }

    @GetMapping("/{date}")
    public ResponseEntity<DailyAgenda> getAgendaForDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestHeader(value = "X-Timezone", defaultValue = "UTC") String timezone) {
        
        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(timezone);
        } catch (Exception e) {
            zoneId = ZoneId.of("UTC");
        }
        
        DailyAgenda agenda = agendaEngineService.getAgendaForDate(date, zoneId);
        return ResponseEntity.ok(agenda);
    }
}
