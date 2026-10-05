package com.monday.app.intelligence.service;

import com.monday.app.intelligence.entity.Article;
import com.monday.app.intelligence.entity.ArticleClassification;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class ArticleRankingService {

    public double calculateRankingScore(ArticleClassification classification, double sourceQuality) {
        Article article = classification.getArticle();
        
        double categoryRelevance = classification.getScore().doubleValue();
        double freshness = calculateFreshness(article.getPublishedAt());
        double significance = calculateSignificance(article);
        double confidence = categoryRelevance; // Base confidence on relevance for now
        
        // Weights
        double wRelevance = 0.35;
        double wSource = 0.25;
        double wFreshness = 0.20;
        double wSignif = 0.10;
        double wConf = 0.10;
        
        return (wRelevance * categoryRelevance) +
               (wSource * sourceQuality) +
               (wFreshness * freshness) +
               (wSignif * significance) +
               (wConf * confidence);
    }

    private double calculateFreshness(OffsetDateTime publishedAt) {
        if (publishedAt == null) return 0.5;
        long hoursOld = ChronoUnit.HOURS.between(publishedAt, OffsetDateTime.now());
        if (hoursOld < 0) hoursOld = 0;
        // exponential decay
        double decay = Math.exp(-0.05 * hoursOld);
        return Math.max(0.0, Math.min(1.0, decay));
    }

    private double calculateSignificance(Article article) {
        // Placeholder for deterministic significance
        // e.g. length of content, specific high-impact words, etc.
        double score = 0.5;
        if (article.getTitle().contains("!")) score += 0.1;
        if (article.getContentText() != null && article.getContentText().length() > 2000) score += 0.2;
        return Math.min(1.0, score);
    }
}
