package com.monday.app.intelligence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "rss_feeds")
public class RssFeed {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String name;

    @Column(name = "feed_url", nullable = false, unique = true)
    private String feedUrl;

    @Column(name = "site_url")
    private String siteUrl;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(name = "area_of_interest")
    private String areaOfInterest = "TECHNOLOGY";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    @Column(name = "article_limit", nullable = false)
    private Integer articleLimit = 5;

    @Column(name = "source_quality", nullable = false)
    private Double sourceQuality = 0.5;

    @Column(name = "etag")
    private String etag;

    @Column(name = "last_modified_header")
    private String lastModifiedHeader;

    @Column(name = "last_attempted_at")
    private OffsetDateTime lastAttemptedAt;

    @Column(name = "last_fetch_status")
    private String lastFetchStatus = "IDLE";

    @Column(name = "last_fetched_at")
    private OffsetDateTime lastFetchedAt;

    @Column(name = "last_success_at")
    private OffsetDateTime lastSuccessAt;

    @Column(name = "last_failure_at")
    private OffsetDateTime lastFailureAt;

    @Column(name = "last_failure_code")
    private String lastFailureCode;

    @Column(name = "last_failure_message")
    private String lastFailureMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    // Getters and Setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFeedUrl() { return feedUrl; }
    public void setFeedUrl(String feedUrl) { this.feedUrl = feedUrl; }
    public String getSiteUrl() { return siteUrl; }
    public void setSiteUrl(String siteUrl) { this.siteUrl = siteUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAreaOfInterest() { return areaOfInterest; }
    public void setAreaOfInterest(String areaOfInterest) { this.areaOfInterest = areaOfInterest; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }
    public Integer getArticleLimit() { return articleLimit; }
    public void setArticleLimit(Integer articleLimit) { this.articleLimit = articleLimit; }
    public Double getSourceQuality() { return sourceQuality; }
    public void setSourceQuality(Double sourceQuality) { this.sourceQuality = sourceQuality; }
    public String getEtag() { return etag; }
    public void setEtag(String etag) { this.etag = etag; }
    public String getLastModifiedHeader() { return lastModifiedHeader; }
    public void setLastModifiedHeader(String lastModifiedHeader) { this.lastModifiedHeader = lastModifiedHeader; }
    public OffsetDateTime getLastAttemptedAt() { return lastAttemptedAt; }
    public void setLastAttemptedAt(OffsetDateTime lastAttemptedAt) { this.lastAttemptedAt = lastAttemptedAt; }
    public String getLastFetchStatus() { return lastFetchStatus; }
    public void setLastFetchStatus(String lastFetchStatus) { this.lastFetchStatus = lastFetchStatus; }
    public OffsetDateTime getLastFetchedAt() { return lastFetchedAt; }
    public void setLastFetchedAt(OffsetDateTime lastFetchedAt) { this.lastFetchedAt = lastFetchedAt; }
    public OffsetDateTime getLastSuccessAt() { return lastSuccessAt; }
    public void setLastSuccessAt(OffsetDateTime lastSuccessAt) { this.lastSuccessAt = lastSuccessAt; }
    public OffsetDateTime getLastFailureAt() { return lastFailureAt; }
    public void setLastFailureAt(OffsetDateTime lastFailureAt) { this.lastFailureAt = lastFailureAt; }
    public String getLastFailureCode() { return lastFailureCode; }
    public void setLastFailureCode(String lastFailureCode) { this.lastFailureCode = lastFailureCode; }
    public String getLastFailureMessage() { return lastFailureMessage; }
    public void setLastFailureMessage(String lastFailureMessage) { this.lastFailureMessage = lastFailureMessage; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
