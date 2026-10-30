package com.monday.app.intelligence.repository;

import com.monday.app.intelligence.entity.ArticleSource;
import com.monday.app.intelligence.entity.ArticleSourceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ArticleSourceRepository extends JpaRepository<ArticleSource, ArticleSourceId> {
    List<ArticleSource> findByIdArticleId(UUID articleId);
    List<ArticleSource> findByIdFeedId(UUID feedId);
    List<ArticleSource> findByIdArticleIdIn(List<UUID> articleIds);
}
