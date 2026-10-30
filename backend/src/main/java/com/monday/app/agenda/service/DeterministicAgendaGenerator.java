package com.monday.app.agenda.service;

import com.monday.app.agenda.dto.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

/**
 * Deterministic generator that produces high-quality, fully grounded daily agendas.
 * Used when the external AI Gateway is unavailable, unconfigured, or fails validation.
 * Ensures the system never halts or produces fictional/empty summaries.
 */
@Component
public class DeterministicAgendaGenerator {

    public TodayAgendaResponse generate(AgendaContextPayload context) {
        LocalDate date = context.agendaDate();
        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(context.timezone());
        } catch (Exception e) {
            zoneId = ZoneId.of("UTC");
        }

        Instant startOfDay = date.atStartOfDay(zoneId).toInstant();

        // 1. Determine top 3 grounded recommendations
        List<AgendaRecommendationDto> recommendations = buildRecommendations(context, startOfDay);

        // 2. Determine non-conflicting suggested focus blocks
        List<SuggestedTimeBlockDto> focusBlocks = buildFocusBlocks(context, recommendations, zoneId);

        // 3. Build important alerts
        List<AgendaAlertDto> alerts = buildAlerts(context);

        // 4. Build financial heads-up
        FinancialHeadsUpDto financialHeadsUp = buildFinancialHeadsUp(context);

        // 5. Build execution insight
        ExecutionInsightDto executionInsight = buildExecutionInsight(context);

        // 6. Build narrative summary markdown
        String summaryMarkdown = buildSummaryMarkdown(context, recommendations, alerts);

