package com.monday.app.search.controller;

import com.monday.app.search.dto.ModuleSearchResponse;
import com.monday.app.search.service.GlobalSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/search")
public class GlobalSearchController {

    private final GlobalSearchService searchService;

    public GlobalSearchController(GlobalSearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Search an individual module (Projects, Events, Finance, Vehicles, Administration, Career, Intelligence).
     * Used by the frontend to run parallel queries simultaneously across each module.
     */
    @GetMapping("/modules/{module}")
    public ResponseEntity<ModuleSearchResponse> searchModule(
            @PathVariable String module,
            @RequestParam(required = false, defaultValue = "") String q) {
        return ResponseEntity.ok(searchService.searchModule(module, q));
    }

    /**
     * Search across all 7 modules combined.
     */
    @GetMapping
    public ResponseEntity<Map<String, ModuleSearchResponse>> searchAll(
            @RequestParam(required = false, defaultValue = "") String q) {
        return ResponseEntity.ok(searchService.searchAll(q));
    }
}
