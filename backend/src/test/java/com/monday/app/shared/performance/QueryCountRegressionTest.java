package com.monday.app.shared.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.app.administration.dto.AppointmentDto;
import com.monday.app.administration.repository.AdministrationChecklistRepository;
import com.monday.app.administration.service.AdministrationAppointmentService;
import com.monday.app.administration.service.AdministrationChecklistService;
import com.monday.app.agenda.dto.HomeDashboardResponse;
import com.monday.app.agenda.service.HomeDashboardService;
import com.monday.app.event.entity.Event;
import com.monday.app.event.repository.EventRepository;
import com.monday.app.finance.entity.Budget;
import com.monday.app.finance.entity.BudgetCategory;
import com.monday.app.finance.entity.FinanceAccount;
import com.monday.app.finance.entity.Transaction;
import com.monday.app.finance.repository.*;
import com.monday.app.finance.service.FinanceService;
import com.monday.app.intelligence.dto.ArticleFeedItemDto;
import com.monday.app.intelligence.entity.Article;
import com.monday.app.intelligence.entity.ArticleSource;
import com.monday.app.intelligence.repository.ArticleRepository;
import com.monday.app.intelligence.repository.ArticleSourceRepository;
import com.monday.app.intelligence.repository.ArticleSummaryRepository;
import com.monday.app.intelligence.service.ArticleFeedService;
import com.monday.app.project.entity.Milestone;
import com.monday.app.project.entity.Project;
import com.monday.app.project.repository.MilestoneRepository;
import com.monday.app.project.repository.ProjectRepository;
import com.monday.app.project.repository.TaskRepository;
import com.monday.app.vehicle.entity.Vehicle;
import com.monday.app.vehicle.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Automated regression test asserting that query counts remain constant O(1)
 * and do NOT scale linearly with parent entity counts (N+1 prevention).
 */
public class QueryCountRegressionTest {

