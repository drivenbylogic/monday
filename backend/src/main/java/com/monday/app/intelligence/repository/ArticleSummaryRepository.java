package com.monday.app.intelligence.repository;

import com.monday.app.intelligence.entity.ArticleSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArticleSummaryRepository extends JpaRepository<ArticleSummary, UUID> {

    List<ArticleSummary> findByArticleId(UUID articleId);

    Optional<ArticleSummary> findFirstByArticleIdAndStatusOrderByCreatedAtDesc(UUID articleId, String status);

    List<ArticleSummary> findByArticleIdInAndStatus(List<UUID> articleIds, String status);

    @Query(value = "SELECT DISTINCT ON (article_id) * FROM article_summaries WHERE article_id IN (:articleIds) AND status = :status ORDER BY article_id, created_at DESC, id DESC", nativeQuery = true)
    List<ArticleSummary> findLatestSummariesByArticleIdsAndStatus(@Param("articleIds") Collection<UUID> articleIds, @Param("status") String status);

    @Modifying
    @Query("DELETE FROM ArticleSummary s WHERE s.createdAt < :cutoff")
    int deleteSummariesOlderThan(@Param("cutoff") OffsetDateTime cutoff);
}
