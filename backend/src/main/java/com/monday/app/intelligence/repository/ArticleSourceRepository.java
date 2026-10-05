package com.monday.app.intelligence.repository;

import com.monday.app.intelligence.entity.ArticleSource;
import com.monday.app.intelligence.entity.ArticleSourceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleSourceRepository extends JpaRepository<ArticleSource, ArticleSourceId> {
}
