package com.monday.app.intelligence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "article_classifications")
public class ArticleClassification {

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private InterestCategory category;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal score;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    @Column(name = "matched_signals", columnDefinition = "jsonb")
    private String matchedSignals;

    @Column(name = "classified_at", nullable = false, updatable = false)
    private OffsetDateTime classifiedAt = OffsetDateTime.now();

    // Getters and Setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Article getArticle() { return article; }
    public void setArticle(Article article) { this.article = article; }
    public InterestCategory getCategory() { return category; }
    public void setCategory(InterestCategory category) { this.category = category; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public Boolean getIsPrimary() { return isPrimary; }
    public void setIsPrimary(Boolean primary) { isPrimary = primary; }
    public String getMatchedSignals() { return matchedSignals; }
    public void setMatchedSignals(String matchedSignals) { this.matchedSignals = matchedSignals; }
    public OffsetDateTime getClassifiedAt() { return classifiedAt; }
    public void setClassifiedAt(OffsetDateTime classifiedAt) { this.classifiedAt = classifiedAt; }
}
