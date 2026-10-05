package com.monday.app.intelligence.controller;

import com.monday.app.intelligence.entity.ArticleSummary;
import com.monday.app.intelligence.entity.DailyArticleSelection;
import com.monday.app.intelligence.entity.InterestCategory;
import com.monday.app.intelligence.repository.ArticleSummaryRepository;
import com.monday.app.intelligence.repository.DailyArticleSelectionRepository;
import com.monday.app.intelligence.repository.InterestCategoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/intelligence")
public class IntelligenceController {

    private final DailyArticleSelectionRepository selectionRepository;
    private final ArticleSummaryRepository summaryRepository;
    private final InterestCategoryRepository categoryRepository;

    public IntelligenceController(DailyArticleSelectionRepository selectionRepository,
                                  ArticleSummaryRepository summaryRepository,
                                  InterestCategoryRepository categoryRepository) {
        this.selectionRepository = selectionRepository;
        this.summaryRepository = summaryRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public ResponseEntity<?> getTodaysIntelligence() {
        return getIntelligenceByDate(LocalDate.now());
    }

    @GetMapping("/{date}")
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
}
