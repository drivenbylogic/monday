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
        List<ImportantDateDto> occasionsToday,

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
) {
    public AdministrationOverviewDto(
            int totalActiveObligations,
            int obligationsDueSoonCount,
            int obligationsOverdueCount,
            BigDecimal monthlySubscriptionCommitment,
            List<ObligationDto> upcomingObligations,
            int totalPeopleCount,
            int upcomingOccasionsCount,
            List<ImportantDateDto> upcomingOccasions,
            int totalGadgetsCount,
            int warrantiesExpiringSoonCount,
            int maintenanceDueCount,
            List<MaintenanceDto> upcomingMaintenance,
            int upcomingAppointmentsCount,
            List<AppointmentDto> upcomingAppointments,
            int activeChecklistsCount,
            int pendingChecklistItemsCount,
            int totalNotesCount,
            List<NoteDto> recentNotes
    ) {
        this(
                totalActiveObligations,
                obligationsDueSoonCount,
                obligationsOverdueCount,
                monthlySubscriptionCommitment,
                upcomingObligations,
                totalPeopleCount,
                upcomingOccasionsCount,
                upcomingOccasions,
                upcomingOccasions != null ? upcomingOccasions.stream().filter(o -> o.daysRemaining() == 0).toList() : List.of(),
                totalGadgetsCount,
                warrantiesExpiringSoonCount,
                maintenanceDueCount,
                upcomingMaintenance,
                upcomingAppointmentsCount,
                upcomingAppointments,
                activeChecklistsCount,
                pendingChecklistItemsCount,
                totalNotesCount,
                recentNotes
        );
    }
}
