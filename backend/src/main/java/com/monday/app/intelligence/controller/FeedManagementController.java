package com.monday.app.intelligence.controller;

import com.monday.app.intelligence.entity.RssFeed;
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
    public ResponseEntity<List<RssFeed>> getFeeds() {
        return ResponseEntity.ok(feedService.getAllFeeds());
    }

    @PostMapping
    public ResponseEntity<RssFeed> createFeed(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String feedUrl = payload.get("feedUrl");
        String siteUrl = payload.get("siteUrl");
        return ResponseEntity.status(HttpStatus.CREATED).body(feedService.createFeed(name, feedUrl, siteUrl));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<RssFeed> updateFeed(@PathVariable UUID id, @RequestBody Map<String, Object> payload) {
        String name = (String) payload.get("name");
        String siteUrl = (String) payload.get("siteUrl");
        Double sourceQuality = payload.containsKey("sourceQuality") ? 
                Double.valueOf(payload.get("sourceQuality").toString()) : null;
                
        return ResponseEntity.ok(feedService.updateFeed(id, name, siteUrl, sourceQuality));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeed(@PathVariable UUID id) {
        feedService.deleteFeed(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<Map<String, Boolean>> testFeed(@PathVariable UUID id) {
        RssFeed feed = feedService.getFeed(id);
        boolean success = feedService.testFeed(feed.getFeedUrl());
        return ResponseEntity.ok(Map.of("success", success));
    }
}
