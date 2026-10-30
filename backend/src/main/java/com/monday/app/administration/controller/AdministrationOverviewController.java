package com.monday.app.administration.controller;

import com.monday.app.administration.dto.AdministrationOverviewDto;
import com.monday.app.administration.service.AdministrationOverviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/administration/overview", "/api/administration/overview"})
public class AdministrationOverviewController {

    private final AdministrationOverviewService service;

    public AdministrationOverviewController(AdministrationOverviewService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<AdministrationOverviewDto> getOverview() {
        return ResponseEntity.ok(service.getOverview());
    }
}
