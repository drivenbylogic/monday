package com.monday.app.intelligence.repository;

import com.monday.app.intelligence.entity.DailyArticleSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface DailyArticleSelectionRepository extends JpaRepository<DailyArticleSelection, UUID> {
    List<DailyArticleSelection> findBySelectionDateAndCategoryIdOrderByRankAsc(LocalDate selectionDate, UUID categoryId);
    void deleteBySelectionDateBefore(LocalDate cutoffDate);
}
