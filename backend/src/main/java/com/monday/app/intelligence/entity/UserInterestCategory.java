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
@Table(name = "user_interest_categories")
public class UserInterestCategory {

    @EmbeddedId
    private UserInterestCategoryId id = new UserInterestCategoryId();

    @ManyToOne
    @MapsId("categoryId")
    @JoinColumn(name = "category_id")
    private InterestCategory category;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // Getters and Setters

    public UserInterestCategoryId getId() { return id; }
    public void setId(UserInterestCategoryId id) { this.id = id; }
    public InterestCategory getCategory() { return category; }
    public void setCategory(InterestCategory category) { this.category = category; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
