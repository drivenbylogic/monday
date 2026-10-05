package com.monday.app.intelligence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "intelligence_runs")
public class IntelligenceRun {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "run_date", nullable = false)
    private LocalDate runDate;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "feeds_attempted", nullable = false)
    private Integer feedsAttempted = 0;

    @Column(name = "feeds_succeeded", nullable = false)
    private Integer feedsSucceeded = 0;

    @Column(name = "feeds_failed", nullable = false)
    private Integer feedsFailed = 0;

    @Column(name = "articles_seen", nullable = false)
    private Integer articlesSeen = 0;

    @Column(name = "articles_new", nullable = false)
    private Integer articlesNew = 0;

    @Column(name = "articles_deduplicated", nullable = false)
    private Integer articlesDeduplicated = 0;

    @Column(name = "articles_classified", nullable = false)
    private Integer articlesClassified = 0;

    @Column(name = "articles_selected", nullable = false)
    private Integer articlesSelected = 0;

    @Column(name = "summaries_generated", nullable = false)
    private Integer summariesGenerated = 0;

    @Column(name = "summaries_failed", nullable = false)
    private Integer summariesFailed = 0;

    @Column(name = "failure_message", columnDefinition = "TEXT")
    private String failureMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    // Getters and Setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public LocalDate getRunDate() { return runDate; }
    public void setRunDate(LocalDate runDate) { this.runDate = runDate; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getFeedsAttempted() { return feedsAttempted; }
    public void setFeedsAttempted(Integer feedsAttempted) { this.feedsAttempted = feedsAttempted; }
    public Integer getFeedsSucceeded() { return feedsSucceeded; }
    public void setFeedsSucceeded(Integer feedsSucceeded) { this.feedsSucceeded = feedsSucceeded; }
    public Integer getFeedsFailed() { return feedsFailed; }
    public void setFeedsFailed(Integer feedsFailed) { this.feedsFailed = feedsFailed; }
    public Integer getArticlesSeen() { return articlesSeen; }
    public void setArticlesSeen(Integer articlesSeen) { this.articlesSeen = articlesSeen; }
    public Integer getArticlesNew() { return articlesNew; }
    public void setArticlesNew(Integer articlesNew) { this.articlesNew = articlesNew; }
    public Integer getArticlesDeduplicated() { return articlesDeduplicated; }
    public void setArticlesDeduplicated(Integer articlesDeduplicated) { this.articlesDeduplicated = articlesDeduplicated; }
    public Integer getArticlesClassified() { return articlesClassified; }
    public void setArticlesClassified(Integer articlesClassified) { this.articlesClassified = articlesClassified; }
    public Integer getArticlesSelected() { return articlesSelected; }
    public void setArticlesSelected(Integer articlesSelected) { this.articlesSelected = articlesSelected; }
    public Integer getSummariesGenerated() { return summariesGenerated; }
    public void setSummariesGenerated(Integer summariesGenerated) { this.summariesGenerated = summariesGenerated; }
    public Integer getSummariesFailed() { return summariesFailed; }
    public void setSummariesFailed(Integer summariesFailed) { this.summariesFailed = summariesFailed; }
    public String getFailureMessage() { return failureMessage; }
    public void setFailureMessage(String failureMessage) { this.failureMessage = failureMessage; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