    // =========================================================================
    // 1. Dashboard: Projects scaling (1 vs 10 vs 30)
    // =========================================================================
    @Test
    @DisplayName("Dashboard calculateProjects repository calls remain constant for 1, 10, and 30 projects")
    void dashboard_projectsScaling_repositoryCallsRemainConstant() {
        for (int projectCount : List.of(1, 10, 30)) {
            TaskRepository taskRepo = mock(TaskRepository.class);
            MilestoneRepository milestoneRepo = mock(MilestoneRepository.class);
            ProjectRepository projectRepo = mock(ProjectRepository.class);
            NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);

            List<Project> projects = new ArrayList<>();
            for (int i = 0; i < projectCount; i++) {
                Project p = new Project();
                p.setId(UUID.randomUUID());
                p.setName("Project " + i);
                p.setStatus("ACTIVE");
                projects.add(p);
            }

            when(projectRepo.findAll()).thenReturn(projects);
            when(taskRepo.getTaskProgressByProjectIds(anyCollection())).thenReturn(Collections.emptyMap());
            when(milestoneRepo.findNextMilestonesByProjectIds(anyCollection())).thenReturn(Collections.emptyMap());
            when(milestoneRepo.findImpendingMilestones(any())).thenReturn(Collections.emptyList());

            HomeDashboardService dashboardService = new HomeDashboardService(
                    mock(EventRepository.class), taskRepo, projectRepo, milestoneRepo,
                    mock(com.monday.app.reminder.repository.ReminderRepository.class),
                    mock(CreditAccountRepository.class), mock(InvestmentRepository.class),
                    mock(BudgetRepository.class), mock(BudgetCategoryRepository.class),
                    mock(TransactionRepository.class), jdbc
            );

            // Execute
            HomeDashboardResponse response = dashboardService.getDashboard(LocalDate.now(), ZoneId.of("UTC"));

            assertNotNull(response);
            assertEquals(projectCount, response.projects().activeProjectCount());

            // Assert: Exactly ONE batch call for progress, ONE batch call for milestones, regardless of project count!
            verify(taskRepo, times(1)).getTaskProgressByProjectIds(argThat(c -> c.size() == projectCount));
            verify(milestoneRepo, times(1)).findNextMilestonesByProjectIds(argThat(c -> c.size() == projectCount));
            // Assert zero per-project calls
            verify(taskRepo, never()).findByProjectId(any(UUID.class));
            verify(milestoneRepo, never()).findNextByProjectId(any(UUID.class));
        }
    }

    // =========================================================================
    // 2. Dashboard: Vehicles scaling (1 vs 5)
    // =========================================================================
    @Test
    @DisplayName("Dashboard calculateVehicles repository calls remain constant for 1 vs 5 vehicles")
    void dashboard_vehiclesScaling_repositoryCallsRemainConstant() {
        for (int vehicleCount : List.of(1, 5)) {
            VehicleRepository vehicleRepo = mock(VehicleRepository.class);
            VehicleMaintenanceScheduleRepository schedRepo = mock(VehicleMaintenanceScheduleRepository.class);
            VehicleDocumentRepository docRepo = mock(VehicleDocumentRepository.class);
            VehicleTaskRepository taskRepo = mock(VehicleTaskRepository.class);
            VehicleObligationRepository oblRepo = mock(VehicleObligationRepository.class);

            List<Vehicle> vehicles = new ArrayList<>();
            for (int i = 0; i < vehicleCount; i++) {
                Vehicle v = new Vehicle();
                v.setId(UUID.randomUUID());
                v.setName("Vehicle " + i);
                v.setCurrentKm(50000);
                vehicles.add(v);
            }

            when(vehicleRepo.findAll()).thenReturn(vehicles);
            when(schedRepo.findByVehicleIds(anyCollection())).thenReturn(Collections.emptyList());
            when(docRepo.findByVehicleIds(anyCollection())).thenReturn(Collections.emptyList());
            when(taskRepo.findByVehicleIds(anyCollection())).thenReturn(Collections.emptyList());
            when(oblRepo.findByVehicleIds(anyCollection())).thenReturn(Collections.emptyList());

            HomeDashboardService dashboardService = new HomeDashboardService(
                    mock(EventRepository.class), mock(TaskRepository.class), mock(ProjectRepository.class),
                    mock(MilestoneRepository.class), mock(com.monday.app.reminder.repository.ReminderRepository.class),
                    mock(CreditAccountRepository.class), mock(InvestmentRepository.class),
                    mock(BudgetRepository.class), mock(BudgetCategoryRepository.class),
                    mock(TransactionRepository.class), mock(NamedParameterJdbcTemplate.class),
                    vehicleRepo, schedRepo, docRepo, taskRepo, oblRepo,
                    null, null, null, null, null, null, new ObjectMapper()
            );

            HomeDashboardResponse response = dashboardService.getDashboard(LocalDate.now(), ZoneId.of("UTC"));
            assertNotNull(response);
            assertEquals(vehicleCount, response.vehicles().totalVehicles());

            // Assert: Exactly ONE batch call per repository, regardless of vehicle count
            verify(schedRepo, times(1)).findByVehicleIds(argThat(c -> c.size() == vehicleCount));
            verify(docRepo, times(1)).findByVehicleIds(argThat(c -> c.size() == vehicleCount));
            verify(taskRepo, times(1)).findByVehicleIds(argThat(c -> c.size() == vehicleCount));
            verify(oblRepo, times(1)).findByVehicleIds(argThat(c -> c.size() == vehicleCount));

            // Assert: ZERO individual per-vehicle queries
            verify(schedRepo, never()).findByVehicleId(any(UUID.class));
            verify(docRepo, never()).findByVehicleId(any(UUID.class));
            verify(taskRepo, never()).findByVehicleId(any(UUID.class));
            verify(oblRepo, never()).findByVehicleId(any(UUID.class));
        }
    }

    // =========================================================================
    // 3. Intelligence Feed: Article summaries scaling (5 vs 25 articles)
    // =========================================================================
    @Test
    @DisplayName("Intelligence feed summary query count remains 1 for both 5 and 25 articles")
    void intelligenceFeed_summaryBatchLoading_scalesConstant() {
        for (int articleCount : List.of(5, 25)) {
            ArticleRepository articleRepo = mock(ArticleRepository.class);
            ArticleSummaryRepository summaryRepo = mock(ArticleSummaryRepository.class);
            ArticleSourceRepository sourceRepo = mock(ArticleSourceRepository.class);

            List<Article> articles = new ArrayList<>();
            List<UUID> articleIds = new ArrayList<>();
            for (int i = 0; i < articleCount; i++) {
                Article a = new Article();
                a.setId(UUID.randomUUID());
                a.setTitle("AI Article " + i);
                articles.add(a);
                articleIds.add(a.getId());
            }

            Page<Article> page = new PageImpl<>(articles, PageRequest.of(0, articleCount), articleCount);
            when(articleRepo.searchArticles(any(), any(), any(), any(), any())).thenReturn(page);
            when(summaryRepo.findLatestSummariesByArticleIdsAndStatus(anyCollection(), eq("SUCCESS")))
                    .thenReturn(Collections.emptyList());
            when(sourceRepo.findByIdArticleIdIn(anyList())).thenReturn(Collections.emptyList());

            ArticleFeedService feedService = new ArticleFeedService(articleRepo, summaryRepo, sourceRepo);
            Page<ArticleFeedItemDto> result = feedService.getArticles(null, null, null, null, 0, articleCount);

            assertNotNull(result);
            assertEquals(articleCount, result.getContent().size());

            // Assert: Exactly ONE batch summary query executed, regardless of page size!
            verify(summaryRepo, times(1)).findLatestSummariesByArticleIdsAndStatus(argThat(c -> c.size() == articleCount), eq("SUCCESS"));
            verify(summaryRepo, never()).findFirstByArticleIdAndStatusOrderByCreatedAtDesc(any(UUID.class), anyString());
        }
    }

    // =========================================================================
    // 4. Administration Appointments: Checklist batch loading (1 vs 20 appointments)
    // =========================================================================
    @Test
    @DisplayName("Administration appointments checklist loading remains 1 batch query for 1 vs 20 appointments")
    void administrationAppointments_checklistLoading_scalesConstant() {
        for (int apptCount : List.of(1, 20)) {
            EventRepository eventRepo = mock(EventRepository.class);
            AdministrationChecklistRepository checklistRepo = mock(AdministrationChecklistRepository.class);
            AdministrationChecklistService checklistService = new AdministrationChecklistService(checklistRepo);

            List<Event> events = new ArrayList<>();
            for (int i = 0; i < apptCount; i++) {
                Event e = new Event();
                e.setId(UUID.randomUUID());
                e.setTitle("Appointment " + i);
                e.setStartAt(Instant.now().plusSeconds(3600 * (i + 1)));
                events.add(e);
            }

            when(eventRepo.findAll()).thenReturn(events);
            when(checklistRepo.findChecklistsByTargets(eq("APPOINTMENT"), anyCollection()))
                    .thenReturn(Collections.emptyList());

            AdministrationAppointmentService apptService = new AdministrationAppointmentService(eventRepo, checklistService);
            List<AppointmentDto> results = apptService.getAppointments(null, null, null, null, null);

            assertNotNull(results);
            assertEquals(apptCount, results.size());

            // Assert: Exactly ONE batch call to checklist repo for all appointments
            verify(checklistRepo, times(1)).findChecklistsByTargets(eq("APPOINTMENT"), argThat(c -> c.size() == apptCount));
            verify(checklistRepo, never()).findChecklistsByTarget(anyString(), any(UUID.class));
        }
    }

    // =========================================================================
    // 5. Bulk Transaction Import: Scaling (1 vs 50 transactions)
    // =========================================================================
    @Test
    @DisplayName("Bulk transaction import database operations remain constant for 1 vs 50 transactions")
    void bulkTransactionImport_scaling_remainsConstant() {
        for (int txCount : List.of(1, 50)) {
            TransactionRepository txRepo = mock(TransactionRepository.class);
            BudgetRepository budgetRepo = mock(BudgetRepository.class);
            BudgetCategoryRepository catRepo = mock(BudgetCategoryRepository.class);
            FinanceAccountRepository accountRepo = mock(FinanceAccountRepository.class);
            CreditAccountRepository creditRepo = mock(CreditAccountRepository.class);
            NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);

            UUID accountId = UUID.randomUUID();
            FinanceAccount account = new FinanceAccount();
            account.setId(accountId);
            when(accountRepo.findAll()).thenReturn(List.of(account));
            when(catRepo.findAll()).thenReturn(Collections.emptyList());
            when(creditRepo.findAll()).thenReturn(Collections.emptyList());
            when(txRepo.batchSave(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<Transaction> txList = new ArrayList<>();
            for (int i = 0; i < txCount; i++) {
                Transaction t = new Transaction();
                t.setAmount(new BigDecimal("100.00"));
                t.setTransactionType("EXPENSE");
                t.setDescription("Store " + i);
                t.setAccountId(accountId);
                txList.add(t);
            }

            FinanceService financeService = new FinanceService(
                    txRepo, budgetRepo, catRepo, accountRepo, creditRepo,
                    mock(InvestmentRepository.class), mock(EventRepository.class), jdbc, null
            );

            List<Transaction> created = financeService.createTransactionsBulk(txList);
            assertEquals(txCount, created.size());

            // Assert: Exactly ONE batchSave call regardless of 1 or 50 transactions!
            verify(txRepo, times(1)).batchSave(argThat(l -> l.size() == txCount));
            // Assert: Exactly ONE grouped balance adjustment for the account
            verify(accountRepo, times(1)).adjustBalance(eq(accountId), any(BigDecimal.class));
            // Assert: Category and account preloaded exactly ONCE
            verify(catRepo, times(1)).findAll();
            verify(accountRepo, times(1)).findAll();
        }
    }
}
