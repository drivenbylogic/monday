package com.monday.app.intelligence.controller;

import com.monday.app.intelligence.dto.ArticleFeedItemDto;
import com.monday.app.intelligence.dto.DashboardStatsDto;
import com.monday.app.intelligence.dto.IntelligenceStatusDto;
import com.monday.app.intelligence.entity.*;
import com.monday.app.intelligence.repository.ArticleSummaryRepository;
import com.monday.app.intelligence.repository.DailyArticleSelectionRepository;
import com.monday.app.intelligence.repository.IntelligenceRunRepository;
import com.monday.app.intelligence.repository.InterestCategoryRepository;
import com.monday.app.intelligence.service.ArticleFeedService;
import com.monday.app.intelligence.service.DashboardStatsService;
import com.monday.app.intelligence.service.IntelligenceCoordinator;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/intelligence")
public class IntelligenceController {

    private final IntelligenceCoordinator coordinator;
    private final ArticleFeedService articleFeedService;
    private final DashboardStatsService statsService;
    private final IntelligenceRunRepository runRepository;
    private final DailyArticleSelectionRepository selectionRepository;
    private final ArticleSummaryRepository summaryRepository;
    private final InterestCategoryRepository categoryRepository;
    private final NamedParameterJdbcTemplate jdbc;

    public IntelligenceController(IntelligenceCoordinator coordinator,
                                  ArticleFeedService articleFeedService,
                                  DashboardStatsService statsService,
                                  IntelligenceRunRepository runRepository,
                                  DailyArticleSelectionRepository selectionRepository,
                                  ArticleSummaryRepository summaryRepository,
                                  InterestCategoryRepository categoryRepository,
                                  NamedParameterJdbcTemplate jdbc) {
        this.coordinator = coordinator;
        this.articleFeedService = articleFeedService;
        this.statsService = statsService;
        this.runRepository = runRepository;
        this.selectionRepository = selectionRepository;
        this.summaryRepository = summaryRepository;
        this.categoryRepository = categoryRepository;
        this.jdbc = jdbc;
    }

    // --- Core Revamped Intelligence Endpoints ---

    @GetMapping("/status")
    public ResponseEntity<IntelligenceStatusDto> getStatus() {
        return ResponseEntity.ok(coordinator.getStatus());
    }

    @PostMapping("/refresh")
    public ResponseEntity<IntelligenceCoordinator.ExecutionOutcome> refreshIntelligence() {
        IntelligenceCoordinator.ExecutionOutcome outcome = coordinator.triggerManualGeneration();
        return ResponseEntity.ok(outcome);
    }

    @PostMapping("/generate")
    public ResponseEntity<IntelligenceCoordinator.ExecutionOutcome> generateIntelligence() {
        return refreshIntelligence();
    }

    @GetMapping("/articles")
    public ResponseEntity<Page<ArticleFeedItemDto>> getArticles(
            @RequestParam(required = false) String area,
            @RequestParam(required = false) UUID feedId,
            @RequestParam(required = false) Boolean bookmarked,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ArticleFeedItemDto> articles = articleFeedService.getArticles(area, feedId, bookmarked, search, page, size);
        return ResponseEntity.ok(articles);
    }

    @PutMapping("/articles/{id}/bookmark")
    public ResponseEntity<ArticleFeedItemDto> toggleArticleBookmark(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        boolean bookmarked = Boolean.TRUE.equals(body.get("bookmarked"));
        return ResponseEntity.ok(articleFeedService.toggleBookmark(id, bookmarked));
    }

    @PutMapping("/articles/{id}/read")
    public ResponseEntity<ArticleFeedItemDto> toggleArticleRead(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        boolean isRead = Boolean.TRUE.equals(body.get("isRead")) || Boolean.TRUE.equals(body.get("read"));
        return ResponseEntity.ok(articleFeedService.toggleRead(id, isRead));
    }

