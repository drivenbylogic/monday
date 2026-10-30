package com.monday.app.administration.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdministrationOverviewDto(
        // Obligations
        int totalActiveObligations,
        int obligationsDueSoonCount,
        int obligationsOverdueCount,
        BigDecimal monthlySubscriptionCommitment,
        List<ObligationDto> upcomingObligations,

        // People & Occasions
        int totalPeopleCount,
        int upcomingOccasionsCount,
        List<ImportantDateDto> upcomingOccasions,

        // Gadgets & Maintenance
        int totalGadgetsCount,
        int warrantiesExpiringSoonCount,
        int maintenanceDueCount,
        List<MaintenanceDto> upcomingMaintenance,

        // Appointments
        int upcomingAppointmentsCount,
        List<AppointmentDto> upcomingAppointments,

        // Checklists & Notes
        int activeChecklistsCount,
        int pendingChecklistItemsCount,
        int totalNotesCount,
        List<NoteDto> recentNotes
) {}
