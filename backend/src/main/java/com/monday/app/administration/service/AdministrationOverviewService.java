package com.monday.app.administration.service;

import com.monday.app.administration.dto.*;
import com.monday.app.administration.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class AdministrationOverviewService {

    private final AdministrationObligationRepository obligationRepository;
    private final AdministrationObligationService obligationService;
    private final AdministrationPersonRepository personRepository;
    private final AdministrationImportantDateRepository dateRepository;
    private final AdministrationImportantDateService dateService;
    private final AdministrationGadgetRepository gadgetRepository;
    private final AdministrationMaintenanceRepository maintenanceRepository;
    private final AdministrationMaintenanceService maintenanceService;
    private final AdministrationAppointmentService appointmentService;
    private final AdministrationChecklistRepository checklistRepository;
    private final AdministrationNoteRepository noteRepository;
    private final AdministrationNoteService noteService;

    public AdministrationOverviewService(
            AdministrationObligationRepository obligationRepository,
            AdministrationObligationService obligationService,
            AdministrationPersonRepository personRepository,
            AdministrationImportantDateRepository dateRepository,
            AdministrationImportantDateService dateService,
            AdministrationGadgetRepository gadgetRepository,
            AdministrationMaintenanceRepository maintenanceRepository,
            AdministrationMaintenanceService maintenanceService,
            AdministrationAppointmentService appointmentService,
            AdministrationChecklistRepository checklistRepository,
            AdministrationNoteRepository noteRepository,
            AdministrationNoteService noteService) {
        this.obligationRepository = obligationRepository;
        this.obligationService = obligationService;
        this.personRepository = personRepository;
        this.dateRepository = dateRepository;
        this.dateService = dateService;
        this.gadgetRepository = gadgetRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.maintenanceService = maintenanceService;
        this.appointmentService = appointmentService;
        this.checklistRepository = checklistRepository;
        this.noteRepository = noteRepository;
        this.noteService = noteService;
    }

    @Transactional(readOnly = true)
    public AdministrationOverviewDto getOverview() {
        LocalDate today = LocalDate.now();

        // 1. Obligations
        int totalActiveObligations = obligationRepository.countActive();
        int obligationsDueSoonCount = obligationRepository.countDueSoon(today, 7);
        int obligationsOverdueCount = obligationRepository.countOverdue(today);
        BigDecimal monthlySubscriptionCommitment = obligationRepository.sumActiveSubscriptionsMonthly();
        List<ObligationDto> upcomingObligations = obligationRepository.findUpcomingDue(today, today.plusDays(30), 5)
                .stream().map(o -> obligationService.toDto(o, today)).toList();

        // 2. People & Occasions
        int totalPeopleCount = personRepository.countAll();
        List<ImportantDateDto> upcomingOccasions = dateService.getUpcomingOccasions(5);
        int upcomingOccasionsCount = (int) upcomingOccasions.stream()
                .filter(d -> d.daysRemaining() >= 0 && d.daysRemaining() <= 30)
                .count();

        // 3. Gadgets & Maintenance
        int totalGadgetsCount = gadgetRepository.countAll();
        int warrantiesExpiringSoonCount = gadgetRepository.countWarrantiesExpiringSoon(today, 60);
        int maintenanceDueCount = maintenanceRepository.countDue(today);
        List<MaintenanceDto> upcomingMaintenance = maintenanceRepository.findUpcomingDue(today, today.plusDays(60), 5)
                .stream().map(m -> maintenanceService.toDto(m, today)).toList();

        // 4. Appointments
        List<AppointmentDto> upcomingAppointments = appointmentService.getUpcomingAppointments(5);
        int upcomingAppointmentsCount = upcomingAppointments.size();

        // 5. Checklists & Notes
        int activeChecklistsCount = checklistRepository.countActiveChecklists();
        int pendingChecklistItemsCount = checklistRepository.countPendingItems();
        int totalNotesCount = noteRepository.countAll();
        List<NoteDto> recentNotes = noteService.getNotes(null).stream().limit(3).toList();

        return new AdministrationOverviewDto(
                totalActiveObligations,
                obligationsDueSoonCount,
                obligationsOverdueCount,
                monthlySubscriptionCommitment,
                upcomingObligations,
                totalPeopleCount,
                upcomingOccasionsCount,
                upcomingOccasions,
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
