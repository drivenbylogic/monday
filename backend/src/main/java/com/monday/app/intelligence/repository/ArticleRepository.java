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

    // Published date - Newest to Oldest
    @Query(value = """
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
    """,
    countQuery = """
        SELECT count(DISTINCT a) FROM Article a
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
    """)
    Page<Article> searchArticles(
            @Param("area") String area,
            @Param("feedId") UUID feedId,
            @Param("bookmarked") Boolean bookmarked,
            @Param("search") String search,
            Pageable pageable
    );

    // Published date - Oldest to Newest
    @Query(value = """
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
        ORDER BY a.publishedAt ASC NULLS LAST, a.firstSeenAt ASC
    """,
    countQuery = """
        SELECT count(DISTINCT a) FROM Article a
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
    """)
    Page<Article> searchArticlesByPublishedAtAsc(
            @Param("area") String area,
            @Param("feedId") UUID feedId,
            @Param("bookmarked") Boolean bookmarked,
            @Param("search") String search,
            Pageable pageable
    );

    // Added time - Newest to Oldest
    @Query(value = """
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
        ORDER BY a.createdAt DESC, a.firstSeenAt DESC
    """,
    countQuery = """
        SELECT count(DISTINCT a) FROM Article a
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
    """)
    Page<Article> searchArticlesByAddedTime(
            @Param("area") String area,
            @Param("feedId") UUID feedId,
            @Param("bookmarked") Boolean bookmarked,
            @Param("search") String search,
            Pageable pageable
    );

    // Added time - Oldest to Newest
    @Query(value = """
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
        ORDER BY a.createdAt ASC, a.firstSeenAt ASC
    """,
    countQuery = """
        SELECT count(DISTINCT a) FROM Article a
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
    """)
    Page<Article> searchArticlesByAddedTimeAsc(
            @Param("area") String area,
            @Param("feedId") UUID feedId,
            @Param("bookmarked") Boolean bookmarked,
            @Param("search") String search,
            Pageable pageable
    );

    @Modifying
    @Query(value = """
        DELETE FROM article_sources WHERE article_id IN (
            SELECT id FROM articles WHERE (bookmarked = false OR bookmarked IS NULL)
            AND (created_at < :cutoff OR first_seen_at < :cutoff OR (published_at IS NOT NULL AND published_at < :cutoff))
        )
    """, nativeQuery = true)
    int deleteOldArticleSources(@Param("cutoff") OffsetDateTime cutoff);

    @Modifying
    @Query(value = """
        DELETE FROM article_summaries WHERE article_id IN (
            SELECT id FROM articles WHERE (bookmarked = false OR bookmarked IS NULL)
            AND (created_at < :cutoff OR first_seen_at < :cutoff OR (published_at IS NOT NULL AND published_at < :cutoff))
        )
    """, nativeQuery = true)
    int deleteOldArticleSummaries(@Param("cutoff") OffsetDateTime cutoff);

    @Modifying
    @Query(value = """
        DELETE FROM article_classifications WHERE article_id IN (
            SELECT id FROM articles WHERE (bookmarked = false OR bookmarked IS NULL)
            AND (created_at < :cutoff OR first_seen_at < :cutoff OR (published_at IS NOT NULL AND published_at < :cutoff))
        )
    """, nativeQuery = true)
    int deleteOldArticleClassifications(@Param("cutoff") OffsetDateTime cutoff);

    @Modifying
    @Query(value = """
        DELETE FROM daily_article_selections WHERE article_id IN (
            SELECT id FROM articles WHERE (bookmarked = false OR bookmarked IS NULL)
            AND (created_at < :cutoff OR first_seen_at < :cutoff OR (published_at IS NOT NULL AND published_at < :cutoff))
        )
    """, nativeQuery = true)
    int deleteOldDailySelections(@Param("cutoff") OffsetDateTime cutoff);

    @Modifying
    @Query(value = """
        UPDATE articles SET duplicate_of_id = NULL WHERE duplicate_of_id IN (
            SELECT id FROM articles WHERE (bookmarked = false OR bookmarked IS NULL)
            AND (created_at < :cutoff OR first_seen_at < :cutoff OR (published_at IS NOT NULL AND published_at < :cutoff))
        )
    """, nativeQuery = true)
    int clearOldArticleDuplicateRefs(@Param("cutoff") OffsetDateTime cutoff);

    @Modifying
    @Query(value = """
        DELETE FROM articles WHERE (bookmarked = false OR bookmarked IS NULL)
        AND (created_at < :cutoff OR first_seen_at < :cutoff OR (published_at IS NOT NULL AND published_at < :cutoff))
    """, nativeQuery = true)
    int deleteArticlesOlderThan(@Param("cutoff") OffsetDateTime cutoff);
}
