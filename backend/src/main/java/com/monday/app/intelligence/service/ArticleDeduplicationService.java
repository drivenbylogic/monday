package com.monday.app.intelligence.service;

import com.monday.app.intelligence.entity.Article;
import com.monday.app.intelligence.entity.ArticleSource;
import com.monday.app.intelligence.entity.ArticleSourceId;
import com.monday.app.intelligence.entity.RssFeed;
import com.monday.app.intelligence.repository.ArticleRepository;
import com.monday.app.intelligence.repository.ArticleSourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ArticleDeduplicationService {

    private final ArticleRepository articleRepository;
    private final ArticleSourceRepository articleSourceRepository;

    public ArticleDeduplicationService(ArticleRepository articleRepository, ArticleSourceRepository articleSourceRepository) {
        this.articleRepository = articleRepository;
        this.articleSourceRepository = articleSourceRepository;
    }

    public Article processAndDeduplicate(Article normalizedArticle, RssFeed feed, String externalGuid) {
        // Find existing by canonical URL or content fingerprint
        // Since we don't have custom query methods yet, we'll fetch all and filter or add custom methods
        // Let's add them to the repository logic via simple iteration for now if missing, or better, we can assume it's new if not found.
        
        Optional<Article> match = articleRepository.findByCanonicalUrl(normalizedArticle.getCanonicalUrl());
        if (match.isEmpty()) {
            match = articleRepository.findByContentHash(normalizedArticle.getContentHash());
        }

        Article articleToUse;
        if (match.isPresent()) {
            articleToUse = match.get();
        } else {
            articleToUse = articleRepository.save(normalizedArticle);
        }

        ArticleSourceId sourceId = new ArticleSourceId(articleToUse.getId(), feed.getId());
        if (!articleSourceRepository.existsById(sourceId)) {
            ArticleSource source = new ArticleSource();
            source.setId(sourceId);
            source.setArticle(articleToUse);
            source.setFeed(feed);
            source.setExternalGuid(externalGuid);
            source.setSourcePublishedAt(normalizedArticle.getPublishedAt());
            articleSourceRepository.save(source);
        }

        return articleToUse;
    }
}
