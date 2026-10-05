package com.monday.app.intelligence.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.app.intelligence.entity.Article;
import com.monday.app.intelligence.entity.ArticleClassification;
import com.monday.app.intelligence.entity.InterestCategory;
import com.monday.app.intelligence.repository.ArticleClassificationRepository;
import com.monday.app.intelligence.repository.InterestCategoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class ArticleClassifier {

    private final InterestCategoryRepository categoryRepository;
    private final ArticleClassificationRepository classificationRepository;
    private final ObjectMapper objectMapper;

    @Value("${intelligence.classification.threshold:0.5}")
    private double threshold;

    public ArticleClassifier(InterestCategoryRepository categoryRepository,
                             ArticleClassificationRepository classificationRepository,
                             ObjectMapper objectMapper) {
        this.categoryRepository = categoryRepository;
        this.classificationRepository = classificationRepository;
        this.objectMapper = objectMapper;
    }

    public void classifyAndPersist(Article article) {
        List<InterestCategory> categories = categoryRepository.findAll();
        ArticleClassification primary = null;

        for (InterestCategory category : categories) {
            double score = calculateScore(article, category);
            if (score >= threshold) {
                ArticleClassification classification = new ArticleClassification();
                classification.setArticle(article);
                classification.setCategory(category);
                classification.setScore(BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP));
                classification.setIsPrimary(false);
                classification.setMatchedSignals("[]"); // simplified for now
                classificationRepository.save(classification);

                if (primary == null || primary.getScore().compareTo(classification.getScore()) < 0) {
                    primary = classification;
                }
            }
        }

        if (primary != null) {
            primary.setIsPrimary(true);
            classificationRepository.save(primary);
        }
    }

    private double calculateScore(Article article, InterestCategory category) {
        Map<String, List<String>> config = parseConfig(category.getClassificationConfig());
        if (config == null || config.isEmpty()) {
            return fallbackScore(article, category);
        }
        
        String text = (article.getTitle() + " " + article.getContentText()).toLowerCase();
        double score = 0.0;

        List<String> strong = config.get("strong");
        if (strong != null) {
            for (String kw : strong) {
                if (text.contains(kw.toLowerCase())) score += 0.4;
            }
        }

        List<String> medium = config.get("medium");
        if (medium != null) {
            for (String kw : medium) {
                if (text.contains(kw.toLowerCase())) score += 0.2;
            }
        }

        List<String> negative = config.get("negative");
        if (negative != null) {
            for (String kw : negative) {
                if (text.contains(kw.toLowerCase())) score -= 0.5;
            }
        }

        return Math.max(0.0, Math.min(1.0, score));
    }

    private Map<String, List<String>> parseConfig(String configJson) {
        if (configJson == null || configJson.isBlank()) return null;
        try {
            return objectMapper.readValue(configJson, new TypeReference<Map<String, List<String>>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private double fallbackScore(Article article, InterestCategory category) {
        // Fallback: simple exact match on category name
        String text = (article.getTitle() + " " + article.getContentText()).toLowerCase();
        if (text.contains(category.getName().toLowerCase())) {
            return 0.6;
        }
        // Artificial Intelligence -> check "ai "
        if (category.getCode().equals("ARTIFICIAL_INTELLIGENCE")) {
            if (text.contains(" ai ") || text.contains("artificial intelligence") || text.contains("llm")) return 0.8;
        }
        return 0.0;
    }
}
