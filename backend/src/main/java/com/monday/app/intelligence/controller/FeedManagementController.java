package com.monday.app.intelligence.controller;

import com.monday.app.intelligence.dto.CreateRssNodeRequest;
import com.monday.app.intelligence.dto.FeedPreviewResponse;
import com.monday.app.intelligence.dto.RssNodeDto;
import com.monday.app.intelligence.dto.UpdateRssNodeRequest;
import com.monday.app.intelligence.service.FeedManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/intelligence/feeds")
public class FeedManagementController {

    private final FeedManagementService feedService;

    public FeedManagementController(FeedManagementService feedService) {
        this.feedService = feedService;
    }

    @GetMapping
    public ResponseEntity<List<RssNodeDto>> getFeeds(@RequestParam(required = false) String area) {
        List<RssNodeDto> nodes = feedService.getAllNodes();
        if (area != null && !area.isBlank() && !"all".equalsIgnoreCase(area)) {
            nodes = nodes.stream()
                    .filter(n -> area.equalsIgnoreCase(n.areaOfInterest()))
                    .toList();
        }
        return ResponseEntity.ok(nodes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RssNodeDto> getFeed(@PathVariable UUID id) {
        return ResponseEntity.ok(feedService.getNode(id));
    }

    @PostMapping
    public ResponseEntity<RssNodeDto> createFeed(@RequestBody CreateRssNodeRequest payload) {
        RssNodeDto created = feedService.createNode(payload);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RssNodeDto> updateFeed(@PathVariable UUID id, @RequestBody UpdateRssNodeRequest payload) {
        return ResponseEntity.ok(feedService.updateNode(id, payload));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<RssNodeDto> patchFeed(@PathVariable UUID id, @RequestBody Map<String, Object> payload) {
        String name = (String) payload.get("name");
        String feedUrl = (String) payload.get("feedUrl");
        String siteUrl = (String) payload.get("siteUrl");
        String area = (String) payload.get("areaOfInterest");
        String description = (String) payload.get("description");
        Integer limit = payload.containsKey("articleLimit") ? Integer.valueOf(payload.get("articleLimit").toString()) : null;
        Boolean isEnabled = payload.containsKey("isEnabled") ? Boolean.valueOf(payload.get("isEnabled").toString()) : null;

        UpdateRssNodeRequest req = new UpdateRssNodeRequest(name, feedUrl, siteUrl, area, description, limit, isEnabled);
        return ResponseEntity.ok(feedService.updateNode(id, req));
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<RssNodeDto> toggleFeed(@PathVariable UUID id, @RequestParam boolean enabled) {
        return ResponseEntity.ok(feedService.toggleFeedEnabled(id, enabled));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeed(@PathVariable UUID id) {
        feedService.deleteFeed(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/preview")
    public ResponseEntity<FeedPreviewResponse> previewFeed(@RequestBody Map<String, String> payload) {
        String url = payload.get("feedUrl");
        return ResponseEntity.ok(feedService.previewFeed(url));
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<Map<String, Object>> testFeed(@PathVariable UUID id) {
        RssNodeDto node = feedService.getNode(id);
        FeedPreviewResponse preview = feedService.previewFeed(node.feedUrl());
        return ResponseEntity.ok(Map.of(
                "success", preview.reachable(),
                "reachable", preview.reachable(),
                "title", preview.title() != null ? preview.title() : "",
                "entryCount", preview.entryCount(),
                "error", preview.errorMessage() != null ? preview.errorMessage() : ""
        ));
    }
}