    @DeleteMapping("/articles/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable UUID id) {
        articleFeedService.deleteArticle(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getDashboardStats(
            @RequestParam(defaultValue = "7days") String period) {
        return ResponseEntity.ok(statsService.getDashboardStatistics(period));
    }

    @GetMapping("/runs")
    public ResponseEntity<List<IntelligenceRun>> getRecentRuns() {
        return ResponseEntity.ok(runRepository.findTop30ByOrderByStartedAtDesc());
    }

    @GetMapping("/areas")
    public ResponseEntity<List<InterestCategory>> getAreas() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }

    @PostMapping("/areas")
    public ResponseEntity<InterestCategory> createArea(@Valid @RequestBody com.monday.app.intelligence.dto.CreateAreaRequest request) {
        String cleanName = request.name().trim();
        String cleanCode = request.code() != null && !request.code().isBlank()
                ? request.code().trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_")
                : cleanName.toUpperCase().replaceAll("[^A-Z0-9]+", "_");

        InterestCategory category = categoryRepository.findByCodeIgnoreCase(cleanCode)
                .or(() -> categoryRepository.findByNameIgnoreCase(cleanName))
                .orElseGet(() -> {
                    InterestCategory newCat = new InterestCategory();
                    newCat.setCode(cleanCode);
                    newCat.setName(cleanName);
                    newCat.setDescription(request.description() != null ? request.description().trim() : null);
                    newCat.setStatus("ACTIVE");
                    return categoryRepository.save(newCat);
                });

        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(category);
    }

    // --- Backwards Compatibility Endpoints for Daily Digest & Legacy Radar/Briefs ---

    @GetMapping
    public ResponseEntity<?> getTodaysIntelligence() {
        return getIntelligenceByDate(LocalDate.now());
    }

    @GetMapping("/{date:\\d{4}-\\d{2}-\\d{2}}")
    public ResponseEntity<?> getIntelligenceByDate(@PathVariable LocalDate date) {
        List<InterestCategory> categories = categoryRepository.findAll();
        List<Map<String, Object>> responseCategories = new ArrayList<>();

        for (InterestCategory category : categories) {
            List<DailyArticleSelection> selections = selectionRepository
                    .findBySelectionDateAndCategoryIdOrderByRankAsc(date, category.getId());

            if (selections.isEmpty()) continue;

            List<Map<String, Object>> articles = new ArrayList<>();
            for (DailyArticleSelection selection : selections) {
                Map<String, Object> articleDto = new HashMap<>();
                articleDto.put("rank", selection.getRank());
                articleDto.put("title", selection.getArticle().getTitle());
                articleDto.put("source", selection.getArticle().getAuthor());
                articleDto.put("publishedAt", selection.getArticle().getPublishedAt());
                articleDto.put("url", selection.getArticle().getCanonicalUrl());

                List<ArticleSummary> summaries = summaryRepository.findByArticleId(selection.getArticle().getId());
                if (!summaries.isEmpty()) {
                    articleDto.put("summary", summaries.get(0).getSummary());
                } else {
                    articleDto.put("summary", "Summary not available.");
                }

                articles.add(articleDto);
            }

            Map<String, Object> categoryDto = new HashMap<>();
            categoryDto.put("category", category.getName());
            categoryDto.put("articles", articles);
            responseCategories.add(categoryDto);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("date", date.toString());
        response.put("categories", responseCategories);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/briefs/{id}/bookmark")
    public ResponseEntity<?> updateBookmark(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        boolean bookmarked = Boolean.TRUE.equals(body.get("bookmarked"));
        Instant now = Instant.now();

        String updateSql = """
            UPDATE intelligence_briefs
            SET bookmarked = :bookmarked,
                bookmarked_at = :bookmarkedAt,
                updated_at = :updatedAt
            WHERE id = :id
        """;
        int updated = jdbc.update(updateSql, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("bookmarked", bookmarked)
                .addValue("bookmarkedAt", java.sql.Timestamp.from(now))
                .addValue("updatedAt", java.sql.Timestamp.from(now)));

        if (updated == 0) {
            String insertSql = """
                INSERT INTO intelligence_briefs (id, title, bookmarked, bookmarked_at, created_at, updated_at)
                VALUES (:id, 'Intelligence Brief ' || :id, :bookmarked, :bookmarkedAt, :now, :now)
                ON CONFLICT (id) DO UPDATE SET bookmarked = EXCLUDED.bookmarked, bookmarked_at = EXCLUDED.bookmarked_at
            """;
            jdbc.update(insertSql, new MapSqlParameterSource()
                    .addValue("id", id)
                    .addValue("bookmarked", bookmarked)
                    .addValue("bookmarkedAt", java.sql.Timestamp.from(now))
                    .addValue("now", java.sql.Timestamp.from(now)));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", id);
        response.put("bookmarked", bookmarked);
        response.put("bookmarkedAt", now.toString());
        response.put("updatedAt", now.toString());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/briefs/{id}/read")
    public ResponseEntity<?> updateReadStatus(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        boolean isRead = Boolean.TRUE.equals(body.get("isRead")) || Boolean.TRUE.equals(body.get("read"));
        Instant now = Instant.now();

        String updateSql = """
            UPDATE intelligence_briefs
            SET is_read = :isRead,
                read_at = :readAt,
                updated_at = :updatedAt
            WHERE id = :id
        """;
        int updated = jdbc.update(updateSql, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("isRead", isRead)
                .addValue("readAt", java.sql.Timestamp.from(now))
                .addValue("updatedAt", java.sql.Timestamp.from(now)));

        if (updated == 0) {
            String insertSql = """
                INSERT INTO intelligence_briefs (id, title, is_read, read_at, created_at, updated_at)
                VALUES (:id, 'Intelligence Brief ' || :id, :isRead, :readAt, :now, :now)
                ON CONFLICT (id) DO UPDATE SET is_read = EXCLUDED.is_read, read_at = EXCLUDED.read_at
            """;
            jdbc.update(insertSql, new MapSqlParameterSource()
                    .addValue("id", id)
                    .addValue("isRead", isRead)
                    .addValue("readAt", java.sql.Timestamp.from(now))
                    .addValue("now", java.sql.Timestamp.from(now)));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", id);
        response.put("isRead", isRead);
        response.put("read", isRead);
        response.put("readAt", now.toString());
        response.put("updatedAt", now.toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/radar")
    public ResponseEntity<?> getHorizonRadar() {
        String sql = """
            SELECT id, title, category,
                   COALESCE(radar_x, 50.0) as "xPercent",
                   COALESCE(radar_y, 50.0) as "yPercent",
                   COALESCE(gravity, 'MEDIUM') as gravity,
                   COALESCE(urgency_score, 75) as "urgencyScore",
                   id as "linkedBriefId",
                   COALESCE(color, 'cyan') as color
            FROM intelligence_briefs
            WHERE radar_x IS NOT NULL AND radar_y IS NOT NULL
            ORDER BY urgency_score DESC
        """;
        List<Map<String, Object>> blips = jdbc.queryForList(sql, new MapSqlParameterSource());

        for (Map<String, Object> blip : blips) {
            Number x = (Number) blip.get("xPercent");
            Number y = (Number) blip.get("yPercent");
            double xVal = x != null ? Math.max(5.0, Math.min(95.0, x.doubleValue())) : 50.0;
            double yVal = y != null ? Math.max(5.0, Math.min(95.0, y.doubleValue())) : 50.0;
            blip.put("xPercent", xVal);
            blip.put("yPercent", yVal);
        }

        java.time.LocalDate today = java.time.LocalDate.now();
        int weekNum = today.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        String cycle = today.getYear() + "-W" + (weekNum < 10 ? "0" + weekNum : weekNum);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("cycle", cycle);
        response.put("totalBlips", blips.size());
        response.put("blips", blips);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/briefs")
    public ResponseEntity<?> getAllBriefs() {
        String sql = """
            SELECT id, title, topic_badge as "topicBadge", impact_badge as "impactBadge",
                   impact_color as "impactColor", time_ago as "timeAgo", summary,
                   why_it_matters as "whyItMatters", why_it_matters_highlight as "whyItMattersHighlight",
                   source, read_time as "readTime", category,
                   cross_link_label as "crossLinkLabel", cross_link_module as "crossLinkModule",
                   bookmarked, to_char(bookmarked_at, 'YYYY-MM-DD"T"HH24:MI:SS"Z"') as "bookmarkedAt",
                   is_read as "isRead", to_char(read_at, 'YYYY-MM-DD"T"HH24:MI:SS"Z"') as "readAt",
                   radar_x as "radarX", radar_y as "radarY", gravity, urgency_score as "urgencyScore", color
            FROM intelligence_briefs
            ORDER BY created_at ASC
        """;
        List<Map<String, Object>> briefs = jdbc.queryForList(sql, new MapSqlParameterSource());
        return ResponseEntity.ok(briefs);
    }

    @GetMapping("/briefs/{id}")
    public ResponseEntity<?> getBriefById(@PathVariable String id) {
        String sql = """
            SELECT id, title, topic_badge as "topicBadge", impact_badge as "impactBadge",
                   impact_color as "impactColor", time_ago as "timeAgo", summary,
                   why_it_matters as "whyItMatters", why_it_matters_highlight as "whyItMattersHighlight",
                   source, read_time as "readTime", category,
                   cross_link_label as "crossLinkLabel", cross_link_module as "crossLinkModule",
                   bookmarked, to_char(bookmarked_at, 'YYYY-MM-DD"T"HH24:MI:SS"Z"') as "bookmarkedAt",
                   is_read as "isRead", to_char(read_at, 'YYYY-MM-DD"T"HH24:MI:SS"Z"') as "readAt",
                   radar_x as "radarX", radar_y as "radarY", gravity, urgency_score as "urgencyScore", color
            FROM intelligence_briefs
            WHERE id = :id
        """;
        List<Map<String, Object>> briefs = jdbc.queryForList(sql, new MapSqlParameterSource("id", id));
        if (briefs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(briefs.get(0));
    }
}