        return new TodayAgendaResponse(
                date,
                "GENERATED",
                Instant.now(),
                summaryMarkdown,
                recommendations,
                focusBlocks,
                alerts,
                financialHeadsUp,
                executionInsight,
                false,
                null
        );
    }

    private List<AgendaRecommendationDto> buildRecommendations(AgendaContextPayload ctx, Instant startOfDay) {
        List<AgendaRecommendationDto> list = new ArrayList<>();

        List<TaskSummaryDto> overdue = ctx.execution().overdueTasks();
        List<TaskSummaryDto> dueToday = ctx.execution().tasksDueToday();
        List<TaskSummaryDto> highPriority = ctx.execution().highPriorityTasks();

        // Priority 1: Top overdue or critical task OR overdue vehicle maintenance OR overdue admin obligation
        if (!overdue.isEmpty()) {
            TaskSummaryDto topOverdue = overdue.get(0);
            List<SourceReferenceDto> refs = new ArrayList<>();
            refs.add(new SourceReferenceDto("TASK", topOverdue.id(), topOverdue.title()));
            if (topOverdue.projectId() != null) {
                refs.add(new SourceReferenceDto("PROJECT", topOverdue.projectId(), topOverdue.projectName()));
            }
            if (topOverdue.milestoneId() != null) {
                refs.add(new SourceReferenceDto("MILESTONE", topOverdue.milestoneId(), topOverdue.milestoneName()));
            }

            list.add(new AgendaRecommendationDto(
                    "rec-1",
                    "Resolve Overdue: " + topOverdue.title(),
                    topOverdue.description() != null ? topOverdue.description() : "Clear this pending task to unblock downstream project momentum.",
                    "HIGH",
                    "This commitment passed its scheduled deadline and impacts project velocity.",
                    "Open the task and complete the immediate action item or advance state.",
                    refs,
                    topOverdue.estimatedMinutes() != null ? topOverdue.estimatedMinutes() : 60,
                    null,
                    null,
                    0.95,
                    null
            ));
        } else if (ctx.vehicles() != null && !ctx.vehicles().upcomingMaintenance().isEmpty() && ctx.vehicles().upcomingMaintenance().get(0).isOverdue()) {
            VehicleAgendaSummaryDto.VehicleMaintenanceItemDto vm = ctx.vehicles().upcomingMaintenance().get(0);
            list.add(new AgendaRecommendationDto(
                    "rec-1",
                    "Vehicle Overdue Service: " + vm.itemName(),
                    "Service cycle overdue for " + vm.vehicleName() + " (" + (vm.nextDueAt() != null ? "due " + vm.nextDueAt() : "") + ").",
                    "HIGH",
                    "Safety and fleet readiness requirement requiring prompt inspection.",
                    "Book service appointment with certified technician.",
                    List.of(new SourceReferenceDto("VEHICLE", vm.vehicleId(), vm.vehicleName())),
                    45,
                    null,
                    null,
                    0.94,
                    null
            ));
        } else if (ctx.administration() != null && !ctx.administration().upcomingObligations().isEmpty() && ctx.administration().upcomingObligations().get(0).isOverdue()) {
            AdministrationAgendaSummaryDto.AdminObligationSummaryDto ao = ctx.administration().upcomingObligations().get(0);
            list.add(new AgendaRecommendationDto(
                    "rec-1",
                    "Administrative Clearance: " + ao.name(),
                    ao.category() + " obligation of " + ao.currency() + " " + (ao.expectedAmount() != null ? ao.expectedAmount().toPlainString() : "0") + " is overdue.",
                    "HIGH",
                    "Overdue administrative liability affecting operational compliance.",
                    "Execute payment settlement and record confirmation.",
                    List.of(new SourceReferenceDto("ADMIN_OBLIGATION", ao.id(), ao.name())),
                    15,
                    null,
                    null,
                    0.93,
                    null
            ));
        } else if (!dueToday.isEmpty()) {
            TaskSummaryDto topDue = dueToday.get(0);
            List<SourceReferenceDto> refs = new ArrayList<>();
            refs.add(new SourceReferenceDto("TASK", topDue.id(), topDue.title()));
            if (topDue.projectId() != null) {
                refs.add(new SourceReferenceDto("PROJECT", topDue.projectId(), topDue.projectName()));
            }

            list.add(new AgendaRecommendationDto(
                    "rec-1",
                    "Complete Today's Focus: " + topDue.title(),
                    topDue.description() != null ? topDue.description() : "Scheduled due today.",
                    "HIGH",
                    "Due today on your execution ledger.",
                    "Review requirements and allocate initial focus block.",
                    refs,
                    topDue.estimatedMinutes() != null ? topDue.estimatedMinutes() : 45,
                    null,
                    null,
                    0.92,
                    null
            ));
        }

        // Priority 2: Upcoming milestone OR Career focus task / milestone OR second overdue / strategic task
        List<MilestoneSummaryDto> milestones = ctx.execution().milestones();
        if (ctx.career() != null && ctx.career().hasActivePlan() && !ctx.career().focusTasks().isEmpty()) {
            CareerAgendaSummaryDto.CareerTaskSummaryDto ct = ctx.career().focusTasks().get(0);
            list.add(new AgendaRecommendationDto(
                    "rec-2",
                    "Career Milestone Sprint: " + ct.title(),
                    "Target milestone: " + ct.milestoneName() + " (" + ctx.career().planName() + ").",
                    "HIGH",
                    "Key strategic step in your 12-month engineering transition trajectory.",
                    "Reserve a focused 60-minute technical block to build or solve deliverables.",
                    List.of(new SourceReferenceDto("CAREER_TASK", UUID.nameUUIDFromBytes(("career:task:" + ct.id()).getBytes(java.nio.charset.StandardCharsets.UTF_8)), ct.title())),
                    60,
                    null,
                    null,
                    0.91,
                    null
            ));
        } else if (!milestones.isEmpty()) {
            MilestoneSummaryDto topMilestone = milestones.get(0);
            List<SourceReferenceDto> refs = new ArrayList<>();
            refs.add(new SourceReferenceDto("MILESTONE", topMilestone.id(), topMilestone.name()));
            if (topMilestone.projectId() != null) {
                refs.add(new SourceReferenceDto("PROJECT", topMilestone.projectId(), topMilestone.projectName()));
            }

            list.add(new AgendaRecommendationDto(
                    "rec-2",
                    "Advance Milestone: " + topMilestone.name(),
                    topMilestone.description() != null ? topMilestone.description() : "Active milestone target approaching due date.",
                    "MEDIUM",
                    "Scheduled due date is approaching on project roadmap.",
                    "Verify subtasks in this milestone and complete pending items.",
                    refs,
                    90,
                    null,
                    null,
                    0.88,
                    null
            ));
        } else if (overdue.size() > 1) {
            TaskSummaryDto secondOverdue = overdue.get(1);
            list.add(new AgendaRecommendationDto(
                    "rec-2",
                    "Clear Backlog: " + secondOverdue.title(),
                    secondOverdue.description() != null ? secondOverdue.description() : "Outstanding task awaiting completion.",
                    "MEDIUM",
                    "Clearing backlogged items restores operational velocity.",
                    "Schedule a 45-minute focus session.",
                    List.of(new SourceReferenceDto("TASK", secondOverdue.id(), secondOverdue.title())),
                    secondOverdue.estimatedMinutes() != null ? secondOverdue.estimatedMinutes() : 45,
                    null,
                    null,
                    0.85,
                    null
            ));
        } else if (!highPriority.isEmpty()) {
            TaskSummaryDto hp = highPriority.get(0);
            list.add(new AgendaRecommendationDto(
                    "rec-2",
                    "Strategic Priority: " + hp.title(),
                    hp.description() != null ? hp.description() : "High priority execution item.",
                    "HIGH",
                    "Flagged as high priority across active projects.",
                    "Engage and execute key deliverables.",
                    List.of(new SourceReferenceDto("TASK", hp.id(), hp.title())),
                    hp.estimatedMinutes() != null ? hp.estimatedMinutes() : 60,
                    null,
                    null,
                    0.90,
                    null
            ));
        }

        // Priority 3: Financial commitment / Event Prep / Admin Date / Intelligence pulse / Vehicle task
        List<CommitmentSummaryDto> commitments = ctx.finances().upcomingCommitments();
        List<EventPrepItemDto> prepItems = ctx.schedule().preparationItems();

        if (!commitments.isEmpty()) {
            CommitmentSummaryDto topCommitment = commitments.get(0);
            list.add(new AgendaRecommendationDto(
                    "rec-3",
                    "Financial Settlement: " + topCommitment.name(),
                    "Amount: ₹" + (topCommitment.amount() != null ? topCommitment.amount().toPlainString() : "0") + " due on " + topCommitment.dueDate(),
                    topCommitment.isUrgent() ? "HIGH" : "MEDIUM",
                    "Imminent cash outflow obligation requiring account balance readiness.",
                    "Reconcile banking account ledger and confirm settlement.",
                    List.of(new SourceReferenceDto("COMMITMENT", topCommitment.id(), topCommitment.name())),
                    15,
                    null,
                    null,
                    0.94,
                    null
            ));
        } else if (ctx.administration() != null && !ctx.administration().upcomingDates().isEmpty() && ctx.administration().upcomingDates().get(0).daysRemaining() <= 7) {
            AdministrationAgendaSummaryDto.AdminImportantDateSummaryDto d = ctx.administration().upcomingDates().get(0);
            list.add(new AgendaRecommendationDto(
                    "rec-3",
                    "Occasion Readiness: " + d.title(),
                    d.title() + (d.personName() != null ? " (" + d.personName() + ")" : "") + " is approaching in " + d.daysRemaining() + " days.",
                    "MEDIUM",
                    "Personal relationship milestone requiring timely preparation.",
                    "Confirm gift, reservation, or necessary greetings.",
                    List.of(new SourceReferenceDto("ADMIN_DATE", d.id(), d.title())),
                    20,
                    null,
                    null,
                    0.89,
                    null
            ));
        } else if (!prepItems.isEmpty()) {
            EventPrepItemDto prep = prepItems.get(0);
            list.add(new AgendaRecommendationDto(
                    "rec-3",
                    "Event Readiness: " + prep.eventTitle(),
                    "Checklist item: " + prep.text(),
                    "MEDIUM",
                    "Required preparation before event session begins.",
                    "Complete preparation checklist item.",
                    List.of(new SourceReferenceDto("EVENT", prep.eventId(), prep.eventTitle())),
                    30,
                    null,
                    null,
                    0.89,
                    null
            ));
        } else if (ctx.intelligence() != null && !ctx.intelligence().curatedArticles().isEmpty()) {
            IntelligenceAgendaSummaryDto.IntelligenceArticleSummaryDto art = ctx.intelligence().curatedArticles().get(0);
            list.add(new AgendaRecommendationDto(
                    "rec-3",
                    "Intelligence Briefing: " + art.title(),
                    "Area: " + (art.areaOfInterest() != null ? art.areaOfInterest() : "Tech") + " • Source: " + art.sourceName(),
                    "MEDIUM",
                    "High-signal industry development to keep your engineering perspective razor-sharp.",
                    "Review the synthesized executive brief (10 mins).",
                    List.of(new SourceReferenceDto("ARTICLE", art.id(), art.title())),
                    15,
                    null,
                    null,
                    0.86,
                    null
            ));
        } else if (ctx.vehicles() != null && !ctx.vehicles().pendingTasks().isEmpty()) {
            VehicleAgendaSummaryDto.VehicleTaskSummaryDto vt = ctx.vehicles().pendingTasks().get(0);
            list.add(new AgendaRecommendationDto(
                    "rec-3",
                    "Vehicle Logistics: " + vt.title(),
                    "Assigned to " + vt.vehicleName() + " (" + vt.priority() + " priority).",
                    "MEDIUM",
                    "Maintenance and readiness task for your vehicle.",
                    "Complete item or mark status in vehicle dashboard.",
                    List.of(new SourceReferenceDto("VEHICLE", vt.vehicleId(), vt.vehicleName())),
                    20,
                    null,
                    null,
                    0.84,
                    null
            ));
        } else if (dueToday.size() > 1) {
            TaskSummaryDto t2 = dueToday.get(1);
            list.add(new AgendaRecommendationDto(
                    "rec-3",
                    "Execution Target: " + t2.title(),
                    t2.description() != null ? t2.description() : "Scheduled due today.",
                    "MEDIUM",
                    "Due today on your schedule.",
                    "Dedicate time to finish this item.",
                    List.of(new SourceReferenceDto("TASK", t2.id(), t2.title())),
                    t2.estimatedMinutes() != null ? t2.estimatedMinutes() : 30,
                    null,
                    null,
                    0.82,
                    null
            ));
        }

        return list;
    }

    private List<SuggestedTimeBlockDto> buildFocusBlocks(AgendaContextPayload ctx,
                                                         List<AgendaRecommendationDto> recs,
                                                         ZoneId zoneId) {
        List<SuggestedTimeBlockDto> blocks = new ArrayList<>();
        LocalDate date = ctx.agendaDate();

        // Standard work hours: 09:00 to 18:00
        Instant windowStart = date.atTime(9, 0).atZone(zoneId).toInstant();
        Instant windowEnd = date.atTime(18, 0).atZone(zoneId).toInstant();

        // Get sorted timed events
        List<EventSummaryDto> events = ctx.schedule().events().stream()
                .filter(e -> !e.allDay() && e.startAt() != null && e.endAt() != null)
                .sorted(Comparator.comparing(EventSummaryDto::startAt))
                .toList();

        // Find candidate free gaps
        Instant cursor = windowStart;
        for (AgendaRecommendationDto rec : recs) {
            int durationMin = rec.suggestedDurationMinutes() != null ? rec.suggestedDurationMinutes() : 60;
            Instant candidateStart = cursor;
            Instant candidateEnd = candidateStart.plus(Duration.ofMinutes(durationMin));

            // Check if candidateEnd exceeds windowEnd
            if (candidateEnd.isAfter(windowEnd)) {
                break;
            }

            // Check against events
            boolean conflicts = false;
            for (EventSummaryDto evt : events) {
                if (candidateStart.isBefore(evt.endAt()) && evt.startAt().isBefore(candidateEnd)) {
                    // Conflict found, advance cursor to end of event
                    cursor = evt.endAt();
                    conflicts = true;
                    break;
                }
            }

            if (!conflicts) {
                UUID taskId = rec.sourceReferences().stream()
                        .filter(r -> "TASK".equalsIgnoreCase(r.type()))
                        .map(SourceReferenceDto::id)
                        .findFirst().orElse(null);

                blocks.add(new SuggestedTimeBlockDto(
                        candidateStart,
                        candidateEnd,
                        rec.title(),
                        "Focused execution window for: " + rec.title(),
                        taskId
                ));
                cursor = candidateEnd.plus(Duration.ofMinutes(15)); // 15 min buffer
            }
        }

        return blocks;
    }

    private List<AgendaAlertDto> buildAlerts(AgendaContextPayload ctx) {
        List<AgendaAlertDto> alerts = new ArrayList<>();

        // Overdue tasks alert
        int overdueCount = ctx.execution().overdueTasks().size();
        if (overdueCount > 0) {
            alerts.add(new AgendaAlertDto(
                    "DEADLINE",
                    overdueCount + " Overdue Task" + (overdueCount == 1 ? "" : "s") + " Detected",
                    "You have " + overdueCount + " incomplete task" + (overdueCount == 1 ? "" : "s") + " past scheduled deadlines. Prioritize clearing the oldest blockers today.",
                    "DANGER"
            ));
        }

        // Capacity overload alert
        WorkCapacityDto cap = ctx.capacity();
        if (cap != null && cap.isOverloaded()) {
            alerts.add(new AgendaAlertDto(
                    "OVERLOAD",
                    "Schedule Density Alert",
                    "Estimated task workload (" + cap.estimatedTaskWorkloadMinutes() + " mins) exceeds your available focus capacity (" + cap.availableWorkMinutes() + " mins). Consider deferring secondary items.",
                    "WARNING"
            ));
        }

        // Schedule conflicts alert
        if (!ctx.schedule().conflicts().isEmpty()) {
            ScheduleConflictDto c = ctx.schedule().conflicts().get(0);
            alerts.add(new AgendaAlertDto(
                    "CONFLICT",
                    "Calendar Overlap Detected",
                    "'" + c.title1() + "' overlaps with '" + c.title2() + "' for " + c.overlapMinutes() + " minutes. Adjust calendar times to avoid double-booking.",
                    "DANGER"
            ));
        }

        // Credit utilization alert
        CreditUtilizationSummaryDto util = ctx.finances().creditUtilization();
        if (util != null && util.isAvailable() && util.overallUtilizationPercent() != null && util.overallUtilizationPercent() > 50.0) {
            alerts.add(new AgendaAlertDto(
                    "FINANCIAL",
                    "Credit Line Utilization High (" + Math.round(util.overallUtilizationPercent()) + "%)",
                    "Aggregate revolving balances exceed 50% of total credit limits. Review repayment timeline to preserve optimal credit standing.",
                    "WARNING"
            ));
        }

        // Vehicle maintenance & document alerts
        if (ctx.vehicles() != null) {
            if (!ctx.vehicles().upcomingMaintenance().isEmpty() && ctx.vehicles().upcomingMaintenance().stream().anyMatch(VehicleAgendaSummaryDto.VehicleMaintenanceItemDto::isOverdue)) {
                alerts.add(new AgendaAlertDto(
                        "VEHICLE",
                        "Vehicle Service Overdue",
                        "One or more scheduled maintenance cycles on your vehicle fleet are overdue. Schedule inspections to maintain reliability.",
                        "DANGER"
                ));
            } else if (!ctx.vehicles().expiringDocuments().isEmpty() && ctx.vehicles().expiringDocuments().stream().anyMatch(VehicleAgendaSummaryDto.VehicleDocExpiryDto::isExpired)) {
                alerts.add(new AgendaAlertDto(
                        "VEHICLE",
                        "Vehicle Document Expired",
                        "Critical vehicle documents (insurance, PUC, or registration) have expired. Renew immediately to ensure road compliance.",
                        "DANGER"
                ));
            }
        }

        // Administration alerts
        if (ctx.administration() != null) {
            if (ctx.administration().obligationsOverdueCount() > 0) {
                alerts.add(new AgendaAlertDto(
                        "ADMINISTRATIVE",
                        ctx.administration().obligationsOverdueCount() + " Overdue Administrative Obligation" + (ctx.administration().obligationsOverdueCount() == 1 ? "" : "s"),
                        "Utility bills, subscriptions, or administrative renewals have passed their due date.",
                        "WARNING"
                ));
            } else if (!ctx.administration().upcomingDates().isEmpty() && ctx.administration().upcomingDates().get(0).daysRemaining() <= 3) {
                AdministrationAgendaSummaryDto.AdminImportantDateSummaryDto id = ctx.administration().upcomingDates().get(0);
                alerts.add(new AgendaAlertDto(
                        "ADMINISTRATIVE",
                        "Impending Date: " + id.title(),
                        id.title() + (id.personName() != null ? " (" + id.personName() + ")" : "") + " is in " + id.daysRemaining() + " days. Verify all arrangements.",
                        "INFO"
                ));
            }
        }

        // Career milestone alert
        if (ctx.career() != null && ctx.career().hasActivePlan() && !ctx.career().activeMilestones().isEmpty()) {
            CareerAgendaSummaryDto.CareerMilestoneSummaryDto cm = ctx.career().activeMilestones().get(0);
            if (cm.dueDate() != null && cm.dueDate().isBefore(ctx.agendaDate().plusDays(7))) {
                alerts.add(new AgendaAlertDto(
                        "CAREER",
                        "Career Milestone Approaching: " + cm.name(),
                        "Target milestone is due on " + cm.dueDate() + " (" + cm.progressPercent() + "% completed). Dedicate deep work sessions to maintain target velocity.",
                        "INFO"
                ));
            }
        }

        return alerts;
    }

    private FinancialHeadsUpDto buildFinancialHeadsUp(AgendaContextPayload ctx) {
        List<CommitmentSummaryDto> commitments = ctx.finances().upcomingCommitments();
        if (!commitments.isEmpty()) {
            BigDecimal totalSoon = commitments.stream()
                    .map(CommitmentSummaryDto::amount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return new FinancialHeadsUpDto(
                    "Impending Obligations: ₹" + totalSoon.toPlainString(),
                    commitments.size() + " scheduled payment" + (commitments.size() == 1 ? "" : "s") + " due within the planning horizon. Verify account liquidity.",
                    totalSoon,
                    totalSoon.compareTo(BigDecimal.valueOf(50000)) > 0 ? "HIGH" : "MEDIUM"
            );
        }

        CreditUtilizationSummaryDto util = ctx.finances().creditUtilization();
        if (util != null && util.isAvailable()) {
            return new FinancialHeadsUpDto(
                    "Revolving Balance: ₹" + util.totalBalance().toPlainString(),
                    "Credit line utilization stands at " + (util.overallUtilizationPercent() != null ? Math.round(util.overallUtilizationPercent()) : 0) + "%.",
                    util.totalBalance(),
                    "LOW"
            );
        }

        return new FinancialHeadsUpDto(
                "Financial Ledger Balanced",
                "No urgent obligations or overdue commitments recorded for this cycle.",
                BigDecimal.ZERO,
                "LOW"
        );
    }

    private ExecutionInsightDto buildExecutionInsight(AgendaContextPayload ctx) {
        if (ctx.career() != null && ctx.career().hasActivePlan()) {
            String daysText = ctx.career().daysToTarget() != null ? " (" + ctx.career().daysToTarget() + " days remaining)" : "";
            return new ExecutionInsightDto(
                    "Career Transition Velocity",
                    "Plan '" + ctx.career().planName() + "' is at " + ctx.career().progressPercent() + "% progress" + daysText + ". Maintain consistent execution on active technical milestones.",
                    ctx.career().progressPercent() + "% Complete"
            );
        }

        DeepWorkSummaryDto dw = ctx.execution().recentDeepWork();
        if (dw != null && dw.sessionsCountLast7Days() > 0) {
            return new ExecutionInsightDto(
                    "Deep Work Momentum",
                    "Logged " + dw.totalHoursLast7Days() + "h across " + dw.sessionsCountLast7Days() + " focus sessions in the past 7 days with an average focus score of " + dw.averageFocusScore() + "%.",
                    dw.totalHoursLast7Days() + "h / 5.0h Target"
            );
        }

        return new ExecutionInsightDto(
                "Execution Readiness",
                "Deep work tracking is active. Engage 90-minute structured sprints for maximal cognitive throughput.",
                "Ready for Focus"
        );
    }

    private String buildSummaryMarkdown(AgendaContextPayload ctx,
                                         List<AgendaRecommendationDto> recs,
                                         List<AgendaAlertDto> alerts) {
        StringBuilder sb = new StringBuilder();
        int taskCount = ctx.execution().overdueTasks().size() + ctx.execution().tasksDueToday().size();
        int eventCount = ctx.schedule().events().size();
        int availMin = ctx.capacity() != null ? ctx.capacity().availableWorkMinutes() : 480;
        double availHours = BigDecimal.valueOf(availMin / 60.0).setScale(1, RoundingMode.HALF_UP).doubleValue();

        sb.append("### Sovereign Command Overview\n\n");
        sb.append("Good morning. Today's operational horizon features **").append(eventCount).append(" scheduled event")
                .append(eventCount == 1 ? "" : "s").append("** and **")
                .append(taskCount).append(" actionable task item")
                .append(taskCount == 1 ? "" : "s").append("**. ")
                .append("Your temporal ledger permits approximately **").append(availHours).append(" hours of unencumbered focus capacity**.\n\n");

        if (ctx.career() != null && ctx.career().hasActivePlan()) {
            sb.append("🎯 **Career Trajectory:** '").append(ctx.career().planName()).append("' is at **")
                    .append(ctx.career().progressPercent()).append("% progress**");
            if (ctx.career().daysToTarget() != null) {
                sb.append(" with **").append(ctx.career().daysToTarget()).append(" days** to target completion");
            }
            sb.append(".\n\n");
        }

        if (ctx.vehicles() != null && ctx.vehicles().totalVehicles() > 0) {
            sb.append("🚗 **Mobility Readiness:** Fleet tracking **").append(ctx.vehicles().totalVehicles()).append(" vehicle(s)** (Primary: ")
                    .append(ctx.vehicles().primaryVehicleName() != null ? ctx.vehicles().primaryVehicleName() : "Standard")
                    .append("). ");
            if (ctx.vehicles().upcomingMaintenance().stream().anyMatch(VehicleAgendaSummaryDto.VehicleMaintenanceItemDto::isOverdue)) {
                sb.append("⚠️ **Overdue maintenance recorded**.\n\n");
            } else {
                sb.append("Fleet telemetry and maintenance cycles are within normal bounds.\n\n");
            }
        }

        if (ctx.administration() != null && ctx.administration().activeObligationsCount() > 0) {
            sb.append("📋 **Administration & Household:** ").append(ctx.administration().activeObligationsCount())
                    .append(" active commitments. ");
            if (!ctx.administration().upcomingDates().isEmpty()) {
                AdministrationAgendaSummaryDto.AdminImportantDateSummaryDto nextDate = ctx.administration().upcomingDates().get(0);
                sb.append("Next occasion: **").append(nextDate.title()).append("** in ").append(nextDate.daysRemaining()).append(" days.\n\n");
            } else {
                sb.append("No immediate occasion alerts.\n\n");
            }
        }

        if (ctx.intelligence() != null && !ctx.intelligence().curatedArticles().isEmpty()) {
            sb.append("📡 **Industry Intelligence:** ").append(ctx.intelligence().curatedArticles().size())
                    .append(" curated briefing(s) available across AI and distributed systems.\n\n");
        }

        if (!recs.isEmpty()) {
            sb.append("**Recommended Strategic Thrusts:**\n\n");
            for (int i = 0; i < recs.size(); i++) {
                AgendaRecommendationDto r = recs.get(i);
                sb.append(i + 1).append(". **").append(r.title()).append("**: ").append(r.reason()).append("\n\n");
            }
        }

        if (!alerts.isEmpty()) {
            sb.append("⚠️ **Critical Attention Required:** ").append(alerts.get(0).message()).append("\n\n");
        }

        return sb.toString();
    }
}
