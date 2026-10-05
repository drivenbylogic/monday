package com.monday.app.intelligence.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ArticleSourceId implements Serializable {

    private UUID articleId;
    private UUID feedId;

    public ArticleSourceId() {}

    public ArticleSourceId(UUID articleId, UUID feedId) {
        this.articleId = articleId;
        this.feedId = feedId;
    }

    public UUID getArticleId() { return articleId; }
    public void setArticleId(UUID articleId) { this.articleId = articleId; }

    public UUID getFeedId() { return feedId; }
    public void setFeedId(UUID feedId) { this.feedId = feedId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArticleSourceId that = (ArticleSourceId) o;
        return Objects.equals(articleId, that.articleId) && Objects.equals(feedId, that.feedId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(articleId, feedId);
    }
}
