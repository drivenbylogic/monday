package com.monday.app.agenda.controller;

import com.monday.app.agenda.dto.HomeDashboardResponse;
import com.monday.app.agenda.service.HomeDashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping({"/api/v1/home", "/api/home"})
public class HomeDashboardController {

    private final HomeDashboardService dashboardService;

    public HomeDashboardController(HomeDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<HomeDashboardResponse> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestHeader(value = "X-Timezone", defaultValue = "UTC") String timezone) {

        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(timezone);
        } catch (Exception e) {
            zoneId = ZoneId.of("UTC");
        }

        if (date == null) {
            date = LocalDate.now(zoneId);
        }

        HomeDashboardResponse response = dashboardService.getDashboard(date, zoneId);
        return ResponseEntity.ok(response);
    }
}
