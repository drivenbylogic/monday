package com.monday.app.administration.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class AdministrationImportantDate {

    private UUID id;
    private String title;
    private String description;
    private LocalDate date;
    private String dateType; // BIRTHDAY, ANNIVERSARY, PERSONAL_MILESTONE, FAMILY_EVENT, OTHER
    private UUID personId;
    private String recurrenceType; // ANNUAL, ONCE, MONTHLY, CUSTOM
    private String recurrenceRule;
    private int reminderLeadDays;
    private Instant archivedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public AdministrationImportantDate() {}

    public AdministrationImportantDate(UUID id, String title, String description, LocalDate date,
                                       String dateType, UUID personId, String recurrenceType,
                                       String recurrenceRule, int reminderLeadDays,
                                       Instant archivedAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.dateType = dateType;
        this.personId = personId;
        this.recurrenceType = recurrenceType;
        this.recurrenceRule = recurrenceRule;
        this.reminderLeadDays = reminderLeadDays;
        this.archivedAt = archivedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getDateType() { return dateType; }
    public void setDateType(String dateType) { this.dateType = dateType; }

    public UUID getPersonId() { return personId; }
    public void setPersonId(UUID personId) { this.personId = personId; }

    public String getRecurrenceType() { return recurrenceType; }
    public void setRecurrenceType(String recurrenceType) { this.recurrenceType = recurrenceType; }

    public String getRecurrenceRule() { return recurrenceRule; }
    public void setRecurrenceRule(String recurrenceRule) { this.recurrenceRule = recurrenceRule; }

    public int getReminderLeadDays() { return reminderLeadDays; }
    public void setReminderLeadDays(int reminderLeadDays) { this.reminderLeadDays = reminderLeadDays; }

    public Instant getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
