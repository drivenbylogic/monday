package com.monday.app.intelligence.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import java.time.OffsetDateTime;

@Entity
@Table(name = "article_sources")
public class ArticleSource {

    @EmbeddedId
    private ArticleSourceId id = new ArticleSourceId();

    @ManyToOne
    @MapsId("articleId")
    @JoinColumn(name = "article_id")
    private Article article;

    @ManyToOne
    @MapsId("feedId")
    @JoinColumn(name = "feed_id")
    private RssFeed feed;

    @Column(name = "external_guid", length = 500)
    private String externalGuid;

    @Column(name = "source_published_at")
    private OffsetDateTime sourcePublishedAt;

    @Column(name = "first_seen_at", nullable = false, updatable = false)
    private OffsetDateTime firstSeenAt = OffsetDateTime.now();

    // Getters and Setters

    public ArticleSourceId getId() { return id; }
    public void setId(ArticleSourceId id) { this.id = id; }
    public Article getArticle() { return article; }
    public void setArticle(Article article) { this.article = article; }
    public RssFeed getFeed() { return feed; }
    public void setFeed(RssFeed feed) { this.feed = feed; }
    public String getExternalGuid() { return externalGuid; }
    public void setExternalGuid(String externalGuid) { this.externalGuid = externalGuid; }
    public OffsetDateTime getSourcePublishedAt() { return sourcePublishedAt; }
    public void setSourcePublishedAt(OffsetDateTime sourcePublishedAt) { this.sourcePublishedAt = sourcePublishedAt; }
    public OffsetDateTime getFirstSeenAt() { return firstSeenAt; }
    public void setFirstSeenAt(OffsetDateTime firstSeenAt) { this.firstSeenAt = firstSeenAt; }
}
