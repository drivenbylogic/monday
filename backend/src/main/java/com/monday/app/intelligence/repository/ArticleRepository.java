package com.monday.app.intelligence.repository;

import com.monday.app.intelligence.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArticleRepository extends JpaRepository<Article, UUID> {

    Optional<Article> findByCanonicalUrl(String canonicalUrl);

    Optional<Article> findByContentHash(String contentHash);

    boolean existsByCanonicalUrl(String canonicalUrl);

    boolean existsByContentHash(String contentHash);

    List<Article> findByCreatedAtAfter(OffsetDateTime cutoff);

    List<Article> findByProcessingStatus(String processingStatus);

    @Query("SELECT a FROM Article a WHERE a.isDeleted = false AND a.createdAt >= :cutoff ORDER BY a.publishedAt DESC NULLS LAST, a.firstSeenAt DESC")
    List<Article> findRecentArticles(@Param("cutoff") OffsetDateTime cutoff);

    @Query("""
        SELECT DISTINCT a FROM Article a
        LEFT JOIN ArticleSource s ON s.article.id = a.id
        WHERE a.isDeleted = false
          AND (:area IS NULL OR :area = '' OR 
               LOWER(a.areaOfInterest) = LOWER(:area) OR 
               LOWER(s.feed.areaOfInterest) = LOWER(:area))
          AND (:feedId IS NULL OR s.feed.id = :feedId)
          AND (:bookmarked IS NULL OR a.bookmarked = :bookmarked)
          AND (:search IS NULL OR :search = '' OR 
               LOWER(a.title) LIKE LOWER(CONCAT('%', :search, '%')) OR 
               LOWER(a.description) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(a.author) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY a.publishedAt DESC NULLS LAST, a.firstSeenAt DESC
    """)
    Page<Article> searchArticles(
            @Param("area") String area,
            @Param("feedId") UUID feedId,
            @Param("bookmarked") Boolean bookmarked,
            @Param("search") String search,
            Pageable pageable
    );

    @Modifying
    @Query("DELETE FROM Article a WHERE a.createdAt < :cutoff")
    int deleteArticlesOlderThan(@Param("cutoff") OffsetDateTime cutoff);
}
