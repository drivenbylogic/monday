package com.monday.app.agenda.service;

import com.monday.app.agenda.dto.BetaDailyBriefDto;
import com.monday.app.agenda.dto.DailyBriefSnapshotDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Deterministic synthesizer for the Beta Daily Briefing.
 * Generates the identical 7-section structured format without requiring LLM inference,
 * providing 100% reliable fallback and high-speed offline operation.
 */
@Service
public class DeterministicBetaBriefGenerator {

    public BetaDailyBriefDto generate(DailyBriefSnapshotDto snapshot) {
        if (snapshot == null || snapshot.modules() == null) {
            return emptyBriefing();
        }

        DailyBriefSnapshotDto.ModulesSnapshot modules = snapshot.modules();

        // 1. TODAY AT A GLANCE
        BetaDailyBriefDto.TodayAtAGlanceSection todayAtAGlance = buildTodayAtAGlance(snapshot);

        // 2. PRIORITIES
        List<BetaDailyBriefDto.PriorityItem> priorities = buildPriorities(modules);

        // 3. SCHEDULE
        List<BetaDailyBriefDto.ScheduleItem> schedule = buildSchedule(modules.calendar());

        // 4. PROJECTS AND TASKS
        List<BetaDailyBriefDto.ProjectTaskItem> projectsAndTasks = buildProjectsAndTasks(modules.projects(), modules.tasks());

        // 5. FINANCES AND VEHICLES
        List<BetaDailyBriefDto.FinanceVehicleItem> financesAndVehicles = buildFinancesAndVehicles(modules.finances(), modules.vehicles());

        // 6. INTELLIGENCE
        List<BetaDailyBriefDto.IntelligenceItem> intelligence = buildIntelligence(modules.intelligence());

        // 7. NEXT ACTIONS
        List<BetaDailyBriefDto.NextActionItem> nextActions = buildNextActions(priorities, schedule, projectsAndTasks);

        return new BetaDailyBriefDto(
                todayAtAGlance,
                priorities,
                schedule,
                projectsAndTasks,
                financesAndVehicles,
                intelligence,
                nextActions
        );
    }

    private BetaDailyBriefDto.TodayAtAGlanceSection buildTodayAtAGlance(DailyBriefSnapshotDto snapshot) {
        DailyBriefSnapshotDto.ModulesSnapshot m = snapshot.modules();

        int overdueCount = m.tasks() != null && m.tasks().overdue() != null ? m.tasks().overdue().size() : 0;
        int dueTodayCount = m.tasks() != null && m.tasks().dueToday() != null ? m.tasks().dueToday().size() : 0;
        int eventCount = m.calendar() != null && m.calendar().eventsToday() != null ? m.calendar().eventsToday().size() : 0;
        int urgentFinances = m.finances() != null && m.finances().upcomingObligations() != null ?
                (int) m.finances().upcomingObligations().stream().filter(DailyBriefSnapshotDto.FinancialObligationBriefItem::isOverdue).count() : 0;
        int maintenanceDue = m.vehicles() != null && m.vehicles().upcomingMaintenance() != null ?
                (int) m.vehicles().upcomingMaintenance().stream().filter(DailyBriefSnapshotDto.VehicleMaintenanceBriefItem::isOverdue).count() : 0;

        String urgencyLevel = "NORMAL";
        if (overdueCount > 0 || urgentFinances > 0 || maintenanceDue > 0) {
            urgencyLevel = "CRITICAL";
        } else if (dueTodayCount > 0 || eventCount > 0) {
            urgencyLevel = "ATTENTION";
        }

        String headline;
        StringBuilder summary = new StringBuilder();

        if (overdueCount > 0) {
            headline = String.format("%d Pending Overdue Items Require Immediate Attention", overdueCount);
            summary.append(String.format("You have %d overdue tasks alongside %d tasks due today. ", overdueCount, dueTodayCount));
        } else if (dueTodayCount > 0) {
            headline = String.format("%d Deliverables Scheduled for Completion Today", dueTodayCount);
            summary.append(String.format("Focused execution day with %d tasks due. ", dueTodayCount));
        } else if (eventCount > 0) {
            headline = String.format("Schedule Aligned: %d Calendar Commitments Today", eventCount);
            summary.append(String.format("Your schedule has %d scheduled events. ", eventCount));
        } else {
            headline = "Operational Posture Clear";
            summary.append("No critical blockers or impending deadlines recorded for today. ");
        }

        if (urgentFinances > 0) {
            summary.append(String.format("Notice: %d financial obligation is overdue. ", urgentFinances));
        }
        if (maintenanceDue > 0) {
            summary.append("Vehicle service requirement flagged for review. ");
        }

        String keyFocus;
        if (overdueCount > 0) {
            keyFocus = "Overdue Remediation & Task Velocity";
        } else if (dueTodayCount > 0) {
            keyFocus = "Deliverable Execution";
        } else if (eventCount > 0) {
            keyFocus = "Calendar Engagements";
        } else if (urgentFinances > 0) {
            keyFocus = "Financial Settlement";
        } else {
            keyFocus = "Strategic Planning & Maintenance";
        }

        return new BetaDailyBriefDto.TodayAtAGlanceSection(
                headline,
                summary.toString().trim(),
                urgencyLevel,
                keyFocus
        );
    }

