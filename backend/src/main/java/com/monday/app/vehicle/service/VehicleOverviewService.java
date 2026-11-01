package com.monday.app.vehicle.service;

import com.monday.app.shared.cache.CacheNames;
import com.monday.app.shared.exception.ResourceNotFoundException;
import com.monday.app.vehicle.dto.*;
import com.monday.app.vehicle.entity.*;
import com.monday.app.vehicle.repository.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class VehicleOverviewService {

    private final VehicleRepository vehicleRepository;
    private final VehicleDocumentRepository documentRepository;
    private final VehicleInsuranceRepository insuranceRepository;
    private final VehicleServiceRecordRepository serviceRecordRepository;
    private final VehicleMaintenanceScheduleRepository scheduleRepository;
    private final VehicleFuelLogRepository fuelLogRepository;
    private final VehicleTaskRepository taskRepository;
    private final VehicleObligationRepository obligationRepository;
    private final VehicleTimelineService timelineService;

    public VehicleOverviewService(VehicleRepository vehicleRepository,
                                  VehicleDocumentRepository documentRepository,
                                  VehicleInsuranceRepository insuranceRepository,
                                  VehicleServiceRecordRepository serviceRecordRepository,
                                  VehicleMaintenanceScheduleRepository scheduleRepository,
                                  VehicleFuelLogRepository fuelLogRepository,
                                  VehicleTaskRepository taskRepository,
                                  VehicleObligationRepository obligationRepository,
                                  VehicleTimelineService timelineService) {
        this.vehicleRepository = vehicleRepository;
        this.documentRepository = documentRepository;
        this.insuranceRepository = insuranceRepository;
        this.serviceRecordRepository = serviceRecordRepository;
        this.scheduleRepository = scheduleRepository;
        this.fuelLogRepository = fuelLogRepository;
        this.taskRepository = taskRepository;
        this.obligationRepository = obligationRepository;
        this.timelineService = timelineService;
    }

    @Cacheable(value = CacheNames.VEHICLE_OVERVIEW, keyGenerator = "userAwareKeyGenerator", sync = true)
    public VehicleOverviewDto getOverview(UUID vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));

        LocalDate today = LocalDate.now();
        String currentYm = today.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        // 1. Vehicle Summary
        VehicleSummaryDto summary = new VehicleSummaryDto(
                vehicle.getId(),
                vehicle.getName(),
                vehicle.getEdition(),
                vehicle.getVariant(),
                vehicle.getType(),
                vehicle.getRegistrationNumber(),
                vehicle.getIsPrimary(),
                vehicle.getCurrentKm(),
                vehicle.getPurchaseDate(),
                vehicle.getPurchaseAmount(),
                vehicle.getStatus(),
                vehicle.getHeroImageUrl(),
                vehicle.getVin(),
                vehicle.getEmissionStandard(),
                vehicle.getRegisteredOwner(),
                vehicle.getRegisteringAuthority()
        );

        // 2. Schedules
        List<VehicleMaintenanceSchedule> schedules = scheduleRepository.findByVehicleId(vehicleId);
        int maintDueCount = 0;
        int maintOverdueCount = 0;
        List<NeedsAttentionItemDto> attentionItems = new ArrayList<>();
        List<TaskReminderItemDto> tasksAndReminders = new ArrayList<>();

        for (VehicleMaintenanceSchedule s : schedules) {
            boolean overdue = false;
            boolean dueSoon = false;
            Long days = s.getNextDueAt() != null ? ChronoUnit.DAYS.between(today, s.getNextDueAt()) : null;
            Integer km = s.getNextDueKm() != null ? s.getNextDueKm() - vehicle.getCurrentKm() : null;

            if ((days != null && days < 0) || (km != null && km < 0)) {
                overdue = true;
                maintOverdueCount++;
            } else if ((days != null && days <= 30) || (km != null && km <= 1000)) {
                dueSoon = true;
                maintDueCount++;
            }

            if (overdue || dueSoon) {
                String threshold = (days != null ? (days < 0 ? Math.abs(days) + " days overdue" : "Due in " + days + " days") : "") +
                        (km != null ? " • " + (km < 0 ? Math.abs(km) + " km overdue" : "In " + km + " km") : "");

                attentionItems.add(new NeedsAttentionItemDto(
                        "maint-" + s.getId(),
                        "MAINTENANCE",
                        s.getItemName(),
                        overdue ? "Maintenance cycle is overdue" : "Maintenance cycle due soon",
                        threshold.trim(),
                        overdue ? "HIGH" : "MEDIUM",
                        "VIEW_MAINTENANCE",
                        s.getId().toString(),
                        s.getNextDueAt(),
                        s.getNextDueKm()
                ));

                tasksAndReminders.add(new TaskReminderItemDto(
                        "rem-maint-" + s.getId(),
                        "MAINTENANCE",
                        s.getItemName(),
                        threshold.trim(),
                        overdue ? "OVERDUE" : "DUE_SOON",
                        s.getNextDueAt(),
                        s.getNextDueKm(),
                        null,
                        overdue ? "HIGH" : "MEDIUM",
                        false
                ));
            }
        }

        // 3. Documents & Expiries
        List<VehicleDocument> docs = documentRepository.findByVehicleId(vehicleId);
        int docsExpiring = 0;
        int docsExpired = 0;

        for (VehicleDocument d : docs) {
            if (d.getExpiryDate() != null) {
                long days = ChronoUnit.DAYS.between(today, d.getExpiryDate());
                if (days < 0) {
                    docsExpired++;
                    attentionItems.add(new NeedsAttentionItemDto(
                            "doc-" + d.getId(),
                            "DOCUMENT",
                            d.getTitle(),
                            "Document expired",
                            Math.abs(days) + " days ago (" + d.getExpiryDate() + ")",
                            "HIGH",
                            "VIEW_DOCUMENTS",
                            d.getId().toString(),
                            d.getExpiryDate(),
                            null
                    ));
                    tasksAndReminders.add(new TaskReminderItemDto(
                            "rem-doc-" + d.getId(),
                            "DOCUMENT",
                            d.getTitle(),
                            "Document expired " + Math.abs(days) + " days ago",
                            "EXPIRED",
                            d.getExpiryDate(),
                            null,
                            null,
                            "HIGH",
                            false
                    ));
                } else if (days <= 30) {
                    docsExpiring++;
                    attentionItems.add(new NeedsAttentionItemDto(
                            "doc-" + d.getId(),
                            "DOCUMENT",
                            d.getTitle(),
                            "Document expiring soon",
                            "Expires in " + days + " days (" + d.getExpiryDate() + ")",
                            days <= 7 ? "HIGH" : "MEDIUM",
                            "VIEW_DOCUMENTS",
                            d.getId().toString(),
                            d.getExpiryDate(),
                            null
                    ));
                    tasksAndReminders.add(new TaskReminderItemDto(
                            "rem-doc-" + d.getId(),
                            "DOCUMENT",
                            d.getTitle(),
                            "Expires in " + days + " days",
                            "EXPIRING_SOON",
                            d.getExpiryDate(),
                            null,
                            null,
                            days <= 7 ? "HIGH" : "MEDIUM",
                            false
                    ));
                }
            }
        }

        // 4. Insurance
        List<VehicleInsurance> insurances = insuranceRepository.findByVehicleId(vehicleId);
        boolean insRenewalDue = false;
        Integer insDaysRemaining = null;

        if (!insurances.isEmpty()) {
            VehicleInsurance latestIns = insurances.get(0);
            if (latestIns.getDueDate() != null) {
                long days = ChronoUnit.DAYS.between(today, latestIns.getDueDate());
                insDaysRemaining = (int) days;
                if (days <= 30) {
                    insRenewalDue = true;
                    if (!Boolean.TRUE.equals(latestIns.getIsRenewed())) {
                        attentionItems.add(new NeedsAttentionItemDto(
                                "ins-" + latestIns.getId(),
                                "INSURANCE",
                                latestIns.getPolicyName() != null ? latestIns.getPolicyName() : "Insurance Policy",
                                days < 0 ? "Insurance expired" : "Insurance renewal due soon",
                                days < 0 ? Math.abs(days) + " days overdue" : "Renewal due in " + days + " days",
                                "HIGH",
                                "VIEW_INSURANCE",
                                latestIns.getId().toString(),
                                latestIns.getDueDate(),
                                null
                        ));
                    }
                }
            }
        }

        // 5. Tasks
        List<VehicleTask> tasks = taskRepository.findByVehicleId(vehicleId);
        int pendingTasksCount = 0;
        for (VehicleTask t : tasks) {
            if (!Boolean.TRUE.equals(t.getCompleted())) {
                pendingTasksCount++;
                tasksAndReminders.add(new TaskReminderItemDto(
                        "task-" + t.getId(),
                        "TASK",
                        t.getTitle(),
                        t.getMetaText() != null ? t.getMetaText() : "Pending task",
                        "PENDING",
                        null,
                        null,
                        null,
                        t.getPriority(),
                        false
                ));
            }
        }

        // 6. Obligations
        List<VehicleObligation> obligations = obligationRepository.findByVehicleId(vehicleId);
        int pendingObligationsCount = 0;
        for (VehicleObligation o : obligations) {
            if (!"COMPLETED".equalsIgnoreCase(o.getStatus()) && !"PAID".equalsIgnoreCase(o.getStatus())) {
                pendingObligationsCount++;
                long days = ChronoUnit.DAYS.between(today, o.getDueDate());
                boolean overdue = days < 0;
                boolean dueSoon = days <= 30;

                if (overdue || dueSoon) {
                    attentionItems.add(new NeedsAttentionItemDto(
                            "obl-" + o.getId(),
                            "OBLIGATION",
                            o.getObligationType() + " Payment",
                            overdue ? "Vehicle obligation overdue" : "Vehicle obligation payment due",
                            (overdue ? Math.abs(days) + " days overdue" : "Due in " + days + " days") +
                                    (o.getAmount() != null ? " • ₹" + o.getAmount() : ""),
                            overdue ? "HIGH" : "MEDIUM",
                            "VIEW_TASKS",
                            o.getId().toString(),
                            o.getDueDate(),
                            null
                    ));
                }

                tasksAndReminders.add(new TaskReminderItemDto(
                        "obl-" + o.getId(),
                        "OBLIGATION",
                        o.getObligationType() + " Obligation",
                        (o.getNotes() != null ? o.getNotes() + " • " : "") + "Due: " + o.getDueDate(),
                        overdue ? "OVERDUE" : "PENDING",
                        o.getDueDate(),
                        null,
                        o.getAmount(),
                        overdue ? "HIGH" : "MEDIUM",
                        false
                ));
            }
        }

        // 7. Costs this month
        List<VehicleFuelLog> fuelLogs = fuelLogRepository.findByVehicleId(vehicleId);
        BigDecimal fuelThisMonth = fuelLogs.stream()
                .filter(l -> l.getLogDate().format(DateTimeFormatter.ofPattern("yyyy-MM")).equals(currentYm))
                .map(l -> l.getTotalCost() != null ? l.getTotalCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<VehicleServiceRecord> serviceRecords = serviceRecordRepository.findByVehicleId(vehicleId);
        BigDecimal serviceThisMonth = serviceRecords.stream()
                .filter(s -> s.getServiceDate().format(DateTimeFormatter.ofPattern("yyyy-MM")).equals(currentYm))
                .map(s -> s.getCost() != null ? s.getCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpensesThisMonth = fuelThisMonth.add(serviceThisMonth);

        // Latest Fuel Efficiency
        Double latestEfficiency = null;
        for (VehicleFuelLog log : fuelLogs) {
            if (log.getTripEfficiency() != null && log.getTripEfficiency().doubleValue() > 0) {
                latestEfficiency = log.getTripEfficiency().doubleValue();
                break;
            }
        }

        // Metric Card DTO
        MetricCardDto metrics = new MetricCardDto(
                maintDueCount,
                maintOverdueCount,
                docsExpiring,
                docsExpired,
                insRenewalDue,
                insDaysRemaining,
                fuelThisMonth,
                totalExpensesThisMonth,
                vehicle.getCurrentKm(),
                latestEfficiency,
                pendingTasksCount,
                pendingObligationsCount
        );

        // Sort attention items by severity (HIGH first) then dueDate
        attentionItems.sort(Comparator.comparing((NeedsAttentionItemDto a) -> "HIGH".equals(a.severity()) ? 0 : 1)
                .thenComparing(NeedsAttentionItemDto::dueDate, Comparator.nullsLast(Comparator.naturalOrder())));

        // Recent timeline preview (latest 8 items)
        List<VehicleTimelineItemDto> recentTimeline = timelineService.getTimeline(vehicleId, 8);

        return new VehicleOverviewDto(summary, metrics, attentionItems, tasksAndReminders, recentTimeline);
    }
}
