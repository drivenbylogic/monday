package com.monday.app.agenda.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.app.administration.dto.AdministrationOverviewDto;
import com.monday.app.administration.service.AdministrationOverviewService;
import com.monday.app.agenda.dto.*;
import com.monday.app.career.entity.CareerPlan;
import com.monday.app.career.service.CareerService;
import com.monday.app.event.entity.Event;
import com.monday.app.event.repository.EventRepository;
import com.monday.app.finance.entity.*;
import com.monday.app.finance.repository.*;
import com.monday.app.intelligence.dto.DashboardStatsDto;
import com.monday.app.intelligence.entity.Article;
import com.monday.app.intelligence.entity.ArticleSource;
import com.monday.app.intelligence.entity.ArticleSummary;
import com.monday.app.intelligence.repository.ArticleRepository;
import com.monday.app.intelligence.repository.ArticleSourceRepository;
import com.monday.app.intelligence.repository.ArticleSummaryRepository;
import com.monday.app.intelligence.service.DashboardStatsService;
import com.monday.app.project.entity.Milestone;
import com.monday.app.project.entity.Project;
import com.monday.app.project.entity.Task;
import com.monday.app.project.repository.MilestoneRepository;
import com.monday.app.project.repository.ProjectRepository;
import com.monday.app.project.repository.TaskRepository;
import com.monday.app.reminder.entity.Reminder;
import com.monday.app.reminder.repository.ReminderRepository;
import com.monday.app.vehicle.entity.*;
import com.monday.app.vehicle.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HomeDashboardService {

    private static final Logger log = LoggerFactory.getLogger(HomeDashboardService.class);
    private static final int DEFAULT_WORK_DAY_MINUTES = 480; // 8 hours

    private final EventRepository eventRepository;
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final MilestoneRepository milestoneRepository;
    private final ReminderRepository reminderRepository;
    private final CreditAccountRepository creditAccountRepository;
    private final InvestmentRepository investmentRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetCategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final NamedParameterJdbcTemplate jdbc;

    // Cross-module extensions
    private final VehicleRepository vehicleRepository;
    private final VehicleMaintenanceScheduleRepository vehicleScheduleRepository;
    private final VehicleDocumentRepository vehicleDocumentRepository;
    private final VehicleTaskRepository vehicleTaskRepository;
    private final VehicleObligationRepository vehicleObligationRepository;
    private final AdministrationOverviewService adminOverviewService;
    private final CareerService careerService;
    private final ArticleRepository articleRepository;
    private final ArticleSummaryRepository summaryRepository;
    private final ArticleSourceRepository articleSourceRepository;
    private final DashboardStatsService dashboardStatsService;
    private final ObjectMapper objectMapper;

    public HomeDashboardService(EventRepository eventRepository,
                                TaskRepository taskRepository,
                                ProjectRepository projectRepository,
                                MilestoneRepository milestoneRepository,
                                ReminderRepository reminderRepository,
                                CreditAccountRepository creditAccountRepository,
                                InvestmentRepository investmentRepository,
                                BudgetRepository budgetRepository,
                                BudgetCategoryRepository categoryRepository,
                                TransactionRepository transactionRepository,
                                NamedParameterJdbcTemplate jdbc) {
        this(eventRepository, taskRepository, projectRepository, milestoneRepository, reminderRepository,
                creditAccountRepository, investmentRepository, budgetRepository, categoryRepository, transactionRepository,
                jdbc, null, null, null, null, null, null, null, null, null, null, null, new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @Autowired
    public HomeDashboardService(EventRepository eventRepository,
                                TaskRepository taskRepository,
                                ProjectRepository projectRepository,
                                MilestoneRepository milestoneRepository,
                                ReminderRepository reminderRepository,
                                CreditAccountRepository creditAccountRepository,
                                InvestmentRepository investmentRepository,
                                BudgetRepository budgetRepository,
                                BudgetCategoryRepository categoryRepository,
                                TransactionRepository transactionRepository,
                                NamedParameterJdbcTemplate jdbc,
                                VehicleRepository vehicleRepository,
                                VehicleMaintenanceScheduleRepository vehicleScheduleRepository,
                                VehicleDocumentRepository vehicleDocumentRepository,
                                VehicleTaskRepository vehicleTaskRepository,
                                VehicleObligationRepository vehicleObligationRepository,
                                AdministrationOverviewService adminOverviewService,
                                CareerService careerService,
                                ArticleRepository articleRepository,
                                ArticleSummaryRepository summaryRepository,
                                ArticleSourceRepository articleSourceRepository,
                                DashboardStatsService dashboardStatsService,
                                ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.milestoneRepository = milestoneRepository;
        this.reminderRepository = reminderRepository;
        this.creditAccountRepository = creditAccountRepository;
        this.investmentRepository = investmentRepository;
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.jdbc = jdbc;
        this.vehicleRepository = vehicleRepository;
        this.vehicleScheduleRepository = vehicleScheduleRepository;
        this.vehicleDocumentRepository = vehicleDocumentRepository;
        this.vehicleTaskRepository = vehicleTaskRepository;
        this.vehicleObligationRepository = vehicleObligationRepository;
        this.adminOverviewService = adminOverviewService;
        this.careerService = careerService;
        this.articleRepository = articleRepository;
        this.summaryRepository = summaryRepository;
        this.articleSourceRepository = articleSourceRepository;
        this.dashboardStatsService = dashboardStatsService;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public HomeDashboardResponse getDashboard(LocalDate date, ZoneId zoneId) {
        if (date == null) {
            date = LocalDate.now(zoneId != null ? zoneId : ZoneId.systemDefault());
        }
        if (zoneId == null) {
            zoneId = ZoneId.of("UTC");
        }

        Instant startOfDay = date.atStartOfDay(zoneId).toInstant();
        Instant endOfDay = date.plusDays(1).atStartOfDay(zoneId).toInstant();

        // 1. Schedule & Events
        ScheduleDashboardDto schedule = calculateSchedule(date, startOfDay, endOfDay);

        // 2. Tasks
        TaskOverviewDto tasks = calculateTasks(startOfDay, endOfDay);

        // 3. Projects & Milestones
        ProjectOverviewDto projects = calculateProjects(startOfDay);

        // 4. Financial Obligations
        FinanceObligationsDto finances = calculateFinances(date);

        // 5. Budget & Spending
        BudgetSpendingDto budget = calculateBudget(date);

        // 6. Work Capacity
        WorkCapacityDto capacity = calculateCapacity(schedule.scheduledMinutes(), tasks.totalEstimatedMinutes());

        // 7. Vehicles
        VehicleAgendaSummaryDto vehicles = calculateVehicles(date);

        // 8. Administration
        AdministrationAgendaSummaryDto administration = calculateAdministration(date);

        // 9. Career
        CareerAgendaSummaryDto career = calculateCareer(date);

        // 10. Intelligence
        IntelligenceAgendaSummaryDto intelligence = calculateIntelligence(date);

        return new HomeDashboardResponse(
                date,
                schedule,
                tasks,
                projects,
                finances,
                budget,
                capacity,
                vehicles,
                administration,
                career,
                intelligence,
                Instant.now()
        );
    }

    private ScheduleDashboardDto calculateSchedule(LocalDate date, Instant startOfDay, Instant endOfDay) {
        List<Event> rawEvents = eventRepository.findUpcoming(startOfDay, endOfDay);

        List<EventSummaryDto> eventSummaries = new ArrayList<>();
        int scheduledMinutes = 0;
        int allDayCount = 0;

        for (Event e : rawEvents) {
            if ("CANCELLED".equalsIgnoreCase(e.getStatus())) {
                continue;
            }

            int durationMinutes = 0;
            if (e.isAllDay()) {
                allDayCount++;
            } else if (e.getStartAt() != null && e.getEndAt() != null) {
                durationMinutes = (int) Duration.between(e.getStartAt(), e.getEndAt()).toMinutes();
                if (durationMinutes > 0) {
                    scheduledMinutes += durationMinutes;
                }
            }

            eventSummaries.add(new EventSummaryDto(
                    e.getId(),
                    e.getTitle(),
                    e.getDescription(),
                    e.getEventType(),
                    e.getStartAt(),
                    e.getEndAt(),
                    e.isAllDay(),
                    e.getLocation(),
                    e.getStatus(),
                    e.isRequiresPrep(),
                    e.getPrepTitle(),
                    e.getPrepDueText()
            ));
        }

        // Reminders
        List<Reminder> remindersToday = reminderRepository.findPendingDueBefore(endOfDay).stream()
                .filter(r -> r.getRemindAt() != null && !r.getRemindAt().isBefore(startOfDay))
                .toList();

        // Incomplete prep items for today's events
        List<EventPrepItemDto> incompletePrep = new ArrayList<>();
        if (!eventSummaries.isEmpty()) {
            String prepSql = """
                    SELECT epi.id, epi.event_id, e.title as event_title, epi.text, epi.completed, epi.due
                    FROM event_prep_items epi
                    JOIN events e ON epi.event_id = e.id
                    WHERE epi.completed = false
                      AND e.start_at >= :startOfDay AND e.start_at < :endOfDay
                    ORDER BY epi.due NULLS LAST, epi.created_at
                    """;
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("startOfDay", Timestamp.from(startOfDay))
                    .addValue("endOfDay", Timestamp.from(endOfDay));

            incompletePrep = jdbc.query(prepSql, params, (rs, rowNum) -> new EventPrepItemDto(
                    UUID.fromString(rs.getString("id")),
                    UUID.fromString(rs.getString("event_id")),
                    rs.getString("event_title"),
                    rs.getString("text"),
                    rs.getBoolean("completed"),
                    rs.getDate("due") != null ? rs.getDate("due").toLocalDate() : null
            ));
        }

        // Detect conflicts between timed events
        List<ScheduleConflictDto> conflicts = detectConflicts(eventSummaries);

        return new ScheduleDashboardDto(
                eventSummaries,
                remindersToday,
                incompletePrep,
                conflicts,
                eventSummaries.size(),
                allDayCount,
                scheduledMinutes
        );
    }

    private List<ScheduleConflictDto> detectConflicts(List<EventSummaryDto> events) {
        List<ScheduleConflictDto> conflicts = new ArrayList<>();
        List<EventSummaryDto> timedEvents = events.stream()
                .filter(e -> !e.allDay() && e.startAt() != null && e.endAt() != null && e.endAt().isAfter(e.startAt()))
                .sorted(Comparator.comparing(EventSummaryDto::startAt))
                .toList();

        for (int i = 0; i < timedEvents.size(); i++) {
            EventSummaryDto e1 = timedEvents.get(i);
            for (int j = i + 1; j < timedEvents.size(); j++) {
                EventSummaryDto e2 = timedEvents.get(j);
                if (e2.startAt().isBefore(e1.endAt())) {
                    // Overlap detected
                    Instant overlapStart = e2.startAt();
                    Instant overlapEnd = e1.endAt().isBefore(e2.endAt()) ? e1.endAt() : e2.endAt();
                    long overlapMin = Duration.between(overlapStart, overlapEnd).toMinutes();
                    if (overlapMin > 0) {
                        conflicts.add(new ScheduleConflictDto(
                                e1.id(), e1.title(), e1.startAt(), e1.endAt(),
                                e2.id(), e2.title(), e2.startAt(), e2.endAt(),
                                overlapMin
                        ));
                    }
                } else {
                    break; // Since sorted by start time
                }
            }
        }
        return conflicts;
    }

    private TaskOverviewDto calculateTasks(Instant startOfDay, Instant endOfDay) {
        List<Task> allTasks = taskRepository.findAll();
        Map<UUID, String> projectNames = projectRepository.findAll().stream()
                .collect(Collectors.toMap(Project::getId, Project::getName, (a, b) -> a));
        Map<UUID, String> milestoneNames = milestoneRepository.findAll().stream()
                .collect(Collectors.toMap(Milestone::getId, Milestone::getName, (a, b) -> a));

        List<TaskSummaryDto> dueToday = new ArrayList<>();
        List<TaskSummaryDto> overdue = new ArrayList<>();
        List<TaskSummaryDto> highPriority = new ArrayList<>();
        List<TaskSummaryDto> inProgress = new ArrayList<>();

        int totalEstimatedMinutes = 0;

        for (Task t : allTasks) {
            if ("COMPLETED".equalsIgnoreCase(t.getStatus()) || "CANCELLED".equalsIgnoreCase(t.getStatus())) {
                continue;
            }

            TaskSummaryDto dto = new TaskSummaryDto(
                    t.getId(),
                    t.getTitle(),
                    t.getDescription(),
                    t.getProjectId(),
                    t.getProjectId() != null ? projectNames.get(t.getProjectId()) : null,
                    t.getMilestoneId(),
                    t.getMilestoneId() != null ? milestoneNames.get(t.getMilestoneId()) : null,
                    t.getStatus(),
                    t.getPriority(),
                    t.getDueAt(),
                    t.getEstimatedMinutes(),
                    t.isRecurring()
            );

            boolean isDueToday = t.getDueAt() != null && !t.getDueAt().isBefore(startOfDay) && t.getDueAt().isBefore(endOfDay);
            boolean isOverdue = t.getDueAt() != null && t.getDueAt().isBefore(startOfDay);
            boolean isHighPriority = "HIGH".equalsIgnoreCase(t.getPriority()) ||
                    "CRITICAL".equalsIgnoreCase(t.getPriority()) ||
                    "URGENT".equalsIgnoreCase(t.getPriority());
            boolean isInProgress = "IN_PROGRESS".equalsIgnoreCase(t.getStatus());

            if (isDueToday) {
                dueToday.add(dto);
                if (t.getEstimatedMinutes() != null && t.getEstimatedMinutes() > 0) {
                    totalEstimatedMinutes += t.getEstimatedMinutes();
                }
            } else if (isOverdue) {
                overdue.add(dto);
                if (t.getEstimatedMinutes() != null && t.getEstimatedMinutes() > 0) {
                    totalEstimatedMinutes += t.getEstimatedMinutes();
                }
            }

            if (isHighPriority) {
                highPriority.add(dto);
            }
            if (isInProgress) {
                inProgress.add(dto);
            }
        }

        // Sort overdue by dueAt ASC (oldest overdue first)
        overdue.sort(Comparator.comparing(TaskSummaryDto::dueAt, Comparator.nullsLast(Comparator.naturalOrder())));
        // Sort due today by dueAt ASC, then priority
        dueToday.sort(Comparator.comparing(TaskSummaryDto::dueAt, Comparator.nullsLast(Comparator.naturalOrder())));

        return new TaskOverviewDto(
                dueToday.size(),
                overdue.size(),
                highPriority.size(),
                inProgress.size(),
                totalEstimatedMinutes,
                dueToday,
                overdue,
                highPriority,
                inProgress
        );
    }

    private ProjectOverviewDto calculateProjects(Instant startOfDay) {
        List<Project> allProjects = projectRepository.findAll();
        List<ProjectProgressSummaryDto> activeProjects = new ArrayList<>();

        for (Project p : allProjects) {
            if ("COMPLETED".equalsIgnoreCase(p.getStatus()) || "ARCHIVED".equalsIgnoreCase(p.getStatus())) {
                continue;
            }

            List<Task> projectTasks = taskRepository.findByProjectId(p.getId());
            int total = projectTasks.size();
            int completed = (int) projectTasks.stream()
                    .filter(t -> "COMPLETED".equalsIgnoreCase(t.getStatus()))
                    .count();

            Integer progressPercent = total > 0 ? (completed * 100) / total : null;

            // Find next milestone
            Optional<Milestone> nextMilestone = milestoneRepository.findNextByProjectId(p.getId());
            String nextMilestoneName = nextMilestone.map(Milestone::getName).orElse(null);
            Instant nextMilestoneDueAt = nextMilestone.map(Milestone::getDueAt).orElse(null);

            activeProjects.add(new ProjectProgressSummaryDto(
                    p.getId(),
                    p.getName(),
                    p.getDescription(),
                    p.getCategory(),
                    p.getStatus(),
                    p.getPriority(),
                    p.getDueAt(),
                    total,
                    completed,
                    progressPercent,
                    nextMilestoneName,
                    nextMilestoneDueAt
            ));
        }

        // Upcoming milestones across all projects within next 14 days
        Map<UUID, String> projectNames = allProjects.stream()
                .collect(Collectors.toMap(Project::getId, Project::getName, (a, b) -> a));

        List<Milestone> rawMilestones = milestoneRepository.findImpendingMilestones(startOfDay.plus(14, ChronoUnit.DAYS));
        List<MilestoneSummaryDto> upcomingMilestones = rawMilestones.stream()
                .map(m -> new MilestoneSummaryDto(
                        m.getId(),
                        m.getProjectId(),
                        m.getProjectId() != null ? projectNames.get(m.getProjectId()) : null,
                        m.getName(),
                        m.getDescription(),
                        m.getStatus(),
                        m.getPriority(),
                        m.getDueAt()
                ))
                .toList();

        // Recent deep work activity in last 7 days
        DeepWorkSummaryDto deepWork = calculateDeepWork();

        return new ProjectOverviewDto(
                activeProjects.size(),
                activeProjects,
                upcomingMilestones,
                deepWork
        );
    }

    private DeepWorkSummaryDto calculateDeepWork() {
        try {
            String sql = """
                    SELECT COALESCE(SUM(duration_minutes), 0) as total_minutes,
                           COUNT(*) as session_count,
                           COALESCE(AVG(focus_score), 100) as avg_focus
                    FROM deep_work_sessions
                    WHERE started_at >= NOW() - INTERVAL '7 days'
                    """;
            return jdbc.queryForObject(sql, new MapSqlParameterSource(), (rs, rowNum) -> {
                int totalMin = rs.getInt("total_minutes");
                int sessions = rs.getInt("session_count");
                int avgFocus = rs.getInt("avg_focus");
                double hours = BigDecimal.valueOf(totalMin / 60.0).setScale(1, RoundingMode.HALF_UP).doubleValue();
                return new DeepWorkSummaryDto(totalMin, hours, 5.0, sessions, avgFocus);
            });
        } catch (Exception e) {
            log.warn("Could not query deep_work_sessions: {}", e.getMessage());
            return new DeepWorkSummaryDto(0, 0.0, 5.0, 0, 100);
        }
    }

    private FinanceObligationsDto calculateFinances(LocalDate date) {
        // Query commitments
        String commitmentSql = """
                SELECT fc.id, fc.name, fc.due_date, fc.amount, fc.account_id,
                       fa.account_name, fc.is_urgent, fc.status
                FROM finance_commitments fc
                LEFT JOIN finance_accounts fa ON fc.account_id = fa.id
                WHERE fc.status != 'PAID'
                ORDER BY fc.due_date ASC
                """;

        List<CommitmentSummaryDto> rawCommitments = jdbc.query(commitmentSql, (rs, rowNum) -> new CommitmentSummaryDto(
                UUID.fromString(rs.getString("id")),
                rs.getString("name"),
                rs.getDate("due_date").toLocalDate(),
                rs.getBigDecimal("amount"),
                rs.getString("account_id") != null ? UUID.fromString(rs.getString("account_id")) : null,
                rs.getString("account_name"),
                rs.getBoolean("is_urgent"),
                rs.getString("status")
        ));

        // Credit cards
        List<CreditAccount> rawCards = creditAccountRepository.findAll();
        List<CreditCardSummaryDto> creditCards = new ArrayList<>();
        BigDecimal totalBalance = BigDecimal.ZERO;
        BigDecimal totalLimit = BigDecimal.ZERO;

        Set<String> cardNamesLower = new HashSet<>();

        for (CreditAccount c : rawCards) {
            cardNamesLower.add(c.getCardName() != null ? c.getCardName().toLowerCase().trim() : "");
            BigDecimal limit = c.getCreditLimit() != null ? c.getCreditLimit() : BigDecimal.ZERO;
            BigDecimal balance = c.getCurrentBalance() != null ? c.getCurrentBalance() : BigDecimal.ZERO;

            totalBalance = totalBalance.add(balance);
            totalLimit = totalLimit.add(limit);

            Double util = null;
            if (limit.compareTo(BigDecimal.ZERO) > 0) {
                util = balance.divide(limit, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue();
            }

            creditCards.add(new CreditCardSummaryDto(
                    c.getId(),
                    c.getCardId(),
                    c.getCardName(),
                    c.getLastFour(),
                    c.getNetwork(),
                    limit,
                    balance,
                    c.getStatementClosingDate(),
                    c.getPaymentDueDate(),
                    c.getMinAmountDue(),
                    c.isAutoPayEnabled(),
                    util
            ));
        }

        // Deduplicate commitments against credit cards
        List<CommitmentSummaryDto> dueToday = new ArrayList<>();
        List<CommitmentSummaryDto> dueSoon = new ArrayList<>();
        List<CommitmentSummaryDto> overdue = new ArrayList<>();
        BigDecimal totalDueSoonAmount = BigDecimal.ZERO;

        LocalDate soonCutoff = date.plusDays(7);

        for (CommitmentSummaryDto c : rawCommitments) {
            // Deduplication check: if this commitment matches a credit card name or is linked
            boolean duplicateWithCard = cardNamesLower.stream()
                    .anyMatch(cn -> !cn.isBlank() && c.name().toLowerCase().contains(cn));

            if (duplicateWithCard) {
                // If it is already tracked in credit accounts, avoid double-counting in commitments list
                continue;
            }

            if (c.dueDate().isEqual(date)) {
                dueToday.add(c);
                totalDueSoonAmount = totalDueSoonAmount.add(c.amount() != null ? c.amount() : BigDecimal.ZERO);
            } else if (c.dueDate().isBefore(date)) {
                overdue.add(c);
            } else if (c.dueDate().isBefore(soonCutoff.plusDays(1))) {
                dueSoon.add(c);
                totalDueSoonAmount = totalDueSoonAmount.add(c.amount() != null ? c.amount() : BigDecimal.ZERO);
            }
        }

        // Overall credit utilization
        Double overallUtil = null;
        boolean utilAvailable = totalLimit.compareTo(BigDecimal.ZERO) > 0;
        if (utilAvailable) {
            overallUtil = totalBalance.divide(totalLimit, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
        }
        CreditUtilizationSummaryDto overallCreditUtilization = new CreditUtilizationSummaryDto(
                totalBalance,
                totalLimit,
                overallUtil,
                utilAvailable
        );

        // Upcoming SIP investments
        List<Investment> allInvestments = investmentRepository.findAll();
        List<InvestmentSipSummaryDto> upcomingSips = allInvestments.stream()
                .filter(i -> "SIP".equalsIgnoreCase(i.getInvestmentType()) && "ACTIVE".equalsIgnoreCase(i.getStatus()))
                .map(i -> new InvestmentSipSummaryDto(
                        i.getId(),
                        i.getName(),
                        i.getInstrument(),
                        i.getAssetClass(),
                        i.getSipAmount(),
                        i.getSipDay(),
                        i.getStatus(),
                        "Scheduled auto-debit contribution target"
                ))
                .toList();

        return new FinanceObligationsDto(
                dueToday,
                dueSoon,
                overdue,
                totalDueSoonAmount,
                creditCards,
                overallCreditUtilization,
                upcomingSips
        );
    }

    private BudgetSpendingDto calculateBudget(LocalDate date) {
        YearMonth currentYm = YearMonth.from(date);
        LocalDate monthStart = currentYm.atDay(1);
        LocalDate monthEnd = currentYm.atEndOfMonth();

        // 1. Total budgeted from budgets table
        List<Budget> allBudgets = budgetRepository.findAll();
        BigDecimal totalBudgeted = BigDecimal.ZERO;
        for (Budget b : allBudgets) {
            if (b.getAmount() != null) {
                totalBudgeted = totalBudgeted.add(b.getAmount());
            }
        }

        // Fallback default monthly allowance if no budgets created yet
        if (totalBudgeted.compareTo(BigDecimal.ZERO) == 0) {
            totalBudgeted = BigDecimal.valueOf(75000.00);
        }

        // 2. Spending for current month: transactions where type in ('EXPENSE', 'DEBIT')
        String spendSql = """
                SELECT COALESCE(SUM(amount), 0) FROM transactions
                WHERE LOWER(transaction_type) IN ('expense', 'debit')
                  AND transaction_at >= :start AND transaction_at <= :end
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("start", Timestamp.valueOf(monthStart.atStartOfDay()))
                .addValue("end", Timestamp.valueOf(monthEnd.atTime(LocalTime.MAX)));

        BigDecimal totalSpent = jdbc.queryForObject(spendSql, params, BigDecimal.class);
        if (totalSpent == null) totalSpent = BigDecimal.ZERO;

        BigDecimal remaining = totalBudgeted.subtract(totalSpent).max(BigDecimal.ZERO);
        Double consumptionPercent = totalBudgeted.compareTo(BigDecimal.ZERO) > 0
                ? totalSpent.divide(totalBudgeted, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

        // Categories spending
        List<BudgetCategory> categories = categoryRepository.findAll();
        List<CategorySpendingSummaryDto> categoryDtos = new ArrayList<>();

        for (BudgetCategory cat : categories) {
            // Find budget for this category
            List<Budget> catBudgets = budgetRepository.findByCategoryId(cat.getId());
            BigDecimal catBudgeted = catBudgets.stream()
                    .map(Budget::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Find spent for this category
            String catSpendSql = """
                    SELECT COALESCE(SUM(amount), 0) FROM transactions
                    WHERE category_id = :catId
                      AND LOWER(transaction_type) IN ('expense', 'debit')
                      AND transaction_at >= :start AND transaction_at <= :end
                    """;
            MapSqlParameterSource catParams = new MapSqlParameterSource()
                    .addValue("catId", cat.getId())
                    .addValue("start", Timestamp.valueOf(monthStart.atStartOfDay()))
                    .addValue("end", Timestamp.valueOf(monthEnd.atTime(LocalTime.MAX)));
            BigDecimal catSpent = jdbc.queryForObject(catSpendSql, catParams, BigDecimal.class);
            if (catSpent == null) catSpent = BigDecimal.ZERO;

            BigDecimal catRemaining = catBudgeted.subtract(catSpent).max(BigDecimal.ZERO);
            Double catConsumption = catBudgeted.compareTo(BigDecimal.ZERO) > 0
                    ? catSpent.divide(catBudgeted, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                    : 0.0;

            categoryDtos.add(new CategorySpendingSummaryDto(
                    cat.getId(),
                    cat.getName(),
                    catBudgeted,
                    catSpent,
                    catRemaining,
                    catConsumption,
                    catConsumption > 100.0,
                    catConsumption >= 80.0
            ));
        }

        // Recent 5 expense transactions
        String recentTxSql = """
                SELECT t.id, t.description, t.amount, t.transaction_at,
                       bc.name as category_name, t.transaction_type, t.payment_method
                FROM transactions t
                LEFT JOIN budget_categories bc ON t.category_id = bc.id
                WHERE LOWER(t.transaction_type) IN ('expense', 'debit')
                ORDER BY t.transaction_at DESC
                LIMIT 5
                """;
        List<RecentTransactionSummaryDto> recentSpending = jdbc.query(recentTxSql, (rs, rowNum) -> new RecentTransactionSummaryDto(
                UUID.fromString(rs.getString("id")),
                rs.getString("description"),
                rs.getBigDecimal("amount"),
                rs.getTimestamp("transaction_at").toInstant(),
                rs.getString("category_name"),
                rs.getString("transaction_type"),
                rs.getString("payment_method")
        ));

        return new BudgetSpendingDto(
                totalBudgeted,
                totalSpent,
                remaining,
                consumptionPercent,
                categoryDtos,
                recentSpending
        );
    }

    private WorkCapacityDto calculateCapacity(int scheduledEventMinutes, int estimatedTaskWorkloadMinutes) {
        int availableMinutes = Math.max(0, DEFAULT_WORK_DAY_MINUTES - scheduledEventMinutes);
        boolean isOverloaded = (estimatedTaskWorkloadMinutes > availableMinutes) || (availableMinutes < 60);

        return new WorkCapacityDto(
                DEFAULT_WORK_DAY_MINUTES,
                scheduledEventMinutes,
                availableMinutes,
                estimatedTaskWorkloadMinutes,
                isOverloaded
        );
    }

    private VehicleAgendaSummaryDto calculateVehicles(LocalDate date) {
        if (vehicleRepository == null) {
            return new VehicleAgendaSummaryDto(0, null, null, 0, 0, Collections.emptyList(), 0, 0, Collections.emptyList(), 0, Collections.emptyList(), Collections.emptyList());
        }

        List<Vehicle> allVehicles = vehicleRepository.findAll();
        if (allVehicles.isEmpty()) {
            return new VehicleAgendaSummaryDto(0, null, null, 0, 0, Collections.emptyList(), 0, 0, Collections.emptyList(), 0, Collections.emptyList(), Collections.emptyList());
        }

        Vehicle primary = allVehicles.stream().filter(v -> Boolean.TRUE.equals(v.getIsPrimary())).findFirst().orElse(allVehicles.get(0));
        String primaryName = primary.getName();
        Integer primaryKm = primary.getCurrentKm();

        List<VehicleAgendaSummaryDto.VehicleMaintenanceItemDto> upcomingMaint = new ArrayList<>();
        int maintDueCount = 0;
        int maintOverdueCount = 0;

        List<VehicleAgendaSummaryDto.VehicleDocExpiryDto> expiringDocs = new ArrayList<>();
        int docsExpiringCount = 0;
        int docsExpiredCount = 0;

        List<VehicleAgendaSummaryDto.VehicleTaskSummaryDto> pendingTasks = new ArrayList<>();
        List<VehicleAgendaSummaryDto.VehicleObligationSummaryDto> upcomingObligations = new ArrayList<>();

        for (Vehicle v : allVehicles) {
            String vName = v.getName();
            int currentKm = v.getCurrentKm();

            if (vehicleScheduleRepository != null) {
                List<VehicleMaintenanceSchedule> schedules = vehicleScheduleRepository.findByVehicleId(v.getId());
                for (VehicleMaintenanceSchedule s : schedules) {
                    boolean overdue = false;
                    boolean dueSoon = false;
                    Long days = s.getNextDueAt() != null ? ChronoUnit.DAYS.between(date, s.getNextDueAt()) : null;
                    Integer kmRem = s.getNextDueKm() != null ? s.getNextDueKm() - currentKm : null;

                    if ((days != null && days < 0) || (kmRem != null && kmRem < 0)) {
                        overdue = true;
                        maintOverdueCount++;
                    } else if ((days != null && days <= 30) || (kmRem != null && kmRem <= 1000)) {
                        dueSoon = true;
                        maintDueCount++;
                    }

                    if (overdue || dueSoon) {
                        upcomingMaint.add(new VehicleAgendaSummaryDto.VehicleMaintenanceItemDto(
                                s.getId(),
                                v.getId(),
                                vName,
                                s.getItemName(),
                                s.getNextDueAt(),
                                s.getNextDueKm(),
                                overdue
                        ));
                    }
                }
            }

            if (vehicleDocumentRepository != null) {
                List<VehicleDocument> docs = vehicleDocumentRepository.findByVehicleId(v.getId());
                for (VehicleDocument d : docs) {
                    if (d.getExpiryDate() != null) {
                        long days = ChronoUnit.DAYS.between(date, d.getExpiryDate());
                        if (days < 0) {
                            docsExpiredCount++;
                            expiringDocs.add(new VehicleAgendaSummaryDto.VehicleDocExpiryDto(
                                    d.getId(), v.getId(), vName, d.getDocType(), d.getTitle(), d.getExpiryDate(), days, true
                            ));
                        } else if (days <= 30) {
                            docsExpiringCount++;
                            expiringDocs.add(new VehicleAgendaSummaryDto.VehicleDocExpiryDto(
                                    d.getId(), v.getId(), vName, d.getDocType(), d.getTitle(), d.getExpiryDate(), days, false
                            ));
                        }
                    }
                }
            }

            if (vehicleTaskRepository != null) {
                List<VehicleTask> tasks = vehicleTaskRepository.findByVehicleId(v.getId());
                for (VehicleTask t : tasks) {
                    if (!Boolean.TRUE.equals(t.getCompleted())) {
                        pendingTasks.add(new VehicleAgendaSummaryDto.VehicleTaskSummaryDto(
                                t.getId(), v.getId(), vName, t.getTitle(), t.getPriority()
                        ));
                    }
                }
            }

            if (vehicleObligationRepository != null) {
                List<VehicleObligation> obligations = vehicleObligationRepository.findByVehicleId(v.getId());
                for (VehicleObligation o : obligations) {
                    if (o.getDueDate() != null && !o.getDueDate().isAfter(date.plusDays(30)) && !"PAID".equalsIgnoreCase(o.getStatus())) {
                        upcomingObligations.add(new VehicleAgendaSummaryDto.VehicleObligationSummaryDto(
                                o.getId(), v.getId(), vName, o.getObligationType(), o.getDueDate(), o.getAmount(), o.getStatus()
                        ));
                    }
                }
            }
        }

        return new VehicleAgendaSummaryDto(
                allVehicles.size(),
                primaryName,
                primaryKm,
                maintDueCount,
                maintOverdueCount,
                upcomingMaint,
                docsExpiringCount,
                docsExpiredCount,
                expiringDocs,
                pendingTasks.size(),
                pendingTasks,
                upcomingObligations
        );
    }

    private AdministrationAgendaSummaryDto calculateAdministration(LocalDate date) {
        if (adminOverviewService == null) {
            return new AdministrationAgendaSummaryDto(
                    0, 0, 0, BigDecimal.ZERO, Collections.emptyList(), 0, Collections.emptyList(), 0, Collections.emptyList(), 0, Collections.emptyList(), 0
            );
        }

        AdministrationOverviewDto overview = adminOverviewService.getOverview();

        List<AdministrationAgendaSummaryDto.AdminObligationSummaryDto> obligations = overview.upcomingObligations() != null
                ? overview.upcomingObligations().stream()
                .map(o -> new AdministrationAgendaSummaryDto.AdminObligationSummaryDto(
                        o.id(),
                        o.name(),
                        o.obligationType(),
                        o.category(),
                        o.nextDueDate(),
                        o.expectedAmount(),
                        o.currency(),
                        o.isOverdue()
                ))
                .toList()
                : Collections.emptyList();

        List<AdministrationAgendaSummaryDto.AdminImportantDateSummaryDto> dates = overview.upcomingOccasions() != null
                ? overview.upcomingOccasions().stream()
                .map(d -> new AdministrationAgendaSummaryDto.AdminImportantDateSummaryDto(
                        d.id(),
                        d.title(),
                        d.date(),
                        d.dateType(),
                        d.personName(),
                        d.daysRemaining()
                ))
                .toList()
                : Collections.emptyList();

        List<AdministrationAgendaSummaryDto.AdminMaintenanceSummaryDto> maintenance = overview.upcomingMaintenance() != null
                ? overview.upcomingMaintenance().stream()
                .map(m -> new AdministrationAgendaSummaryDto.AdminMaintenanceSummaryDto(
                        m.id(),
                        m.gadgetName(),
                        m.title(),
                        m.maintenanceType(),
                        m.dueDate(),
                        m.isOverdue()
                ))
                .toList()
                : Collections.emptyList();

        List<AdministrationAgendaSummaryDto.AdminAppointmentSummaryDto> appointments = overview.upcomingAppointments() != null
                ? overview.upcomingAppointments().stream()
                .map(a -> new AdministrationAgendaSummaryDto.AdminAppointmentSummaryDto(
                        a.id(),
                        a.title(),
                        a.startAt(),
                        a.location()
                ))
                .toList()
                : Collections.emptyList();

        return new AdministrationAgendaSummaryDto(
                overview.totalActiveObligations(),
                overview.obligationsDueSoonCount(),
                overview.obligationsOverdueCount(),
                overview.monthlySubscriptionCommitment() != null ? overview.monthlySubscriptionCommitment() : BigDecimal.ZERO,
                obligations,
                overview.upcomingOccasionsCount(),
                dates,
                overview.maintenanceDueCount(),
                maintenance,
                overview.upcomingAppointmentsCount(),
                appointments,
                overview.pendingChecklistItemsCount()
        );
    }

    private CareerAgendaSummaryDto calculateCareer(LocalDate date) {
        if (careerService == null) {
            return new CareerAgendaSummaryDto(
                    false, null, null, null, null, null, null, 0, 0, 0, 0, 0.0, Collections.emptyList(), Collections.emptyList()
            );
        }

        Optional<CareerPlan> optPlan = careerService.getCurrentPlan();
        if (optPlan.isEmpty()) {
            return new CareerAgendaSummaryDto(
                    false, null, null, null, null, null, null, 0, 0, 0, 0, 0.0, Collections.emptyList(), Collections.emptyList()
            );
        }

        CareerPlan plan = optPlan.get();
        Long daysToTarget = plan.getTargetDate() != null ? ChronoUnit.DAYS.between(date, plan.getTargetDate()) : null;

        List<CareerAgendaSummaryDto.CareerMilestoneSummaryDto> activeMilestones = new ArrayList<>();
        List<CareerAgendaSummaryDto.CareerTaskSummaryDto> upcomingTasks = new ArrayList<>();

        int totalMilestones = 0;
        int completedMilestones = 0;
        int totalTasks = 0;
        int completedTasks = 0;

        try {
            if (plan.getPlanData() != null && !plan.getPlanData().isBlank()) {
                JsonNode root = objectMapper.readTree(plan.getPlanData());
                JsonNode milestonesNode = root.path("milestones");
                if (milestonesNode.isArray()) {
                    totalMilestones = milestonesNode.size();
                    for (JsonNode m : milestonesNode) {
                        String mId = m.path("id").asText();
                        String mName = m.path("name").asText();
                        LocalDate mDueDate = null;
                        if (m.hasNonNull("dueDate")) {
                            try { mDueDate = LocalDate.parse(m.path("dueDate").asText()); } catch (Exception ignored) {}
                        }
                        String mPriority = m.path("priority").asText("p1");

                        JsonNode tasksNode = m.path("tasks");
                        int mTotalTasks = 0;
                        int mCompletedTasks = 0;
                        if (tasksNode.isArray()) {
                            mTotalTasks = tasksNode.size();
                            for (JsonNode t : tasksNode) {
                                totalTasks++;
                                boolean isCompleted = t.path("isCompleted").asBoolean(false);
                                if (isCompleted) {
                                    completedTasks++;
                                    mCompletedTasks++;
                                } else {
                                    String tId = t.path("id").asText();
                                    String tTitle = t.path("title").asText();
                                    String tPriority = t.path("priority").asText("p1");
                                    LocalDate tDueDate = null;
                                    if (t.hasNonNull("dueDate")) {
                                        try { tDueDate = LocalDate.parse(t.path("dueDate").asText()); } catch (Exception ignored) {}
                                    }
                                    if (upcomingTasks.size() < 10) {
                                        upcomingTasks.add(new CareerAgendaSummaryDto.CareerTaskSummaryDto(
                                                tId, mId, mName, tTitle, tPriority, tDueDate, false
                                        ));
                                    }
                                }
                            }
                        }

                        double mProgress = mTotalTasks > 0 ? (mCompletedTasks * 100.0) / mTotalTasks : 0.0;
                        if (mTotalTasks > 0 && mCompletedTasks == mTotalTasks) {
                            completedMilestones++;
                        } else if (activeMilestones.size() < 5) {
                            activeMilestones.add(new CareerAgendaSummaryDto.CareerMilestoneSummaryDto(
                                    mId, mName, mDueDate, mPriority, Math.round(mProgress * 10.0) / 10.0, mTotalTasks
                            ));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse career plan data JSON for dashboard: {}", e.getMessage());
        }

        double overallProgress = totalTasks > 0 ? Math.round(((completedTasks * 100.0) / totalTasks) * 10.0) / 10.0 : 0.0;

        return new CareerAgendaSummaryDto(
                true,
                plan.getId(),
                plan.getName(),
                plan.getDescription(),
                plan.getStartDate(),
                plan.getTargetDate(),
                daysToTarget,
                totalMilestones,
                completedMilestones,
                totalTasks,
                completedTasks,
                overallProgress,
                activeMilestones,
                upcomingTasks
        );
    }

    private IntelligenceAgendaSummaryDto calculateIntelligence(LocalDate date) {
        long discoveredToday = 0;
        long summariesToday = 0;
        int activeFeeds = 0;

        try {
            if (dashboardStatsService != null) {
                DashboardStatsDto stats = dashboardStatsService.getDashboardStatistics("today");
                if (stats != null) {
                    discoveredToday = stats.articlesDiscovered();
                    summariesToday = stats.summariesGenerated();
                    activeFeeds = stats.activeNodes();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch intelligence stats: {}", e.getMessage());
        }

        List<IntelligenceAgendaSummaryDto.IntelligenceArticleSummaryDto> topArticles = new ArrayList<>();
        try {
            if (articleRepository != null) {
                OffsetDateTime cutoff = date.minusDays(7).atStartOfDay().atOffset(ZoneOffset.UTC);
                List<Article> recent = articleRepository.findRecentArticles(cutoff);
                int limit = Math.min(5, recent.size());
                for (int i = 0; i < limit; i++) {
                    Article a = recent.get(i);
                    Optional<ArticleSummary> summaryOpt = summaryRepository != null
                            ? summaryRepository.findFirstByArticleIdAndStatusOrderByCreatedAtDesc(a.getId(), "SUCCESS")
                            : Optional.empty();
                    String summarySnippet = summaryOpt.map(ArticleSummary::getSummary)
                            .orElse(a.getDescription() != null ? a.getDescription() : "Curated article awaiting briefing generation.");
                    if (summarySnippet.length() > 200) {
                        summarySnippet = summarySnippet.substring(0, 197) + "...";
                    }

                    String sourceName = "Curated Source";
                    if (articleSourceRepository != null) {
                        List<ArticleSource> sources = articleSourceRepository.findByIdArticleIdIn(List.of(a.getId()));
                        if (!sources.isEmpty() && sources.get(0).getFeed() != null) {
                            sourceName = sources.get(0).getFeed().getName();
                        } else if (a.getAuthor() != null && !a.getAuthor().isBlank()) {
                            sourceName = a.getAuthor();
                        }
                    }

                    topArticles.add(new IntelligenceAgendaSummaryDto.IntelligenceArticleSummaryDto(
                            a.getId(),
                            a.getTitle(),
                            sourceName,
                            a.getAreaOfInterest(),
                            a.getPublishedAt(),
                            summarySnippet,
                            Boolean.TRUE.equals(a.getIsRead())
                    ));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch top articles for intelligence agenda: {}", e.getMessage());
        }

        return new IntelligenceAgendaSummaryDto(
                discoveredToday,
                summariesToday,
                activeFeeds,
                topArticles
        );
    }
}