    private List<BetaDailyBriefDto.PriorityItem> buildPriorities(DailyBriefSnapshotDto.ModulesSnapshot modules) {
        List<BetaDailyBriefDto.PriorityItem> items = new ArrayList<>();
        int count = 1;

        // 1. Check Overdue Tasks
        if (modules.tasks() != null && modules.tasks().overdue() != null) {
            for (var t : modules.tasks().overdue()) {
                items.add(new BetaDailyBriefDto.PriorityItem(
                        "pri-" + (count++),
                        t.title(),
                        String.format("Task is overdue by %d days in project '%s'.", t.daysOverdue() != null ? t.daysOverdue() : 1, t.projectName()),
                        "URGENT",
                        "TASK",
                        t.id(),
                        "Complete Task"
                ));
                if (items.size() >= 2) break;
            }
        }

        // 2. Check Blocked Items
        if (modules.projects() != null && modules.projects().blockedItems() != null) {
            for (var b : modules.projects().blockedItems()) {
                items.add(new BetaDailyBriefDto.PriorityItem(
                        "pri-" + (count++),
                        b.title(),
                        String.format("Blocked in '%s': %s", b.projectName(), b.blockerReason()),
                        "HIGH",
                        "BLOCKER",
                        b.id(),
                        "Resolve Blocker"
                ));
                if (items.size() >= 3) break;
            }
        }

        // 3. Check Financial Obligations due today or overdue
        if (modules.finances() != null && modules.finances().upcomingObligations() != null) {
            for (var f : modules.finances().upcomingObligations()) {
                if (f.isOverdue() || "URGENT".equalsIgnoreCase(f.urgency())) {
                    items.add(new BetaDailyBriefDto.PriorityItem(
                            "pri-" + (count++),
                            f.title(),
                            String.format("Payment obligation of %s is due or overdue.", f.amount()),
                            "URGENT",
                            "FINANCE",
                            f.id(),
                            "Review Payment"
                    ));
                    if (items.size() >= 4) break;
                }
            }
        }

        // 4. Check Vehicle Maintenance overdue
        if (modules.vehicles() != null && modules.vehicles().upcomingMaintenance() != null) {
            for (var v : modules.vehicles().upcomingMaintenance()) {
                if (v.isOverdue()) {
                    items.add(new BetaDailyBriefDto.PriorityItem(
                            "pri-" + (count++),
                            String.format("%s: %s", v.vehicleName(), v.itemName()),
                            "Scheduled vehicle maintenance is overdue.",
                            "HIGH",
                            "VEHICLE",
                            v.id(),
                            "Schedule Service"
                    ));
                    if (items.size() >= 4) break;
                }
            }
        }

        // 5. If still empty, add top due today task
        if (items.isEmpty() && modules.tasks() != null && modules.tasks().dueToday() != null && !modules.tasks().dueToday().isEmpty()) {
            var t = modules.tasks().dueToday().get(0);
            items.add(new BetaDailyBriefDto.PriorityItem(
                    "pri-" + (count++),
                    t.title(),
                    String.format("Deliverable due today in project '%s'.", t.projectName()),
                    "HIGH",
                    "TASK",
                    t.id(),
                    "Start Task"
            ));
        }

        return items;
    }

