package com.monday.app.intelligence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "articles")
public class Article {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "canonical_url", nullable = false, length = 1000)
    private String canonicalUrl;

    @Column(name = "original_url", nullable = false, length = 1000)
    private String originalUrl;

    @Column(nullable = false, length = 1000)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "content_text", columnDefinition = "TEXT")
    private String contentText;

    private String author;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "area_of_interest")
    private String areaOfInterest = "TECHNOLOGY";

    @Column(name = "processing_status", nullable = false)
    private String processingStatus = "DISCOVERED";

    @Column(nullable = false)
    private Boolean bookmarked = false;

    @Column(name = "bookmarked_at")
    private OffsetDateTime bookmarkedAt;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @Column(name = "duplicate_of_id")
    private UUID duplicateOfId;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "first_seen_at", nullable = false)
    private OffsetDateTime firstSeenAt = OffsetDateTime.now();

    @Column(name = "last_seen_at", nullable = false)
    private OffsetDateTime lastSeenAt = OffsetDateTime.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    // Getters and Setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getCanonicalUrl() { return canonicalUrl; }
    public void setCanonicalUrl(String canonicalUrl) { this.canonicalUrl = canonicalUrl; }
    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getContentText() { return contentText; }
    public void setContentText(String contentText) { this.contentText = contentText; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public OffsetDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(OffsetDateTime publishedAt) { this.publishedAt = publishedAt; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public String getAreaOfInterest() { return areaOfInterest; }
    public void setAreaOfInterest(String areaOfInterest) { this.areaOfInterest = areaOfInterest; }
    public String getProcessingStatus() { return processingStatus; }
    public void setProcessingStatus(String processingStatus) { this.processingStatus = processingStatus; }
    public Boolean getBookmarked() { return bookmarked; }
    public void setBookmarked(Boolean bookmarked) { this.bookmarked = bookmarked; }
    public OffsetDateTime getBookmarkedAt() { return bookmarkedAt; }
    public void setBookmarkedAt(OffsetDateTime bookmarkedAt) { this.bookmarkedAt = bookmarkedAt; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
    public OffsetDateTime getReadAt() { return readAt; }
    public void setReadAt(OffsetDateTime readAt) { this.readAt = readAt; }
    public UUID getDuplicateOfId() { return duplicateOfId; }
    public void setDuplicateOfId(UUID duplicateOfId) { this.duplicateOfId = duplicateOfId; }
    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }
    public OffsetDateTime getFirstSeenAt() { return firstSeenAt; }
    public void setFirstSeenAt(OffsetDateTime firstSeenAt) { this.firstSeenAt = firstSeenAt; }
    public OffsetDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(OffsetDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
