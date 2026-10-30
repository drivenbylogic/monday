package com.monday.app.agenda.service;

import com.monday.app.agenda.dto.HomeDashboardResponse;
import com.monday.app.event.entity.Event;
import com.monday.app.event.repository.EventRepository;
import com.monday.app.finance.entity.CreditAccount;
import com.monday.app.finance.repository.*;
import com.monday.app.project.entity.Milestone;
import com.monday.app.project.entity.Project;
import com.monday.app.project.entity.Task;
import com.monday.app.project.repository.MilestoneRepository;
import com.monday.app.project.repository.ProjectRepository;
import com.monday.app.project.repository.TaskRepository;
import com.monday.app.reminder.repository.ReminderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HomeDashboardServiceTest {

    private EventRepository eventRepository;
    private TaskRepository taskRepository;
    private ProjectRepository projectRepository;
    private MilestoneRepository milestoneRepository;
    private ReminderRepository reminderRepository;
    private CreditAccountRepository creditAccountRepository;
    private InvestmentRepository investmentRepository;
    private BudgetRepository budgetRepository;
    private BudgetCategoryRepository categoryRepository;
    private TransactionRepository transactionRepository;
    private NamedParameterJdbcTemplate jdbc;

    private HomeDashboardService service;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        taskRepository = mock(TaskRepository.class);
        projectRepository = mock(ProjectRepository.class);
        milestoneRepository = mock(MilestoneRepository.class);
        reminderRepository = mock(ReminderRepository.class);
        creditAccountRepository = mock(CreditAccountRepository.class);
        investmentRepository = mock(InvestmentRepository.class);
        budgetRepository = mock(BudgetRepository.class);
        categoryRepository = mock(BudgetCategoryRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        jdbc = mock(NamedParameterJdbcTemplate.class);

        service = new HomeDashboardService(
                eventRepository,
                taskRepository,
                projectRepository,
                milestoneRepository,
                reminderRepository,
                creditAccountRepository,
                investmentRepository,
                budgetRepository,
                categoryRepository,
                transactionRepository,
                jdbc
        );
    }

    @Test
    void getDashboard_partitionsTasksCorrectlyAndExcludesCompleted() {
        LocalDate date = LocalDate.of(2026, 10, 26);
        ZoneId zone = ZoneId.of("UTC");
        Instant startOfDay = date.atStartOfDay(zone).toInstant();

        UUID pId = UUID.randomUUID();
        Project p = new Project(pId, "Flagship Initiative", "Desc", "WORK", "IN_PROGRESS", "HIGH", "[]", null, null, null, Instant.now(), Instant.now());
        when(projectRepository.findAll()).thenReturn(List.of(p));
        when(milestoneRepository.findAll()).thenReturn(Collections.emptyList());

        Task overdueTask = new Task();
        overdueTask.setId(UUID.randomUUID());
        overdueTask.setTitle("Overdue Item");
        overdueTask.setDueAt(startOfDay.minusSeconds(3600)); // 1 hour before start of today
        overdueTask.setStatus("PENDING");
        overdueTask.setPriority("HIGH");
        overdueTask.setEstimatedMinutes(45);

        Task dueTodayTask = new Task();
        dueTodayTask.setId(UUID.randomUUID());
        dueTodayTask.setTitle("Due Today Item");
        dueTodayTask.setDueAt(startOfDay.plusSeconds(3600)); // 1 hour into today
        dueTodayTask.setStatus("IN_PROGRESS");
        dueTodayTask.setPriority("MEDIUM");
        dueTodayTask.setEstimatedMinutes(30);

        Task completedTask = new Task();
        completedTask.setId(UUID.randomUUID());
        completedTask.setTitle("Completed Item");
        completedTask.setDueAt(startOfDay.minusSeconds(7200));
        completedTask.setStatus("COMPLETED");

        when(taskRepository.findAll()).thenReturn(List.of(overdueTask, dueTodayTask, completedTask));
        when(taskRepository.findByProjectId(pId)).thenReturn(List.of(dueTodayTask));
        when(eventRepository.findUpcoming(any(), any())).thenReturn(Collections.emptyList());
        when(reminderRepository.findPendingDueBefore(any())).thenReturn(Collections.emptyList());
        when(creditAccountRepository.findAll()).thenReturn(Collections.emptyList());
        when(investmentRepository.findAll()).thenReturn(Collections.emptyList());
        when(budgetRepository.findAll()).thenReturn(Collections.emptyList());
        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());

        HomeDashboardResponse response = service.getDashboard(date, zone);

        assertNotNull(response);
        assertEquals(1, response.tasks().overdueCount());
        assertEquals("Overdue Item", response.tasks().overdueTasks().get(0).title());
        assertEquals(1, response.tasks().dueTodayCount());
        assertEquals("Due Today Item", response.tasks().dueTodayTasks().get(0).title());
        assertEquals(75, response.tasks().totalEstimatedMinutes()); // 45 + 30
        assertEquals(1, response.tasks().inProgressCount());

        // Capacity
        assertEquals(480, response.capacity().availableWorkMinutes());
        assertEquals(75, response.capacity().estimatedTaskWorkloadMinutes());
        assertFalse(response.capacity().isOverloaded());
    }

    @Test
    void getDashboard_detectsEventConflictsAndCalculatesDuration() {
        LocalDate date = LocalDate.of(2026, 10, 26);
        ZoneId zone = ZoneId.of("UTC");
        Instant startOfDay = date.atStartOfDay(zone).toInstant();

        Event event1 = new Event();
        event1.setId(UUID.randomUUID());
        event1.setTitle("Strategy Sync");
        event1.setStartAt(startOfDay.plusSeconds(3600 * 10)); // 10:00
        event1.setEndAt(startOfDay.plusSeconds(3600 * 11));   // 11:00
        event1.setStatus("SCHEDULED");
        event1.setAllDay(false);

        Event event2 = new Event();
        event2.setId(UUID.randomUUID());
        event2.setTitle("Executive Review");
        event2.setStartAt(startOfDay.plusSeconds(3600 * 10 + 1800)); // 10:30 (overlaps with event1 by 30 mins)
        event2.setEndAt(startOfDay.plusSeconds(3600 * 11 + 1800));   // 11:30
        event2.setStatus("SCHEDULED");
        event2.setAllDay(false);

        when(eventRepository.findUpcoming(any(), any())).thenReturn(List.of(event1, event2));
        when(taskRepository.findAll()).thenReturn(Collections.emptyList());
        when(projectRepository.findAll()).thenReturn(Collections.emptyList());
        when(milestoneRepository.findAll()).thenReturn(Collections.emptyList());
        when(reminderRepository.findPendingDueBefore(any())).thenReturn(Collections.emptyList());
        when(creditAccountRepository.findAll()).thenReturn(Collections.emptyList());
        when(investmentRepository.findAll()).thenReturn(Collections.emptyList());
        when(budgetRepository.findAll()).thenReturn(Collections.emptyList());
        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());

        HomeDashboardResponse response = service.getDashboard(date, zone);

        assertNotNull(response);
        assertEquals(2, response.schedule().totalEventsCount());
        assertEquals(120, response.schedule().scheduledMinutes()); // 60 + 60
        assertEquals(1, response.schedule().conflicts().size());
        assertEquals(30, response.schedule().conflicts().get(0).overlapMinutes());
        assertEquals("Strategy Sync", response.schedule().conflicts().get(0).title1());
        assertEquals("Executive Review", response.schedule().conflicts().get(0).title2());
    }

    @Test
    void getDashboard_handlesZeroCreditLimitGracefullyWithoutDivisionByZero() {
        LocalDate date = LocalDate.of(2026, 10, 26);
        ZoneId zone = ZoneId.of("UTC");

        CreditAccount zeroLimitCard = new CreditAccount();
        zeroLimitCard.setId(UUID.randomUUID());
        zeroLimitCard.setCardName("Test Card Zero Limit");
        zeroLimitCard.setCreditLimit(BigDecimal.ZERO);
        zeroLimitCard.setCurrentBalance(BigDecimal.valueOf(500.00));

        when(creditAccountRepository.findAll()).thenReturn(List.of(zeroLimitCard));
        when(eventRepository.findUpcoming(any(), any())).thenReturn(Collections.emptyList());
        when(taskRepository.findAll()).thenReturn(Collections.emptyList());
        when(projectRepository.findAll()).thenReturn(Collections.emptyList());
        when(milestoneRepository.findAll()).thenReturn(Collections.emptyList());
        when(reminderRepository.findPendingDueBefore(any())).thenReturn(Collections.emptyList());
        when(investmentRepository.findAll()).thenReturn(Collections.emptyList());
        when(budgetRepository.findAll()).thenReturn(Collections.emptyList());
        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());

        HomeDashboardResponse response = service.getDashboard(date, zone);

        assertNotNull(response);
        assertNull(response.finances().creditCards().get(0).utilizationPercent());
        assertFalse(response.finances().overallCreditUtilization().isAvailable());
        assertNull(response.finances().overallCreditUtilization().overallUtilizationPercent());
    }
}