    private List<BetaDailyBriefDto.ScheduleItem> buildSchedule(DailyBriefSnapshotDto.CalendarModule calendar) {
        if (calendar == null || calendar.eventsToday() == null || calendar.eventsToday().isEmpty()) {
            return Collections.emptyList();
        }

        List<BetaDailyBriefDto.ScheduleItem> items = new ArrayList<>();
        int count = 1;

        for (var e : calendar.eventsToday()) {
            String timeDisplay = e.startTime() != null && !e.startTime().isBlank() ? e.startTime() : (e.isAllDay() ? "All Day" : "TBD");
            String duration = (e.startTime() != null && e.endTime() != null) ? e.startTime() + " - " + e.endTime() : "Scheduled";

            items.add(new BetaDailyBriefDto.ScheduleItem(
                    "sch-" + (count++),
                    timeDisplay,
                    e.title(),
                    duration,
                    e.hasConflict(),
                    e.hasConflict() ? "Schedule overlap detected with adjacent commitment" : null,
                    e.prepNotes() != null && !e.prepNotes().isBlank() ? e.prepNotes() : null
            ));
        }

        return items;
    }

    private List<BetaDailyBriefDto.ProjectTaskItem> buildProjectsAndTasks(
            DailyBriefSnapshotDto.ProjectsModule projects,
            DailyBriefSnapshotDto.TasksModule tasks) {

        List<BetaDailyBriefDto.ProjectTaskItem> items = new ArrayList<>();
        int count = 1;

        // Approaching milestones
        if (projects != null && projects.upcomingMilestones() != null) {
            for (var m : projects.upcomingMilestones()) {
                items.add(new BetaDailyBriefDto.ProjectTaskItem(
                        "pt-" + (count++),
                        m.title(),
                        m.projectName(),
                        "MILESTONE",
                        m.dueDate() != null ? m.dueDate().toString() : "Impending",
                        String.format("Milestone status: %s", m.status())
                ));
                if (items.size() >= 3) break;
            }
        }

        // Blocked items
        if (projects != null && projects.blockedItems() != null) {
            for (var b : projects.blockedItems()) {
                items.add(new BetaDailyBriefDto.ProjectTaskItem(
                        "pt-" + (count++),
                        b.title(),
                        b.projectName(),
                        "BLOCKER",
                        "Blocked",
                        b.blockerReason()
                ));
                if (items.size() >= 5) break;
            }
        }

        // Due today tasks
        if (tasks != null && tasks.dueToday() != null) {
            for (var t : tasks.dueToday()) {
                items.add(new BetaDailyBriefDto.ProjectTaskItem(
                        "pt-" + (count++),
                        t.title(),
                        t.projectName(),
                        "APPROACHING_DEADLINE",
                        "Due Today",
                        String.format("Estimated time: %d mins | Priority: %s", t.estimatedMinutes() != null ? t.estimatedMinutes() : 30, t.priority())
                ));
                if (items.size() >= 6) break;
            }
        }

        return items;
    }

    private List<BetaDailyBriefDto.FinanceVehicleItem> buildFinancesAndVehicles(
            DailyBriefSnapshotDto.FinancesModule finances,
            DailyBriefSnapshotDto.VehiclesModule vehicles) {

        List<BetaDailyBriefDto.FinanceVehicleItem> items = new ArrayList<>();
        int count = 1;

        if (finances != null && finances.upcomingObligations() != null) {
            for (var f : finances.upcomingObligations()) {
                items.add(new BetaDailyBriefDto.FinanceVehicleItem(
                        "fv-" + (count++),
                        f.title(),
                        "FINANCE",
                        f.amount(),
                        f.dueDate() != null ? f.dueDate().toString() : "Due Soon",
                        f.urgency() != null ? f.urgency() : "MEDIUM",
                        f.isOverdue() ? "OVERDUE" : "UPCOMING",
                        String.format("Category: %s", f.category())
                ));
                if (items.size() >= 4) break;
            }
        }

        if (vehicles != null && vehicles.upcomingMaintenance() != null) {
            for (var v : vehicles.upcomingMaintenance()) {
                items.add(new BetaDailyBriefDto.FinanceVehicleItem(
                        "fv-" + (count++),
                        String.format("%s: %s", v.vehicleName(), v.itemName()),
                        "VEHICLE",
                        v.nextDueKm() != null ? v.nextDueKm() + " km" : "Due Date",
                        v.nextDueAt() != null ? v.nextDueAt().toString() : "Scheduled",
                        v.isOverdue() ? "HIGH" : "MEDIUM",
                        v.isOverdue() ? "OVERDUE" : "UPCOMING",
                        "Scheduled periodic vehicle maintenance"
                ));
                if (items.size() >= 7) break;
            }
        }

        if (vehicles != null && vehicles.expiringDocuments() != null) {
            for (var d : vehicles.expiringDocuments()) {
                items.add(new BetaDailyBriefDto.FinanceVehicleItem(
                        "fv-" + (count++),
                        String.format("%s: %s (%s)", d.vehicleName(), d.title(), d.docType()),
                        "VEHICLE",
                        d.daysRemaining() + " days remaining",
                        d.expiryDate() != null ? d.expiryDate().toString() : "Expiring",
                        d.isExpired() ? "HIGH" : "MEDIUM",
                        d.isExpired() ? "EXPIRING" : "UPCOMING",
                        "Vehicle document renewal requirement"
                ));
                if (items.size() >= 8) break;
            }
        }

        return items;
    }

