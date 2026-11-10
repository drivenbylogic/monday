package com.monday.app.agenda.service;

import com.monday.app.agenda.dto.*;
import com.monday.app.intelligence.entity.Article;
import com.monday.app.intelligence.repository.ArticleRepository;
import com.monday.app.project.entity.Task;
import com.monday.app.project.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Assembles the compact, bounded, and validated snapshot for the Daily Brief.
 * Adheres strictly to the Daily Brief input contract.
 */
@Service
public class DailyBriefSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(DailyBriefSnapshotService.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final HomeDashboardService dashboardService;
    private final ArticleRepository articleRepository;
    private final TaskRepository taskRepository;

    @Autowired
    public DailyBriefSnapshotService(
            HomeDashboardService dashboardService,
            @Nullable ArticleRepository articleRepository,
            @Nullable TaskRepository taskRepository) {
        this.dashboardService = dashboardService;
        this.articleRepository = articleRepository;
        this.taskRepository = taskRepository;
    }

    public DailyBriefSnapshotDto buildSnapshot(LocalDate date, ZoneId zoneId) {
        if (date == null) {
            date = LocalDate.now(zoneId != null ? zoneId : ZoneId.systemDefault());
        }
        if (zoneId == null) {
            zoneId = ZoneId.of("UTC");
        }

        HomeDashboardResponse dashboard = dashboardService.getDashboard(date, zoneId);

        DailyBriefSnapshotDto.CalendarModule calendar = buildCalendarModule(dashboard, zoneId);
        DailyBriefSnapshotDto.ProjectsModule projects = buildProjectsModule(dashboard, zoneId);
        DailyBriefSnapshotDto.TasksModule tasks = buildTasksModule(dashboard, date, zoneId);
        DailyBriefSnapshotDto.FinancesModule finances = buildFinancesModule(dashboard);
        DailyBriefSnapshotDto.VehiclesModule vehicles = buildVehiclesModule(dashboard);
        DailyBriefSnapshotDto.IntelligenceModule intelligence = buildIntelligenceModule(dashboard);

        DailyBriefSnapshotDto.ModulesSnapshot modules = new DailyBriefSnapshotDto.ModulesSnapshot(
                calendar,
                projects,
                tasks,
                finances,
                vehicles,
                intelligence
        );

        return new DailyBriefSnapshotDto(
                date,
                Instant.now(),
                zoneId.getId(),
                modules
        );
    }

    private DailyBriefSnapshotDto.CalendarModule buildCalendarModule(HomeDashboardResponse dashboard, ZoneId zoneId) {
        try {
            if (dashboard.schedule() == null) {
                return new DailyBriefSnapshotDto.CalendarModule("UNAVAILABLE", Collections.emptyList(), Collections.emptyList());
            }

            Set<UUID> conflictingEventIds = new HashSet<>();
            if (dashboard.schedule().conflicts() != null) {
                for (ScheduleConflictDto conflict : dashboard.schedule().conflicts()) {
                    if (conflict.eventId1() != null) conflictingEventIds.add(conflict.eventId1());
                    if (conflict.eventId2() != null) conflictingEventIds.add(conflict.eventId2());
                }
            }

            List<DailyBriefSnapshotDto.EventBriefItem> events = new ArrayList<>();
            if (dashboard.schedule().eventsToday() != null) {
                for (EventSummaryDto e : dashboard.schedule().eventsToday()) {
                    String startTimeStr = e.startAt() != null ? e.startAt().atZone(zoneId).format(TIME_FORMATTER) : "";
                    String endTimeStr = e.endAt() != null ? e.endAt().atZone(zoneId).format(TIME_FORMATTER) : "";
                    boolean hasConflict = e.id() != null && conflictingEventIds.contains(e.id());
                    String prepNotes = e.prepTitle() != null ? e.prepTitle() : (e.requiresPrep() ? "Preparation required" : "");

                    events.add(new DailyBriefSnapshotDto.EventBriefItem(
                            e.id() != null ? e.id().toString() : UUID.randomUUID().toString(),
                            truncateText(e.title(), 120),
                            startTimeStr,
                            endTimeStr,
                            e.allDay(),
                            truncateText(e.location(), 80),
                            hasConflict,
                            truncateText(prepNotes, 160)
                    ));
                    if (events.size() >= 10) break;
                }
            }

            List<DailyBriefSnapshotDto.CalendarCommitmentBriefItem> commitments = new ArrayList<>();
            if (dashboard.schedule().remindersToday() != null) {
                for (var r : dashboard.schedule().remindersToday()) {
                    commitments.add(new DailyBriefSnapshotDto.CalendarCommitmentBriefItem(
                            r.getId() != null ? r.getId().toString() : UUID.randomUUID().toString(),
                            truncateText(r.getTitle(), 100),
                            r.getRemindAt() != null ? r.getRemindAt().toString() : "",
                            "",
                            "REMINDER"
                    ));
                    if (commitments.size() >= 5) break;
                }
            }

            return new DailyBriefSnapshotDto.CalendarModule("AVAILABLE", events, commitments);
        } catch (Exception e) {
            log.warn("Failed to assemble Calendar module for brief snapshot: {}", e.getMessage());
            return new DailyBriefSnapshotDto.CalendarModule("UNAVAILABLE", Collections.emptyList(), Collections.emptyList());
        }
    }

    private DailyBriefSnapshotDto.ProjectsModule buildProjectsModule(HomeDashboardResponse dashboard, ZoneId zoneId) {
        try {
            if (dashboard.projects() == null) {
                return new DailyBriefSnapshotDto.ProjectsModule("UNAVAILABLE", Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
            }

            List<DailyBriefSnapshotDto.ActiveProjectBriefItem> activeProjects = new ArrayList<>();
            if (dashboard.projects().activeProjects() != null) {
                for (ProjectProgressSummaryDto p : dashboard.projects().activeProjects()) {
                    double progress = p.progressPercent() != null ? p.progressPercent().doubleValue() : 0.0;
                    activeProjects.add(new DailyBriefSnapshotDto.ActiveProjectBriefItem(
                            p.id() != null ? p.id().toString() : UUID.randomUUID().toString(),
                            truncateText(p.name(), 100),
                            p.status(),
                            p.priority(),
                            progress,
                            p.category()
                    ));
                    if (activeProjects.size() >= 5) break;
                }
            }

            List<DailyBriefSnapshotDto.UpcomingMilestoneBriefItem> milestones = new ArrayList<>();
            if (dashboard.projects().upcomingMilestones() != null) {
                for (MilestoneSummaryDto m : dashboard.projects().upcomingMilestones()) {
                    LocalDate dueDate = m.dueAt() != null ? m.dueAt().atZone(zoneId).toLocalDate() : null;
                    milestones.add(new DailyBriefSnapshotDto.UpcomingMilestoneBriefItem(
                            m.id() != null ? m.id().toString() : UUID.randomUUID().toString(),
                            m.projectId() != null ? m.projectId().toString() : "",
                            truncateText(m.projectName(), 80),
                            truncateText(m.name(), 100),
                            dueDate,
                            m.status()
                    ));
                    if (milestones.size() >= 5) break;
                }
            }

            List<DailyBriefSnapshotDto.BlockedBriefItem> blockedItems = new ArrayList<>();
            if (taskRepository != null) {
                try {
                    List<Task> blockedTasks = taskRepository.findByStatus("BLOCKED");
                    for (Task t : blockedTasks) {
                        blockedItems.add(new DailyBriefSnapshotDto.BlockedBriefItem(
                                t.getId() != null ? t.getId().toString() : UUID.randomUUID().toString(),
                                "TASK",
                                truncateText(t.getTitle(), 100),
                                t.getProjectId() != null ? t.getProjectId().toString() : "Direct Task",
                                truncateText(t.getDescription(), 140)
                        ));
                        if (blockedItems.size() >= 5) break;
                    }
                } catch (Exception ex) {
                    log.debug("No blocked tasks retrieved: {}", ex.getMessage());
                }
            }

            return new DailyBriefSnapshotDto.ProjectsModule("AVAILABLE", activeProjects, milestones, blockedItems);
        } catch (Exception e) {
            log.warn("Failed to assemble Projects module for brief snapshot: {}", e.getMessage());
            return new DailyBriefSnapshotDto.ProjectsModule("UNAVAILABLE", Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }
    }

    private DailyBriefSnapshotDto.TasksModule buildTasksModule(HomeDashboardResponse dashboard, LocalDate today, ZoneId zoneId) {
        try {
            if (dashboard.tasks() == null) {
                return new DailyBriefSnapshotDto.TasksModule("UNAVAILABLE", Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
            }

            List<DailyBriefSnapshotDto.TaskBriefItem> overdue = new ArrayList<>();
            if (dashboard.tasks().overdueTasks() != null) {
                for (TaskSummaryDto t : dashboard.tasks().overdueTasks()) {
                    LocalDate dueDate = t.dueAt() != null ? t.dueAt().atZone(zoneId).toLocalDate() : null;
                    long daysOverdue = dueDate != null ? Math.max(1, ChronoUnit.DAYS.between(dueDate, today)) : 1;
                    overdue.add(new DailyBriefSnapshotDto.TaskBriefItem(
                            t.id() != null ? t.id().toString() : UUID.randomUUID().toString(),
                            truncateText(t.title(), 100),
                            truncateText(t.projectName(), 80),
                            dueDate,
                            t.priority(),
                            t.estimatedMinutes(),
                            daysOverdue,
                            t.status()
                    ));
                    if (overdue.size() >= 8) break;
                }
            }

            List<DailyBriefSnapshotDto.TaskBriefItem> dueToday = new ArrayList<>();
            if (dashboard.tasks().dueTodayTasks() != null) {
                for (TaskSummaryDto t : dashboard.tasks().dueTodayTasks()) {
                    LocalDate dueDate = t.dueAt() != null ? t.dueAt().atZone(zoneId).toLocalDate() : today;
                    dueToday.add(new DailyBriefSnapshotDto.TaskBriefItem(
                            t.id() != null ? t.id().toString() : UUID.randomUUID().toString(),
                            truncateText(t.title(), 100),
                            truncateText(t.projectName(), 80),
                            dueDate,
                            t.priority(),
                            t.estimatedMinutes(),
                            0L,
                            t.status()
                    ));
                    if (dueToday.size() >= 8) break;
                }
            }

            List<DailyBriefSnapshotDto.TaskBriefItem> upcoming = new ArrayList<>();
            if (dashboard.tasks().highPriorityTasks() != null) {
                for (TaskSummaryDto t : dashboard.tasks().highPriorityTasks()) {
                    boolean alreadyIncluded = overdue.stream().anyMatch(o -> o.id().equals(t.id() != null ? t.id().toString() : ""))
                            || dueToday.stream().anyMatch(d -> d.id().equals(t.id() != null ? t.id().toString() : ""));
                    if (!alreadyIncluded) {
                        LocalDate dueDate = t.dueAt() != null ? t.dueAt().atZone(zoneId).toLocalDate() : null;
                        upcoming.add(new DailyBriefSnapshotDto.TaskBriefItem(
                                t.id() != null ? t.id().toString() : UUID.randomUUID().toString(),
                                truncateText(t.title(), 100),
                                truncateText(t.projectName(), 80),
                                dueDate,
                                t.priority(),
                                t.estimatedMinutes(),
                                0L,
                                t.status()
                        ));
                    }
                    if (upcoming.size() >= 5) break;
                }
            }

            return new DailyBriefSnapshotDto.TasksModule("AVAILABLE", overdue, dueToday, upcoming);
        } catch (Exception e) {
            log.warn("Failed to assemble Tasks module for brief snapshot: {}", e.getMessage());
            return new DailyBriefSnapshotDto.TasksModule("UNAVAILABLE", Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }
    }

    private DailyBriefSnapshotDto.FinancesModule buildFinancesModule(HomeDashboardResponse dashboard) {
        try {
            if (dashboard.finances() == null && dashboard.budget() == null) {
                return new DailyBriefSnapshotDto.FinancesModule("UNAVAILABLE", Collections.emptyMap(), Collections.emptyList(), Collections.emptyList());
            }

            Map<String, Object> metrics = new LinkedHashMap<>();
            if (dashboard.finances() != null) {
                metrics.put("totalDueSoonAmount", dashboard.finances().totalDueSoonAmount() != null ? dashboard.finances().totalDueSoonAmount() : BigDecimal.ZERO);
                if (dashboard.finances().overallCreditUtilization() != null) {
                    metrics.put("creditUtilizationPercent", dashboard.finances().overallCreditUtilization().overallUtilizationPercent());
                }
                metrics.put("activeCreditCardCount", dashboard.finances().creditCards() != null ? dashboard.finances().creditCards().size() : 0);
            }
            if (dashboard.budget() != null) {
                metrics.put("totalBudgeted", dashboard.budget().totalBudgeted() != null ? dashboard.budget().totalBudgeted() : BigDecimal.ZERO);
                metrics.put("totalSpent", dashboard.budget().totalSpent() != null ? dashboard.budget().totalSpent() : BigDecimal.ZERO);
                metrics.put("remainingBudget", dashboard.budget().remainingBudget() != null ? dashboard.budget().remainingBudget() : BigDecimal.ZERO);
                metrics.put("consumptionPercent", dashboard.budget().consumptionPercent() != null ? dashboard.budget().consumptionPercent() : 0.0);
            }

            List<DailyBriefSnapshotDto.FinancialObligationBriefItem> obligations = new ArrayList<>();
            if (dashboard.finances() != null) {
                if (dashboard.finances().dueToday() != null) {
                    for (CommitmentSummaryDto c : dashboard.finances().dueToday()) {
                        obligations.add(new DailyBriefSnapshotDto.FinancialObligationBriefItem(
                                c.id() != null ? c.id().toString() : UUID.randomUUID().toString(),
                                truncateText(c.name(), 100),
                                c.amount() != null ? c.amount().toPlainString() : "0.00",
                                c.dueDate(),
                                c.isUrgent(),
                                c.accountName() != null ? c.accountName() : "Commitment",
                                "URGENT"
                        ));
                    }
                }
                if (dashboard.finances().dueSoon() != null) {
                    for (CommitmentSummaryDto c : dashboard.finances().dueSoon()) {
                        obligations.add(new DailyBriefSnapshotDto.FinancialObligationBriefItem(
                                c.id() != null ? c.id().toString() : UUID.randomUUID().toString(),
                                truncateText(c.name(), 100),
                                c.amount() != null ? c.amount().toPlainString() : "0.00",
                                c.dueDate(),
                                c.isUrgent(),
                                c.accountName() != null ? c.accountName() : "Commitment",
                                c.isUrgent() ? "URGENT" : "HIGH"
                        ));
                        if (obligations.size() >= 8) break;
                    }
                }
            }

            List<DailyBriefSnapshotDto.BudgetAlertBriefItem> budgetAlerts = new ArrayList<>();
            if (dashboard.budget() != null && dashboard.budget().categories() != null) {
                for (CategorySpendingSummaryDto cs : dashboard.budget().categories()) {
                    if (cs.isOverLimit() || cs.isApproachingLimit() || (cs.consumptionPercent() != null && cs.consumptionPercent() >= 80.0)) {
                        budgetAlerts.add(new DailyBriefSnapshotDto.BudgetAlertBriefItem(
                                cs.categoryName(),
                                cs.spentAmount() != null ? cs.spentAmount().toPlainString() : "0.00",
                                cs.budgetedAmount() != null ? cs.budgetedAmount().toPlainString() : "0.00",
                                cs.consumptionPercent() != null ? cs.consumptionPercent() : 0.0,
                                String.format("%s budget at %.1f%% utilization", cs.categoryName(), cs.consumptionPercent() != null ? cs.consumptionPercent() : 0.0)
                        ));
                    }
                }
            }

            return new DailyBriefSnapshotDto.FinancesModule("AVAILABLE", metrics, obligations, budgetAlerts);
        } catch (Exception e) {
            log.warn("Failed to assemble Finances module for brief snapshot: {}", e.getMessage());
            return new DailyBriefSnapshotDto.FinancesModule("UNAVAILABLE", Collections.emptyMap(), Collections.emptyList(), Collections.emptyList());
        }
    }

    private DailyBriefSnapshotDto.VehiclesModule buildVehiclesModule(HomeDashboardResponse dashboard) {
        try {
            if (dashboard.vehicles() == null) {
                return new DailyBriefSnapshotDto.VehiclesModule("UNAVAILABLE", Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
            }

            VehicleAgendaSummaryDto veh = dashboard.vehicles();
            List<DailyBriefSnapshotDto.VehicleMaintenanceBriefItem> maintenance = new ArrayList<>();
            if (veh.upcomingMaintenance() != null) {
                for (VehicleAgendaSummaryDto.VehicleMaintenanceItemDto m : veh.upcomingMaintenance()) {
                    maintenance.add(new DailyBriefSnapshotDto.VehicleMaintenanceBriefItem(
                            m.id() != null ? m.id().toString() : UUID.randomUUID().toString(),
                            truncateText(m.vehicleName(), 60),
                            truncateText(m.itemName(), 80),
                            m.nextDueAt(),
                            m.nextDueKm(),
                            m.isOverdue()
                    ));
                    if (maintenance.size() >= 5) break;
                }
            }

            List<DailyBriefSnapshotDto.VehicleDocExpiryBriefItem> docs = new ArrayList<>();
            if (veh.expiringDocuments() != null) {
                for (VehicleAgendaSummaryDto.VehicleDocExpiryDto d : veh.expiringDocuments()) {
                    docs.add(new DailyBriefSnapshotDto.VehicleDocExpiryBriefItem(
                            d.id() != null ? d.id().toString() : UUID.randomUUID().toString(),
                            truncateText(d.vehicleName(), 60),
                            d.docType(),
                            truncateText(d.title(), 80),
                            d.expiryDate(),
                            d.daysRemaining(),
                            d.isExpired()
                    ));
                    if (docs.size() >= 5) break;
                }
            }

            List<DailyBriefSnapshotDto.VehicleObligationBriefItem> obligations = new ArrayList<>();
            if (veh.upcomingObligations() != null) {
                for (VehicleAgendaSummaryDto.VehicleObligationSummaryDto o : veh.upcomingObligations()) {
                    obligations.add(new DailyBriefSnapshotDto.VehicleObligationBriefItem(
                            o.id() != null ? o.id().toString() : UUID.randomUUID().toString(),
                            truncateText(o.vehicleName(), 60),
                            truncateText(o.obligationType(), 80),
                            o.amount() != null ? o.amount().toPlainString() : "0.00",
                            o.dueDate()
                    ));
                    if (obligations.size() >= 5) break;
                }
            }

            return new DailyBriefSnapshotDto.VehiclesModule("AVAILABLE", maintenance, docs, obligations);
        } catch (Exception e) {
            log.warn("Failed to assemble Vehicles module for brief snapshot: {}", e.getMessage());
            return new DailyBriefSnapshotDto.VehiclesModule("UNAVAILABLE", Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }
    }

    private DailyBriefSnapshotDto.IntelligenceModule buildIntelligenceModule(HomeDashboardResponse dashboard) {
        try {
            if (dashboard.intelligence() == null) {
                return new DailyBriefSnapshotDto.IntelligenceModule("UNAVAILABLE", Collections.emptyList());
            }

            List<DailyBriefSnapshotDto.IntelligenceArticleBriefItem> articles = new ArrayList<>();
            if (dashboard.intelligence().topDailyArticles() != null) {
                for (IntelligenceAgendaSummaryDto.IntelligenceArticleSummaryDto a : dashboard.intelligence().topDailyArticles()) {
                    String sourceUrl = "https://monday.app/intelligence";
                    if (articleRepository != null && a.id() != null) {
                        try {
                            Optional<Article> artOpt = articleRepository.findById(a.id());
                            if (artOpt.isPresent()) {
                                Article art = artOpt.get();
                                if (art.getCanonicalUrl() != null && !art.getCanonicalUrl().isBlank()) {
                                    sourceUrl = art.getCanonicalUrl();
                                } else if (art.getOriginalUrl() != null && !art.getOriginalUrl().isBlank()) {
                                    sourceUrl = art.getOriginalUrl();
                                }
                            }
                        } catch (Exception ex) {
                            log.debug("Could not resolve article URL: {}", ex.getMessage());
                        }
                    }

                    articles.add(new DailyBriefSnapshotDto.IntelligenceArticleBriefItem(
                            a.id() != null ? a.id().toString() : UUID.randomUUID().toString(),
                            truncateText(a.title(), 120),
                            truncateText(a.sourceName(), 60),
                            sourceUrl,
                            a.areaOfInterest(),
                            a.publishedAt() != null ? a.publishedAt().toInstant() : Instant.now(),
                            truncateText(a.summarySnippet(), 280)
                    ));
                    if (articles.size() >= 4) break;
                }
            }

            return new DailyBriefSnapshotDto.IntelligenceModule("AVAILABLE", articles);
        } catch (Exception e) {
            log.warn("Failed to assemble Intelligence module for brief snapshot: {}", e.getMessage());
            return new DailyBriefSnapshotDto.IntelligenceModule("UNAVAILABLE", Collections.emptyList());
        }
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        String trimmed = text.trim();
        if (trimmed.length() <= maxLength) return trimmed;
        return trimmed.substring(0, maxLength - 3) + "...";
    }
}
