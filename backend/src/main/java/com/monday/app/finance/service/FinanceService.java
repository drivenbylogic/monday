package com.monday.app.finance.service;

import com.monday.app.finance.dto.SafeToSpend;
import com.monday.app.finance.entity.Budget;
import com.monday.app.finance.entity.BudgetCategory;
import com.monday.app.finance.entity.Transaction;
import com.monday.app.finance.repository.BudgetCategoryRepository;
import com.monday.app.finance.repository.BudgetRepository;
import com.monday.app.finance.repository.TransactionRepository;
import com.monday.app.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FinanceService {

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetCategoryRepository categoryRepository;
    private final org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate jdbc;

    public FinanceService(TransactionRepository transactionRepository,
                          BudgetRepository budgetRepository,
                          BudgetCategoryRepository categoryRepository,
                          org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate jdbc) {
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.jdbc = jdbc;
    }

    // ─── Budget Categories ──────────────────────────────────────────

    @Transactional
    public BudgetCategory createCategory(BudgetCategory category) {
        if (category.getStatus() == null) category.setStatus("ACTIVE");
        return categoryRepository.save(category);
    }

    @Transactional
    public BudgetCategory updateCategory(UUID id, BudgetCategory updated) {
        BudgetCategory existing = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BudgetCategory", id));
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setStatus(updated.getStatus());
        return categoryRepository.update(existing);
    }

    public BudgetCategory getCategoryById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BudgetCategory", id));
    }

    public List<BudgetCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Transactional
    public void deleteCategory(UUID id) {
        categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BudgetCategory", id));
        categoryRepository.deleteById(id);
    }

    // ─── Budgets ────────────────────────────────────────────────────

    @Transactional
    public Budget createBudget(Budget budget) {
        categoryRepository.findById(budget.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("BudgetCategory", budget.getCategoryId()));
        return budgetRepository.save(budget);
    }

    @Transactional
    public Budget updateBudget(UUID id, Budget updated) {
        Budget existing = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", id));
        existing.setName(updated.getName());
        existing.setCategoryId(updated.getCategoryId());
        existing.setPeriodStart(updated.getPeriodStart());
        existing.setPeriodEnd(updated.getPeriodEnd());
        existing.setAmount(updated.getAmount());
        return budgetRepository.update(existing);
    }

    public Budget getBudgetById(UUID id) {
        return budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", id));
    }

    public List<Budget> getAllBudgets() {
        return budgetRepository.findAll();
    }

    @Transactional
    public void deleteBudget(UUID id) {
        budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", id));
        budgetRepository.deleteById(id);
    }

    // ─── Transactions ───────────────────────────────────────────────

    @Transactional
    public Transaction createTransaction(Transaction txn) {
        if (txn.getCategoryId() != null) {
            categoryRepository.findById(txn.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("BudgetCategory", txn.getCategoryId()));
        }
        return transactionRepository.save(txn);
    }

    @Transactional
    public Transaction updateTransaction(UUID id, Transaction updated) {
        Transaction existing = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
        existing.setTransactionType(updated.getTransactionType());
        existing.setAmount(updated.getAmount());
        existing.setTransactionAt(updated.getTransactionAt());
        existing.setCategoryId(updated.getCategoryId());
        existing.setPaymentMethod(updated.getPaymentMethod());
        existing.setDescription(updated.getDescription());
        existing.setNotes(updated.getNotes());
        return transactionRepository.update(existing);
    }

    public Transaction getTransactionById(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public List<Transaction> getTransactionsByDateRange(Instant from, Instant to) {
        return transactionRepository.findByDateRange(from, to);
    }

    @Transactional
    public void deleteTransaction(UUID id) {
        transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
        transactionRepository.deleteById(id);
    }

    // ─── Safe-to-Spend (derived, never persisted) ───────────────────

    /**
     * Calculates safe-to-spend for all active budgets covering today's date.
     *
     * <pre>
     * remaining_category_budget = category_budget - current_period_category_expenses
     * daily_safe_to_spend       = remaining_budget / remaining_days_in_budget_period
     * </pre>
     */
    public List<SafeToSpend> calculateSafeToSpend() {
        LocalDate today = LocalDate.now();
        List<Budget> activeBudgets = budgetRepository.findActiveForDate(today);
        List<SafeToSpend> results = new ArrayList<>();

        for (Budget budget : activeBudgets) {
            BudgetCategory category = categoryRepository.findById(budget.getCategoryId()).orElse(null);
            if (category == null) continue;

            BigDecimal spent = transactionRepository.sumExpensesByCategoryAndPeriod(
                    budget.getCategoryId(), budget.getPeriodStart(), budget.getPeriodEnd());

            BigDecimal remaining = budget.getAmount().subtract(spent);
            long remainingDays = Math.max(1, ChronoUnit.DAYS.between(today, budget.getPeriodEnd()) + 1);
            BigDecimal dailySafe = remaining.divide(BigDecimal.valueOf(remainingDays), 2, RoundingMode.HALF_UP);

            results.add(new SafeToSpend(
                    budget.getId(),
                    category.getId(),
                    category.getName(),
                    budget.getAmount(),
                    spent,
                    remaining,
                    dailySafe,
                    (int) remainingDays,
                    budget.getPeriodStart(),
                    budget.getPeriodEnd()
            ));
        }

        return results;
    }

    // --- Analytical Endpoints ---
    public java.util.Map<String, Object> getPulse(String cycle) {
        if (cycle == null) cycle = java.time.YearMonth.now().toString();
        
        String sql = """
            SELECT 
                (SELECT COALESCE(SUM(balance), 0) FROM finance_accounts) as "totalLiquidReserves",
                (SELECT COALESCE(SUM(amount), 0) FROM budgets WHERE to_char(period_start, 'YYYY-MM') <= :cycle AND to_char(period_end, 'YYYY-MM') >= :cycle) as "monthlyCap",
                (SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE transaction_type = 'expense' AND to_char(transaction_at, 'YYYY-MM') = :cycle) as "monthlySpent"
        """;
        java.util.Map<String, Object> data = jdbc.queryForMap(sql, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("cycle", cycle));
        
        BigDecimal cap = (BigDecimal) data.get("monthlyCap");
        BigDecimal spent = (BigDecimal) data.get("monthlySpent");
        BigDecimal reserves = (BigDecimal) data.get("totalLiquidReserves");
        
        double burnPercentage = cap.compareTo(BigDecimal.ZERO) > 0 ? spent.doubleValue() / cap.doubleValue() * 100 : 0.0;
        double runwayMonths = spent.compareTo(BigDecimal.ZERO) > 0 ? reserves.doubleValue() / spent.doubleValue() : 0.0;
        
        List<SafeToSpend> safeList = calculateSafeToSpend();
        BigDecimal safeSpendToday = safeList.stream().map(SafeToSpend::dailySafeToSpend).reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return java.util.Map.of(
                "cycle", cycle,
                "safeSpendToday", safeSpendToday,
                "monthlySpent", spent,
                "monthlyCap", cap,
                "burnPercentage", burnPercentage,
                "totalLiquidReserves", reserves,
                "runwayMonths", runwayMonths,
                "burnStatus", burnPercentage > 90 ? "Critical" : (burnPercentage > 75 ? "Warning" : "Optimal")
        );
    }

    public List<java.util.Map<String, Object>> getBurnTrajectory(int days, String cycle) {
        String sql = """
            SELECT to_char(transaction_at, 'DD') as day, to_char(transaction_at, 'YYYY-MM-DD') as date, 
                   SUM(amount) as spend, 
                   (SUM(amount) / 100) as "heightPercent",
                   string_agg(description, ', ') as note,
                   false as "isWarning"
            FROM transactions
            WHERE transaction_type = 'expense' AND transaction_at >= CURRENT_DATE - (:days || ' days')::interval
            GROUP BY date, day
            ORDER BY date
        """;
        return jdbc.queryForList(sql, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("days", days));
    }

    public List<java.util.Map<String, Object>> getAccounts() {
        return jdbc.queryForList("SELECT id as \"accountId\", institution, account_type as \"accountType\", balance, currency, to_char(last_reconciled, 'YYYY-MM-DD HH24:MI:SS') as \"lastReconciled\" FROM finance_accounts", new org.springframework.jdbc.core.namedparam.MapSqlParameterSource());
    }

    public List<java.util.Map<String, Object>> getImpendingCommitments(int windowDays) {
        String sql = """
            SELECT id, name, DATE_PART('day', due_date::timestamp - CURRENT_DATE::timestamp) as "dueDays",
                   to_char(due_date, 'Mon DD, YYYY') as "dueDate",
                   amount, account_id as account, is_urgent as "isUrgent"
            FROM finance_commitments
            WHERE due_date >= CURRENT_DATE AND due_date <= CURRENT_DATE + (:windowDays || ' days')::interval
              AND status = 'PENDING'
            ORDER BY due_date
        """;
        return jdbc.queryForList(sql, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("windowDays", windowDays));
    }

    @Transactional
    public java.util.Map<String, Object> settleCommitment(java.util.Map<String, Object> body) {
        String obligationId = (String) body.get("obligationId");
        String accountId = (String) body.get("sourceAccount");
        Number amount = (Number) body.get("amount");
        
        jdbc.update("UPDATE finance_commitments SET status = 'SETTLED' WHERE id = :id::uuid", 
            new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("id", obligationId));
            
        String txId = UUID.randomUUID().toString();
        String txSql = """
            INSERT INTO transactions (id, transaction_type, amount, transaction_at, description, notes)
            VALUES (:id::uuid, 'expense', :amount, CURRENT_TIMESTAMP, :desc, 'Settled obligation')
        """;
        jdbc.update(txSql, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
            .addValue("id", txId)
            .addValue("amount", amount.doubleValue())
            .addValue("desc", body.get("merchant")));
            
        if (accountId != null && accountId.length() == 36) {
            jdbc.update("UPDATE finance_accounts SET balance = balance - :amount WHERE id = :id::uuid",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("amount", amount.doubleValue()).addValue("id", accountId));
        }
        
        BigDecimal updatedReserves = jdbc.queryForObject("SELECT COALESCE(SUM(balance), 0) FROM finance_accounts", new org.springframework.jdbc.core.namedparam.MapSqlParameterSource(), BigDecimal.class);

        return java.util.Map.of(
                "status", "SETTLED",
                "transactionId", txId,
                "settlementTimestamp", java.time.Instant.now().toString(),
                "updatedReserves", updatedReserves,
                "downstreamSync", java.util.Map.of(
                        "vehiclesPolicyActive", true,
                        "administrationObligationSettled", true,
                        "homePriorityResolved", true
                )
        );
    }
}
