package com.monday.app.intelligence.repository;

import com.monday.app.intelligence.entity.ArticleClassification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ArticleClassificationRepository extends JpaRepository<ArticleClassification, UUID> {
    List<ArticleClassification> findByArticleId(UUID articleId);
    List<ArticleClassification> findByCategoryIdAndClassifiedAtGreaterThanEqual(UUID categoryId, java.time.OffsetDateTime since);
}
