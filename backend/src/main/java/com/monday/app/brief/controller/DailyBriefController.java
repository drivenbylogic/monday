package com.monday.app.brief.controller;

import com.monday.app.brief.entity.DailyBrief;
import com.monday.app.brief.service.DailyBriefService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/briefs")
public class DailyBriefController {

    private final DailyBriefService briefService;

    public DailyBriefController(DailyBriefService briefService) {
        this.briefService = briefService;
    }

    @PostMapping
    public ResponseEntity<DailyBrief> saveBrief(@Valid @RequestBody DailyBrief brief) {
        // Creates or updates based on presence of ID, per service logic
        boolean isNew = brief.getId() == null;
        DailyBrief saved = briefService.saveBrief(brief);
        return ResponseEntity.status(isNew ? HttpStatus.CREATED : HttpStatus.OK).body(saved);
    }

    @GetMapping
    public ResponseEntity<List<DailyBrief>> getRecentBriefs(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(briefService.getRecentBriefs(days));
    }

    @GetMapping("/{date}")
    public ResponseEntity<DailyBrief> getBriefByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        DailyBrief brief = briefService.getBriefByDate(date);
        return brief != null ? ResponseEntity.ok(brief) : ResponseEntity.notFound().build();
    }
}
