package com.monday.app.intelligence.repository;

import com.monday.app.intelligence.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArticleRepository extends JpaRepository<Article, UUID> {
    Optional<Article> findByCanonicalUrl(String canonicalUrl);
    Optional<Article> findByContentHash(String contentHash);
}