    private List<BetaDailyBriefDto.IntelligenceItem> buildIntelligence(DailyBriefSnapshotDto.IntelligenceModule intel) {
        if (intel == null || intel.articles() == null || intel.articles().isEmpty()) {
            return Collections.emptyList();
        }

        List<BetaDailyBriefDto.IntelligenceItem> items = new ArrayList<>();
        int count = 1;

        for (var a : intel.articles()) {
            items.add(new BetaDailyBriefDto.IntelligenceItem(
                    "int-" + (count++),
                    a.title(),
                    a.summarySnippet() != null && !a.summarySnippet().isBlank() ? a.summarySnippet() : "Relevant curated intelligence briefing.",
                    a.sourceUrl() != null ? a.sourceUrl() : "https://monday.app/intelligence",
                    a.sourceName() != null ? a.sourceName() : "Curated Intel",
                    a.areaOfInterest() != null ? a.areaOfInterest() : "TECHNOLOGY"
            ));
            if (items.size() >= 3) break;
        }

        return items;
    }

    private List<BetaDailyBriefDto.NextActionItem> buildNextActions(
            List<BetaDailyBriefDto.PriorityItem> priorities,
            List<BetaDailyBriefDto.ScheduleItem> schedule,
            List<BetaDailyBriefDto.ProjectTaskItem> projectsAndTasks) {

        List<BetaDailyBriefDto.NextActionItem> actions = new ArrayList<>();

        if (priorities != null && !priorities.isEmpty()) {
            var top = priorities.get(0);
            actions.add(new BetaDailyBriefDto.NextActionItem(
                    1,
                    String.format("%s: %s", top.actionLabel(), top.title()),
                    top.whyItMatters(),
                    top.category()
            ));
        }

        if (schedule != null && !schedule.isEmpty()) {
            var nextEvent = schedule.get(0);
            actions.add(new BetaDailyBriefDto.NextActionItem(
                    2,
                    String.format("Prepare for %s (%s)", nextEvent.title(), nextEvent.time()),
                    nextEvent.prepNotes() != null ? nextEvent.prepNotes() : "Upcoming calendar engagement",
                    "SCHEDULING"
            ));
        }

        if (projectsAndTasks != null && !projectsAndTasks.isEmpty()) {
            for (var pt : projectsAndTasks) {
                if ("APPROACHING_DEADLINE".equalsIgnoreCase(pt.itemType()) || "MILESTONE".equalsIgnoreCase(pt.itemType())) {
                    actions.add(new BetaDailyBriefDto.NextActionItem(
                            actions.size() + 1,
                            String.format("Advance %s in %s", pt.title(), pt.projectName()),
                            pt.details(),
                            "EXECUTION"
                    ));
                    break;
                }
            }
        }

        if (actions.isEmpty()) {
            actions.add(new BetaDailyBriefDto.NextActionItem(
                    1,
                    "Review operational backlog and weekly targets",
                    "No immediate blockers requiring intervention",
                    "EXECUTION"
            ));
        }

        return actions.stream().limit(3).toList();
    }

    private BetaDailyBriefDto emptyBriefing() {
        return new BetaDailyBriefDto(
                new BetaDailyBriefDto.TodayAtAGlanceSection(
                        "Operations Synchronized",
                        "All systems operational. No high-urgency notifications.",
                        "NORMAL",
                        "Strategic Overview"
                ),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(new BetaDailyBriefDto.NextActionItem(
                        1,
                        "Plan key milestones for the week",
                        "Operational slate clear",
                        "EXECUTION"
                ))
        );
    }
}

