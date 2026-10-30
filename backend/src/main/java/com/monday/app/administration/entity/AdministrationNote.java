package com.monday.app.administration.entity;

import java.time.Instant;
import java.util.UUID;

public class AdministrationNote {

    private UUID id;
    private String title;
    private String body;
    private Instant createdAt;
    private Instant updatedAt;

    public AdministrationNote() {}

    public AdministrationNote(UUID id, String title, String body, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
