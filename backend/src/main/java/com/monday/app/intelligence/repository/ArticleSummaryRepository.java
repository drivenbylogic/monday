package com.monday.app.intelligence.repository;

import com.monday.app.intelligence.entity.ArticleSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ArticleSummaryRepository extends JpaRepository<ArticleSummary, UUID> {
    List<ArticleSummary> findByArticleId(UUID articleId);
}
