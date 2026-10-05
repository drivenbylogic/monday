package com.monday.app.intelligence.service;

import com.monday.app.intelligence.entity.ArticleClassification;
import com.monday.app.intelligence.entity.DailyArticleSelection;
import com.monday.app.intelligence.entity.InterestCategory;
import com.monday.app.intelligence.repository.ArticleClassificationRepository;
import com.monday.app.intelligence.repository.DailyArticleSelectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class DailySelectionService {

    private final DailyArticleSelectionRepository selectionRepository;

    public DailySelectionService(DailyArticleSelectionRepository selectionRepository) {
        this.selectionRepository = selectionRepository;
    }

    public List<DailyArticleSelection> selectTopArticles(InterestCategory category, 
                                                         List<RankedArticle> rankedArticles,
                                                         LocalDate selectionDate) {
        // Sort DESC by score
        rankedArticles.sort(Comparator.comparingDouble(RankedArticle::score).reversed());
        
        List<DailyArticleSelection> selections = new java.util.ArrayList<>();
        int limit = Math.min(3, rankedArticles.size());
        
        for (int i = 0; i < limit; i++) {
            RankedArticle ranked = rankedArticles.get(i);
            
            DailyArticleSelection selection = new DailyArticleSelection();
            selection.setSelectionDate(selectionDate);
            selection.setArticle(ranked.classification().getArticle());
            selection.setCategory(category);
            selection.setRank(i + 1);
            selection.setRankingScore(BigDecimal.valueOf(ranked.score()).setScale(4, RoundingMode.HALF_UP));
            
            try {
                selections.add(selectionRepository.save(selection));
            } catch (Exception e) {
                // Ignore uniqueness violation on idempotent runs
            }
        }
        
        return selections;
    }

    public record RankedArticle(ArticleClassification classification, double score) {}
}
