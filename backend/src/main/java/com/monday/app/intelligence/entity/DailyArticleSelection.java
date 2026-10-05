package com.monday.app.intelligence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "daily_article_selections")
public class DailyArticleSelection {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "selection_date", nullable = false)
    private LocalDate selectionDate;

    @ManyToOne
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private InterestCategory category;

    @Column(nullable = false)
    private Integer rank;

    @Column(name = "ranking_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal rankingScore;

    @Column(name = "selected_at", nullable = false, updatable = false)
    private OffsetDateTime selectedAt = OffsetDateTime.now();

    // Getters and Setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public LocalDate getSelectionDate() { return selectionDate; }
    public void setSelectionDate(LocalDate selectionDate) { this.selectionDate = selectionDate; }
    public Article getArticle() { return article; }
    public void setArticle(Article article) { this.article = article; }
    public InterestCategory getCategory() { return category; }
    public void setCategory(InterestCategory category) { this.category = category; }
    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }
    public BigDecimal getRankingScore() { return rankingScore; }
    public void setRankingScore(BigDecimal rankingScore) { this.rankingScore = rankingScore; }
    public OffsetDateTime getSelectedAt() { return selectedAt; }
    public void setSelectedAt(OffsetDateTime selectedAt) { this.selectedAt = selectedAt; }
}
