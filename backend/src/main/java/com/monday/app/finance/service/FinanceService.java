package com.monday.app.finance.service;

import com.monday.app.event.entity.Event;
import com.monday.app.event.repository.EventRepository;
import com.monday.app.finance.dto.EmiConversionRequest;
import com.monday.app.finance.dto.EmiConversionResult;
import com.monday.app.finance.dto.SafeToSpend;
import com.monday.app.finance.entity.*;
import com.monday.app.finance.repository.*;
import com.monday.app.shared.exception.ResourceNotFoundException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class FinanceService {

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetCategoryRepository categoryRepository;
    private final FinanceAccountRepository accountRepository;
    private final CreditAccountRepository creditAccountRepository;
    private final InvestmentRepository investmentRepository;
    private final EventRepository eventRepository;
    private final org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate jdbc;

    public FinanceService(TransactionRepository transactionRepository,
                          BudgetRepository budgetRepository,
                          BudgetCategoryRepository categoryRepository,
                          FinanceAccountRepository accountRepository,
                          CreditAccountRepository creditAccountRepository,
                          InvestmentRepository investmentRepository,
                          EventRepository eventRepository,
                          org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate jdbc) {
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.accountRepository = accountRepository;
        this.creditAccountRepository = creditAccountRepository;
        this.investmentRepository = investmentRepository;
        this.eventRepository = eventRepository;
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
        if (updated.getName() != null && !updated.getName().isBlank()) {
            existing.setName(updated.getName().trim());
        }
        if (updated.getDescription() != null) {
            existing.setDescription(updated.getDescription());
        }
        if (updated.getStatus() != null && !updated.getStatus().isBlank()) {
            existing.setStatus(updated.getStatus());
        }
        return categoryRepository.update(existing);
    }

    public BudgetCategory getCategoryById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BudgetCategory", id));
    }

    public List<BudgetCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    public UUID resolveCategoryId(String idStr) {
        if (idStr == null || idStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Category ID cannot be blank");
        }
        String clean = idStr.trim();
        if (clean.length() == 36) {
            try {
                return UUID.fromString(clean);
            } catch (IllegalArgumentException ignored) {}
        }
        String pattern = clean.replace("-", "") + "%";
        String sql = "SELECT id FROM budget_categories WHERE replace(id::text, '-', '') ILIKE :pat LIMIT 1";
        List<UUID> matches = jdbc.query(sql, new MapSqlParameterSource("pat", pattern), (rs, i) -> (UUID) rs.getObject("id"));
        if (!matches.isEmpty()) {
            return matches.get(0);
        }
        try {
            return UUID.fromString(clean);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("BudgetCategory", clean);
        }
    }

    @Transactional
    public BudgetCategory updateCategory(String idStr, BudgetCategory updated) {
        return updateCategory(resolveCategoryId(idStr), updated);
    }

    @Transactional
    public void deleteCategory(UUID id) {
        categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BudgetCategory", id));
        // Cascade delete child budgets
        jdbc.update("DELETE FROM budgets WHERE category_id = :id", new MapSqlParameterSource("id", id));
        // Nullify transaction category references
        jdbc.update("UPDATE transactions SET category_id = NULL WHERE category_id = :id", new MapSqlParameterSource("id", id));
        categoryRepository.deleteById(id);
    }

    @Transactional
    public void deleteCategory(String idStr) {
        deleteCategory(resolveCategoryId(idStr));
    }

    // ─── Budgets ────────────────────────────────────────────────────

    @Transactional
    public Budget createBudget(Budget budget) {
        BudgetCategory category = categoryRepository.findById(budget.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("BudgetCategory", budget.getCategoryId()));
        if (budget.getName() == null || budget.getName().trim().isEmpty()) {
            budget.setName(category.getName() + " Budget Envelope");
        }

        if (budget.getPeriodType() == null || budget.getPeriodType().trim().isEmpty()) {
            budget.setPeriodType("MONTHLY");
        } else {
            budget.setPeriodType(budget.getPeriodType().trim().toUpperCase());
        }

        if ("MONTHLY".equalsIgnoreCase(budget.getPeriodType())) {
            LocalDate start = budget.getPeriodStart() != null ? budget.getPeriodStart() : LocalDate.now().withDayOfMonth(1);
            budget.setPeriodStart(start.withDayOfMonth(1));
            budget.setPeriodEnd(start.withDayOfMonth(start.lengthOfMonth()));
        } else {
            if (budget.getPeriodStart() == null) {
                budget.setPeriodStart(LocalDate.now().withDayOfMonth(1));
            }
            if (budget.getPeriodEnd() == null) {
                budget.setPeriodEnd(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()));
            }
            if (budget.getPeriodEnd().isBefore(budget.getPeriodStart())) {
                budget.setPeriodEnd(budget.getPeriodStart().plusMonths(1).minusDays(1));
            }
        }

        if (budget.getAmount() == null) {
            budget.setAmount(BigDecimal.ZERO);
        }
        return budgetRepository.save(budget);
    }

    @Transactional
    public Budget updateBudget(UUID id, Budget updated) {
        Budget existing = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", id));
        if (updated.getName() != null && !updated.getName().trim().isEmpty()) {
            existing.setName(updated.getName().trim());
        }
        if (updated.getCategoryId() != null) {
            existing.setCategoryId(updated.getCategoryId());
        }
        if (updated.getPeriodType() != null && !updated.getPeriodType().trim().isEmpty()) {
            existing.setPeriodType(updated.getPeriodType().trim().toUpperCase());
        }
        if (updated.getPeriodStart() != null) {
            existing.setPeriodStart(updated.getPeriodStart());
        }
        if (updated.getPeriodEnd() != null) {
            existing.setPeriodEnd(updated.getPeriodEnd());
        }
        if ("MONTHLY".equalsIgnoreCase(existing.getPeriodType()) && existing.getPeriodStart() != null) {
            LocalDate start = existing.getPeriodStart();
            existing.setPeriodStart(start.withDayOfMonth(1));
            existing.setPeriodEnd(start.withDayOfMonth(start.lengthOfMonth()));
        }
        if (updated.getAmount() != null) {
            existing.setAmount(updated.getAmount());
        }
        return budgetRepository.update(existing);
    }

    public Budget getBudgetById(UUID id) {
        return budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", id));
    }

    public List<Budget> getAllBudgets() {
        return budgetRepository.findAll();
    }

    public UUID resolveBudgetId(String idStr) {
        if (idStr == null || idStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Budget ID cannot be blank");
        }
        String clean = idStr.trim();
        if (clean.length() == 36) {
            try {
                return UUID.fromString(clean);
            } catch (IllegalArgumentException ignored) {}
        }
        String pattern = clean.replace("-", "") + "%";
        String sql = "SELECT id FROM budgets WHERE replace(id::text, '-', '') ILIKE :pat LIMIT 1";
        List<UUID> matches = jdbc.query(sql, new MapSqlParameterSource("pat", pattern), (rs, i) -> (UUID) rs.getObject("id"));
        if (!matches.isEmpty()) {
            return matches.get(0);
        }
        try {
            return UUID.fromString(clean);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Budget", clean);
        }
    }

    @Transactional
    public Budget updateBudget(String idStr, Budget updated) {
        return updateBudget(resolveBudgetId(idStr), updated);
    }

    @Transactional
    public void deleteBudget(UUID id) {
        budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", id));
        budgetRepository.deleteById(id);
    }

    @Transactional
    public void deleteBudget(String idStr) {
        deleteBudget(resolveBudgetId(idStr));
    }

    private static final Set<String> VALID_LINKED_MODULES = Set.of(
            "PROJECTS", "VEHICLES", "ADMINISTRATION", "EVENTS", "GENERAL", "FINANCE"
    );

    // ─── Transactions ───────────────────────────────────────────────

    @Transactional
    public Transaction createTransaction(Transaction txn) {
        if (txn.getCategoryId() == null) {
            List<BudgetCategory> allCats = categoryRepository.findAll();
            for (BudgetCategory cat : allCats) {
                if ((txn.getDescription() != null && txn.getDescription().toLowerCase().contains(cat.getName().toLowerCase())) ||
                    (txn.getNotes() != null && txn.getNotes().toLowerCase().contains(cat.getName().toLowerCase()))) {
                    txn.setCategoryId(cat.getId());
                    break;
                }
            }
        }
        if (txn.getCategoryId() != null) {
            Optional<BudgetCategory> catOpt = categoryRepository.findById(txn.getCategoryId());
            if (catOpt.isEmpty()) {
                txn.setCategoryId(null);
            }
        }
        if (txn.getLinkedModule() != null && !VALID_LINKED_MODULES.contains(txn.getLinkedModule().toUpperCase())) {
            throw new IllegalArgumentException("Invalid linkedModule: " + txn.getLinkedModule() + ". Allowed: " + VALID_LINKED_MODULES);
        }
        if (txn.getLinkedModule() != null) {
            txn.setLinkedModule(txn.getLinkedModule().toUpperCase());
        }

        // Adjust account balance if account_id is provided and corresponds to a finance account
        UUID accountId = txn.getAccountId();
        if (accountId != null) {
            Optional<FinanceAccount> fa = accountRepository.findById(accountId);
            if (fa.isPresent()) {
                BigDecimal delta = txn.getAmount() != null ? txn.getAmount() : BigDecimal.ZERO;
                if ("EXPENSE".equalsIgnoreCase(txn.getTransactionType())) {
                    delta = delta.negate();
                }
                accountRepository.adjustBalance(accountId, delta);
            } else {
                txn.setAccountId(null);
            }
        }

        if (txn.getCreditCardId() != null) {
            Optional<CreditAccount> cardOpt = creditAccountRepository.findById(txn.getCreditCardId());
            if (cardOpt.isEmpty()) {
                txn.setCreditCardId(null);
            }
        }

        // Automatic credit card commitment & event handling
        processCreditCardTransactionOnCreate(txn);

        return transactionRepository.save(txn);
    }

    @Transactional
    public List<Transaction> createTransactionsBulk(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return Collections.emptyList();
        }
        List<Transaction> created = new ArrayList<>();
        for (Transaction txn : transactions) {
            created.add(createTransaction(txn));
        }
        return created;
    }

    @Transactional
    public EmiConversionResult convertToEmi(UUID transactionId, EmiConversionRequest request) {
        Transaction original = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId));

        if (!"EXPENSE".equalsIgnoreCase(original.getTransactionType())) {
            throw new IllegalArgumentException("Only expense transactions can be converted to EMI");
        }

        CreditAccount card = findAssociatedCreditCard(original);
        if (card == null) {
            throw new IllegalArgumentException("Transaction was not made on a credit card and cannot be converted to EMI");
        }

        if ("EMI_INSTALLMENT".equalsIgnoreCase(original.getLinkedEntityType())
                || "EMI_PARENT".equalsIgnoreCase(original.getLinkedEntityType())
                || "EMI_FEE".equalsIgnoreCase(original.getLinkedEntityType())) {
            throw new IllegalStateException("Transaction has already been converted to EMI or is an EMI installment");
        }

        int tenure = (request != null && request.tenureMonths() != null) ? request.tenureMonths() : 6;
        if (tenure < 2 || tenure > 60) {
            throw new IllegalArgumentException("EMI tenure must be between 2 and 60 months");
        }

        BigDecimal principal = original.getAmount();
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction amount must be greater than zero for EMI conversion");
        }

        BigDecimal rate = (request != null && request.annualInterestRate() != null && request.annualInterestRate().compareTo(BigDecimal.ZERO) > 0)
                ? request.annualInterestRate()
                : BigDecimal.ZERO;
        BigDecimal fee = (request != null && request.processingFee() != null && request.processingFee().compareTo(BigDecimal.ZERO) > 0)
                ? request.processingFee()
                : BigDecimal.ZERO;

        BigDecimal monthlyInstallment;
        BigDecimal totalPayable;
        BigDecimal totalInterest;
        List<BigDecimal> installmentAmounts = new ArrayList<>();

        if (rate.compareTo(BigDecimal.ZERO) == 0) {
            // 0% No Cost EMI
            BigDecimal baseMonthly = principal.divide(BigDecimal.valueOf(tenure), 2, RoundingMode.FLOOR);
            BigDecimal remainder = principal.subtract(baseMonthly.multiply(BigDecimal.valueOf(tenure)));
            for (int i = 1; i <= tenure; i++) {
                if (i == tenure) {
                    installmentAmounts.add(baseMonthly.add(remainder));
                } else {
                    installmentAmounts.add(baseMonthly);
                }
            }
            monthlyInstallment = baseMonthly;
            totalPayable = principal;
            totalInterest = BigDecimal.ZERO;
        } else {
            // Standard Reducing Balance EMI: E = P * r * (1 + r)^n / ((1 + r)^n - 1)
            double monthlyRate = (rate.doubleValue() / 100.0) / 12.0;
            double pDouble = principal.doubleValue();
            double emiDouble = (pDouble * monthlyRate * Math.pow(1 + monthlyRate, tenure)) / (Math.pow(1 + monthlyRate, tenure) - 1);
            monthlyInstallment = BigDecimal.valueOf(emiDouble).setScale(2, RoundingMode.HALF_UP);
            totalPayable = monthlyInstallment.multiply(BigDecimal.valueOf(tenure));
            totalInterest = totalPayable.subtract(principal);

            for (int i = 1; i <= tenure; i++) {
                installmentAmounts.add(monthlyInstallment);
            }
        }

        LocalDate baseDate = (request != null && request.firstInstallmentDate() != null)
                ? request.firstInstallmentDate()
                : (original.getTransactionAt() != null
                    ? original.getTransactionAt().atZone(ZoneId.systemDefault()).toLocalDate()
                    : LocalDate.now());

        UUID emiGroupId = UUID.randomUUID();
        String originalDescription = original.getDescription() != null ? original.getDescription() : "Credit Card Purchase";

        // Update the original transaction to become Installment 1 of N
        original.setAmount(installmentAmounts.get(0));
        original.setDescription(originalDescription + " (EMI 1/" + tenure + ")");
        original.setNotes((original.getNotes() != null && !original.getNotes().isBlank() ? original.getNotes() + " | " : "")
                + "EMI 1 of " + tenure + " · Converted from ₹" + principal.toPlainString() + " (" + card.getCardName() + " •• " + card.getLastFour() + ")");
        original.setCreditCardId(card.getId());
        original.setLinkedModule("FINANCE");
        original.setLinkedEntityType("EMI_INSTALLMENT");
        original.setLinkedEntityId(emiGroupId.toString());

        Transaction updatedFirst = updateTransaction(original.getId(), original);

        List<Transaction> createdInstallments = new ArrayList<>();
        createdInstallments.add(updatedFirst);

        // Create installments 2 .. N
        for (int i = 2; i <= tenure; i++) {
            LocalDate instDate = baseDate.plusMonths(i - 1);
            Instant instInstant = instDate.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant();

            Transaction inst = new Transaction();
            inst.setTransactionType("EXPENSE");
            inst.setAmount(installmentAmounts.get(i - 1));
            inst.setTransactionAt(instInstant);
            inst.setCategoryId(original.getCategoryId());
            inst.setCreditCardId(card.getId());
            inst.setPaymentMethod(original.getPaymentMethod());
            inst.setDescription(originalDescription + " (EMI " + i + "/" + tenure + ")");
            inst.setNotes("EMI installment " + i + " of " + tenure + " · Plan: " + emiGroupId);
            inst.setLinkedModule("FINANCE");
            inst.setLinkedEntityType("EMI_INSTALLMENT");
            inst.setLinkedEntityId(emiGroupId.toString());

            createdInstallments.add(createTransaction(inst));
        }

        // If processing fee applies, log fee transaction
        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            Transaction feeTx = new Transaction();
            feeTx.setTransactionType("EXPENSE");
            feeTx.setAmount(fee);
            feeTx.setTransactionAt(updatedFirst.getTransactionAt());
            feeTx.setCategoryId(original.getCategoryId());
            feeTx.setCreditCardId(card.getId());
            feeTx.setPaymentMethod(original.getPaymentMethod());
            feeTx.setDescription("EMI Processing Fee - " + originalDescription);
            feeTx.setNotes("One-time processing fee for " + tenure + "-month EMI conversion · Plan: " + emiGroupId);
            feeTx.setLinkedModule("FINANCE");
            feeTx.setLinkedEntityType("EMI_FEE");
            feeTx.setLinkedEntityId(emiGroupId.toString());

            createdInstallments.add(createTransaction(feeTx));
        }

        return new EmiConversionResult(
                emiGroupId,
                original.getId(),
                tenure,
                principal,
                monthlyInstallment,
                totalPayable,
                totalInterest,
                fee,
                createdInstallments
        );
    }

    @Transactional
    public Transaction updateTransaction(UUID id, Transaction updated) {
        Transaction existing = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
        if (updated.getLinkedModule() != null && !VALID_LINKED_MODULES.contains(updated.getLinkedModule().toUpperCase())) {
            throw new IllegalArgumentException("Invalid linkedModule: " + updated.getLinkedModule() + ". Allowed: " + VALID_LINKED_MODULES);
        }

        // Revert old account balance adjustment
        if (existing.getAccountId() != null && existing.getAmount() != null) {
            BigDecimal oldDelta = existing.getAmount();
            if (!"EXPENSE".equalsIgnoreCase(existing.getTransactionType())) {
                oldDelta = oldDelta.negate();
            }
            try {
                accountRepository.adjustBalance(existing.getAccountId(), oldDelta);
            } catch (Exception ignored) {}
        }

        // Revert old credit card transaction impact
        processCreditCardTransactionOnDelete(existing);

        existing.setTransactionType(updated.getTransactionType());
        existing.setAmount(updated.getAmount());
        existing.setTransactionAt(updated.getTransactionAt());
        existing.setCategoryId(updated.getCategoryId());
        existing.setPaymentMethod(updated.getPaymentMethod());
        existing.setDescription(updated.getDescription());
        existing.setNotes(updated.getNotes());
        if (updated.getLinkedModule() != null) {
            existing.setLinkedModule(updated.getLinkedModule().toUpperCase());
        }
        if (updated.getLinkedEntityId() != null) {
            existing.setLinkedEntityId(updated.getLinkedEntityId());
        }
        if (updated.getLinkedEntityType() != null) {
            existing.setLinkedEntityType(updated.getLinkedEntityType());
        }
        existing.setAccountId(updated.getAccountId());
        existing.setCreditCardId(updated.getCreditCardId());

        // Apply new account balance adjustment
        UUID targetAccountId = updated.getAccountId() != null ? updated.getAccountId() : existing.getAccountId();
        if (targetAccountId != null && existing.getAmount() != null) {
            BigDecimal newDelta = existing.getAmount();
            if ("EXPENSE".equalsIgnoreCase(existing.getTransactionType())) {
                newDelta = newDelta.negate();
            }
            try {
                accountRepository.adjustBalance(targetAccountId, newDelta);
            } catch (Exception ignored) {}
        }

        // Apply new credit card transaction impact
        processCreditCardTransactionOnCreate(existing);

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
        Transaction existing = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
        if (existing.getAccountId() != null && existing.getAmount() != null) {
            BigDecimal delta = existing.getAmount();
            if ("INCOME".equalsIgnoreCase(existing.getTransactionType())) {
                delta = delta.negate();
            }
            try {
                accountRepository.adjustBalance(existing.getAccountId(), delta);
            } catch (Exception ignored) {}
        }

        processCreditCardTransactionOnDelete(existing);

        transactionRepository.deleteById(id);
    }

    // ─── Credit Card Impending Commitment & Calendar Automation ──────────

    private CreditAccount findAssociatedCreditCard(Transaction txn) {
        if (txn.getCreditCardId() != null) {
            return creditAccountRepository.findById(txn.getCreditCardId()).orElse(null);
        }
        if (txn.getLinkedEntityId() != null) {
            try {
                UUID entityUuid = UUID.fromString(txn.getLinkedEntityId());
                Optional<CreditAccount> opt = creditAccountRepository.findById(entityUuid);
                if (opt.isPresent()) return opt.get();
            } catch (Exception ignored) {}
            List<CreditAccount> all = creditAccountRepository.findAll();
            for (CreditAccount c : all) {
                if (txn.getLinkedEntityId().equalsIgnoreCase(c.getCardId())) {
                    return c;
                }
            }
        }
        if (txn.getAccountId() != null) {
            Optional<CreditAccount> opt = creditAccountRepository.findById(txn.getAccountId());
            if (opt.isPresent()) return opt.get();
        }
        String method = txn.getPaymentMethod();
        String desc = txn.getDescription();
        List<CreditAccount> all = creditAccountRepository.findAll();
        for (CreditAccount c : all) {
            if (c.getLastFour() != null && !c.getLastFour().isBlank()) {
                if ((method != null && method.contains(c.getLastFour())) ||
                    (desc != null && desc.contains(c.getLastFour()))) {
                    return c;
                }
            }
            if (c.getCardName() != null && !c.getCardName().isBlank()) {
                String cn = c.getCardName().toLowerCase();
                if ((method != null && method.toLowerCase().contains(cn)) ||
                    (desc != null && desc.toLowerCase().contains(cn))) {
                    return c;
                }
            }
        }
        if (method != null && method.toUpperCase().replace(" ", "_").contains("CREDIT_CARD") && !all.isEmpty()) {
            return all.get(0);
        }
        return null;
    }

    private void processCreditCardTransactionOnCreate(Transaction txn) {
        CreditAccount card = findAssociatedCreditCard(txn);
        if (card == null) return;

        txn.setCreditCardId(card.getId());
        if (txn.getLinkedEntityId() == null) {
            txn.setLinkedEntityId(card.getId().toString());
            txn.setLinkedEntityType("CREDIT_CARD");
            txn.setLinkedModule("FINANCE");
        }

        BigDecimal txnAmount = txn.getAmount() != null ? txn.getAmount() : BigDecimal.ZERO;
        boolean isExpense = "EXPENSE".equalsIgnoreCase(txn.getTransactionType());
        BigDecimal delta = isExpense ? txnAmount : txnAmount.negate();

        // Update credit account current balance
        BigDecimal currentBal = card.getCurrentBalance() != null ? card.getCurrentBalance() : BigDecimal.ZERO;
        card.setCurrentBalance(currentBal.add(delta).max(BigDecimal.ZERO));
        creditAccountRepository.update(card);

        LocalDate txnDate = txn.getTransactionAt() != null
                ? txn.getTransactionAt().atZone(ZoneId.systemDefault()).toLocalDate()
                : LocalDate.now();

        // Check for an existing PENDING commitment for this card
        String findSql = """
            SELECT id, name, due_date, amount, event_id FROM finance_commitments
            WHERE (credit_card_id = :cardId OR name = :cardPaymentName) AND status = 'PENDING'
            ORDER BY due_date DESC LIMIT 1
        """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("cardId", card.getId())
                .addValue("cardPaymentName", card.getCardName() + " Payment");
        List<Map<String, Object>> existingList = jdbc.queryForList(findSql, params);

        if (existingList.isEmpty()) {
            // First transaction of the month for that credit line!
            LocalDate dueDate = card.getDueDate() != null ? card.getDueDate() : card.getPaymentDueDate();
            if (dueDate == null) {
                LocalDate billingDate = card.getBillingDate() != null ? card.getBillingDate() : card.getStatementClosingDate();
                if (billingDate != null) {
                    dueDate = billingDate.plusDays(20);
                } else {
                    dueDate = txnDate.plusDays(25);
                }
            }
            if (dueDate.isBefore(txnDate)) {
                int dueDay = Math.min(dueDate.getDayOfMonth(), txnDate.lengthOfMonth());
                dueDate = txnDate.withDayOfMonth(dueDay);
                if (dueDate.isBefore(txnDate)) {
                    LocalDate nextMonth = txnDate.plusMonths(1);
                    dueDate = nextMonth.withDayOfMonth(Math.min(dueDay, nextMonth.lengthOfMonth()));
                }
            }

            BigDecimal initialAmount = isExpense ? txnAmount : BigDecimal.ZERO;

            // Automatically create an event 3 days before due date
            LocalDate eventDate = dueDate.minusDays(3);
            if (eventDate.isBefore(LocalDate.now())) {
                eventDate = LocalDate.now();
            }

            UUID eventId = UUID.randomUUID();
            Event event = new Event();
            event.setId(eventId);
            event.setTitle("Payment Due: " + card.getCardName());
            event.setDescription("Credit card statement payment of ₹" + initialAmount.toPlainString() + " for " + card.getCardName() + " (•••• " + card.getLastFour() + ") due on " + dueDate + ".");
            event.setEventType("FINANCE");
            event.setStartAt(eventDate.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant());
            event.setEndAt(eventDate.atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant());
            event.setStatus("SCHEDULED");
            event.setAllDay(false);
            event.setRecurring(false);
            event.setLocation("Online Banking / Auto-Debit");
            eventRepository.save(event);

            // Create impending commitment
            UUID commitmentId = UUID.randomUUID();
            boolean isUrgent = ChronoUnit.DAYS.between(LocalDate.now(), dueDate) <= 7;
            String insertSql = """
                INSERT INTO finance_commitments (id, name, due_date, amount, account_id, credit_card_id, event_id, is_urgent, status)
                VALUES (:id, :name, :dueDate, :amount, NULL, :cardId, :eventId, :isUrgent, 'PENDING')
            """;
            jdbc.update(insertSql, new MapSqlParameterSource()
                    .addValue("id", commitmentId)
                    .addValue("name", card.getCardName() + " Payment")
                    .addValue("dueDate", Date.valueOf(dueDate))
                    .addValue("amount", initialAmount)
                    .addValue("cardId", card.getId())
                    .addValue("eventId", eventId)
                    .addValue("isUrgent", isUrgent));
        } else {
            // Commitment already exists! Update commitment amount to reflect adjusted amount
            Map<String, Object> existing = existingList.get(0);
            Object idObj = existing.get("id");
            UUID commId = idObj instanceof UUID u ? u : UUID.fromString(idObj.toString());
            Object amtObj = existing.get("amount");
            BigDecimal oldAmount = amtObj instanceof BigDecimal b ? b : (amtObj != null ? new BigDecimal(amtObj.toString()) : BigDecimal.ZERO);
            BigDecimal newAmount = oldAmount.add(delta).max(BigDecimal.ZERO);

            jdbc.update("UPDATE finance_commitments SET amount = :amount, credit_card_id = :cardId, updated_at = CURRENT_TIMESTAMP WHERE id = :id",
                    new MapSqlParameterSource()
                            .addValue("id", commId)
                            .addValue("cardId", card.getId())
                            .addValue("amount", newAmount));

            Object eventIdObj = existing.get("event_id");
            if (eventIdObj != null) {
                UUID eventId = (UUID) eventIdObj;
                String updatedDesc = "Credit card statement payment of ₹" + newAmount.toPlainString() + " for " + card.getCardName() + " (•••• " + card.getLastFour() + ") due on " + existing.get("due_date") + ".";
                jdbc.update("UPDATE events SET description = :desc, updated_at = CURRENT_TIMESTAMP WHERE id = :id",
                        new MapSqlParameterSource()
                                .addValue("id", eventId)
                                .addValue("desc", updatedDesc));
            }
        }
    }

    private void processCreditCardTransactionOnDelete(Transaction txn) {
        CreditAccount card = findAssociatedCreditCard(txn);
        if (card == null) return;

        BigDecimal txnAmount = txn.getAmount() != null ? txn.getAmount() : BigDecimal.ZERO;
        boolean isExpense = "EXPENSE".equalsIgnoreCase(txn.getTransactionType());
        BigDecimal delta = isExpense ? txnAmount.negate() : txnAmount;

        BigDecimal currentBal = card.getCurrentBalance() != null ? card.getCurrentBalance() : BigDecimal.ZERO;
        card.setCurrentBalance(currentBal.add(delta).max(BigDecimal.ZERO));
        creditAccountRepository.update(card);

        String findSql = """
            SELECT id, name, due_date, amount, event_id FROM finance_commitments
            WHERE (credit_card_id = :cardId OR name = :cardPaymentName) AND status = 'PENDING'
            ORDER BY due_date DESC LIMIT 1
        """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("cardId", card.getId())
                .addValue("cardPaymentName", card.getCardName() + " Payment");
        List<Map<String, Object>> existingList = jdbc.queryForList(findSql, params);

        if (!existingList.isEmpty()) {
            Map<String, Object> existing = existingList.get(0);
            Object idObj = existing.get("id");
            UUID commId = idObj instanceof UUID u ? u : UUID.fromString(idObj.toString());
            Object amtObj = existing.get("amount");
            BigDecimal oldAmount = amtObj instanceof BigDecimal b ? b : (amtObj != null ? new BigDecimal(amtObj.toString()) : BigDecimal.ZERO);
            BigDecimal newAmount = oldAmount.add(delta).max(BigDecimal.ZERO);

            if (newAmount.compareTo(BigDecimal.ZERO) <= 0) {
                jdbc.update("UPDATE finance_commitments SET amount = 0, status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP WHERE id = :id",
                        new MapSqlParameterSource("id", commId));
                Object eventIdObj = existing.get("event_id");
                if (eventIdObj != null) {
                    jdbc.update("UPDATE events SET status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP WHERE id = :id",
                            new MapSqlParameterSource("id", (UUID) eventIdObj));
                }
            } else {
                jdbc.update("UPDATE finance_commitments SET amount = :amount, updated_at = CURRENT_TIMESTAMP WHERE id = :id",
                        new MapSqlParameterSource()
                                .addValue("id", commId)
                                .addValue("amount", newAmount));

                Object eventIdObj = existing.get("event_id");
                if (eventIdObj != null) {
                    UUID eventId = (UUID) eventIdObj;
                    String updatedDesc = "Credit card statement payment of ₹" + newAmount.toPlainString() + " for " + card.getCardName() + " (•••• " + card.getLastFour() + ") due on " + existing.get("due_date") + ".";
                    jdbc.update("UPDATE events SET description = :desc, updated_at = CURRENT_TIMESTAMP WHERE id = :id",
                            new MapSqlParameterSource()
                                    .addValue("id", eventId)
                                    .addValue("desc", updatedDesc));
                }
            }
        }
    }

    // ─── Finance Accounts CRUD ──────────────────────────────────────

    @Transactional
    public FinanceAccount createAccount(FinanceAccount account) {
        if (account.getCurrency() == null) account.setCurrency("INR");
        return accountRepository.save(account);
    }

    @Transactional
    public FinanceAccount updateAccount(UUID id, FinanceAccount updated) {
        FinanceAccount existing = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FinanceAccount", id));
        existing.setAccountName(updated.getAccountName());
        existing.setInstitution(updated.getInstitution());
        existing.setAccountType(updated.getAccountType());
        existing.setBalance(updated.getBalance());
        if (updated.getCurrency() != null) existing.setCurrency(updated.getCurrency());
        return accountRepository.update(existing);
    }

    public List<FinanceAccount> getAllAccounts() {
        return accountRepository.findAll();
    }

    public UUID resolveAccountId(String idStr) {
        if (idStr == null || idStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Account ID cannot be blank");
        }
        String clean = idStr.trim();
        if (clean.length() == 36) {
            try {
                return UUID.fromString(clean);
            } catch (IllegalArgumentException ignored) {}
        }
        String pattern = clean.replace("-", "") + "%";
        String sql = "SELECT id FROM finance_accounts WHERE replace(id::text, '-', '') ILIKE :pat LIMIT 1";
        List<UUID> matches = jdbc.query(sql, new MapSqlParameterSource("pat", pattern), (rs, i) -> (UUID) rs.getObject("id"));
        if (!matches.isEmpty()) {
            return matches.get(0);
        }
        try {
            return UUID.fromString(clean);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("FinanceAccount", clean);
        }
    }

    @Transactional
    public FinanceAccount updateAccount(String idStr, FinanceAccount updated) {
        return updateAccount(resolveAccountId(idStr), updated);
    }

    @Transactional
    public void deleteAccount(UUID id) {
        accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FinanceAccount", id));
        // Decouple commitments, transactions, investments prior to deletion
        jdbc.update("UPDATE finance_commitments SET account_id = NULL WHERE account_id = :id", new MapSqlParameterSource("id", id));
        jdbc.update("UPDATE transactions SET account_id = NULL WHERE account_id = :id", new MapSqlParameterSource("id", id));
        jdbc.update("UPDATE investments SET account_id = NULL WHERE account_id = :id", new MapSqlParameterSource("id", id));
        accountRepository.deleteById(id);
    }

    @Transactional
    public void deleteAccount(String idStr) {
        deleteAccount(resolveAccountId(idStr));
    }

    // ─── Credit Accounts CRUD ───────────────────────────────────────

    @Transactional
    public CreditAccount createCreditAccount(CreditAccount card) {
        return creditAccountRepository.save(card);
    }

    @Transactional
    public CreditAccount updateCreditAccount(UUID id, CreditAccount updated) {
        CreditAccount existing = creditAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CreditAccount", id));
        existing.setCardName(updated.getCardName());
        existing.setLastFour(updated.getLastFour());
        existing.setNetwork(updated.getNetwork());
        existing.setCreditLimit(updated.getCreditLimit());
        existing.setCurrentBalance(updated.getCurrentBalance());
        existing.setStatementClosingDate(updated.getStatementClosingDate());
        existing.setPaymentDueDate(updated.getPaymentDueDate());
        existing.setMinAmountDue(updated.getMinAmountDue());
        existing.setAutoPayEnabled(updated.isAutoPayEnabled());
        existing.setNotes(updated.getNotes());
        return creditAccountRepository.update(existing);
    }

    public List<CreditAccount> getAllCreditAccounts() {
        return creditAccountRepository.findAll();
    }

    @Transactional
    public void deleteCreditAccount(UUID id) {
        creditAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CreditAccount", id));
        jdbc.update("UPDATE finance_commitments SET credit_card_id = NULL WHERE credit_card_id = :id", new MapSqlParameterSource("id", id));
        jdbc.update("UPDATE transactions SET credit_card_id = NULL WHERE credit_card_id = :id", new MapSqlParameterSource("id", id));
        creditAccountRepository.deleteById(id);
    }

    // ─── Investments CRUD ───────────────────────────────────────────

    @Transactional
    public Investment createInvestment(Investment investment) {
        if (investment.getStatus() == null) investment.setStatus("ACTIVE");
        if (investment.getInvestmentType() == null) investment.setInvestmentType("ONE_TIME");
        if (investment.getAssetClass() == null) investment.setAssetClass("EQUITY");
        if (investment.getCurrentValue() == null && investment.getInvestedAmount() != null) {
            investment.setCurrentValue(investment.getInvestedAmount());
        }
        return investmentRepository.save(investment);
    }

    @Transactional
    public Investment updateInvestment(UUID id, Investment updated) {
        Investment existing = investmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Investment", id));
        existing.setName(updated.getName());
        existing.setInvestmentType(updated.getInvestmentType());
        existing.setAssetClass(updated.getAssetClass());
        existing.setInstrument(updated.getInstrument());
        existing.setInvestedAmount(updated.getInvestedAmount());
        existing.setCurrentValue(updated.getCurrentValue());
        existing.setSipAmount(updated.getSipAmount());
        existing.setSipDay(updated.getSipDay());
        existing.setStartDate(updated.getStartDate());
        existing.setEndDate(updated.getEndDate());
        existing.setUnits(updated.getUnits());
        existing.setNav(updated.getNav());
        existing.setFolioNumber(updated.getFolioNumber());
        existing.setBroker(updated.getBroker());
        existing.setNotes(updated.getNotes());
        if (updated.getStatus() != null) existing.setStatus(updated.getStatus());
        existing.setAccountId(updated.getAccountId());
        return investmentRepository.update(existing);
    }

    public List<Investment> getAllInvestments() {
        return investmentRepository.findAll();
    }

    @Transactional
    public void deleteInvestment(UUID id) {
        investmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Investment", id));
        investmentRepository.deleteById(id);
    }

    // ─── Monthly Allowance Setting ───────────────────────────────────

    public BigDecimal getMonthlyAllowance() {
        try {
            String val = jdbc.queryForObject(
                    "SELECT value FROM app_settings WHERE key = 'monthly_expense_allowance'",
                    new org.springframework.jdbc.core.namedparam.MapSqlParameterSource(),
                    String.class);
            return val != null ? new BigDecimal(val) : new BigDecimal("75000");
        } catch (Exception e) {
            return new BigDecimal("75000");
        }
    }

    @Transactional
    public BigDecimal setMonthlyAllowance(BigDecimal amount) {
        jdbc.update("""
                INSERT INTO app_settings (key, value, description, updated_at)
                VALUES ('monthly_expense_allowance', :val, 'Monthly expense budget cap in INR', CURRENT_TIMESTAMP)
                ON CONFLICT (key) DO UPDATE SET value = :val, updated_at = CURRENT_TIMESTAMP
                """,
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("val", amount.toPlainString()));
        return amount;
    }

    // ─── Safe-to-Spend (derived, never persisted) ───────────────────

    /**
     * Calculates safe-to-spend for all active budgets covering today's date,
     * taking into account transactions already performed today.
     */
    public List<SafeToSpend> calculateSafeToSpend() {
        LocalDate today = LocalDate.now();
        List<Budget> activeBudgets = budgetRepository.findActiveForDate(today);
        List<SafeToSpend> results = new ArrayList<>();

        for (Budget budget : activeBudgets) {
            BudgetCategory category = categoryRepository.findById(budget.getCategoryId()).orElse(null);
            if (category == null) continue;

            LocalDate periodStart = budget.getPeriodStart();
            LocalDate periodEnd = budget.getPeriodEnd();
            if ("MONTHLY".equalsIgnoreCase(budget.getPeriodType())) {
                periodStart = today.withDayOfMonth(1);
                periodEnd = today.withDayOfMonth(today.lengthOfMonth());
            }

            // Total expenses in this category across the budget period
            String sqlTotalSpent = """
                SELECT COALESCE(SUM(amount), 0) FROM transactions
                WHERE category_id = :categoryId
                  AND UPPER(transaction_type) = 'EXPENSE'
                  AND transaction_at >= :periodStart::date::timestamptz
                  AND transaction_at < (:periodEnd::date + INTERVAL '1 day')::timestamptz
            """;
            MapSqlParameterSource paramsTotal = new MapSqlParameterSource()
                    .addValue("categoryId", budget.getCategoryId())
                    .addValue("periodStart", Date.valueOf(periodStart))
                    .addValue("periodEnd", Date.valueOf(periodEnd));
            BigDecimal totalSpent = jdbc.queryForObject(sqlTotalSpent, paramsTotal, BigDecimal.class);
            if (totalSpent == null) totalSpent = BigDecimal.ZERO;

            // Expenses in this category done today
            String sqlToday = """
                SELECT COALESCE(SUM(amount), 0) FROM transactions
                WHERE category_id = :categoryId
                  AND UPPER(transaction_type) = 'EXPENSE'
                  AND transaction_at >= :today::date::timestamptz
                  AND transaction_at < (:today::date + INTERVAL '1 day')::timestamptz
            """;
            MapSqlParameterSource paramsToday = new MapSqlParameterSource()
                    .addValue("categoryId", budget.getCategoryId())
                    .addValue("today", Date.valueOf(today));
            BigDecimal spentToday = jdbc.queryForObject(sqlToday, paramsToday, BigDecimal.class);
            if (spentToday == null) spentToday = BigDecimal.ZERO;

            BigDecimal remainingBudget = budget.getAmount().subtract(totalSpent);

            long remainingDays = Math.max(1, ChronoUnit.DAYS.between(today, periodEnd) + 1);

            // Daily allocation calculated from budget available at start of today
            BigDecimal spentBeforeToday = totalSpent.subtract(spentToday).max(BigDecimal.ZERO);
            BigDecimal budgetAtStartOfToday = budget.getAmount().subtract(spentBeforeToday).max(BigDecimal.ZERO);
            BigDecimal dailySafeTarget = budgetAtStartOfToday.divide(BigDecimal.valueOf(remainingDays), 2, RoundingMode.HALF_UP);
            // Deduct transactions done today so safe spend responds immediately to today's activity
            BigDecimal dailySafeToSpend = dailySafeTarget.subtract(spentToday).max(BigDecimal.ZERO);

            results.add(new SafeToSpend(
                    budget.getId(),
                    budget.getName(),
                    category.getId(),
                    category.getName(),
                    budget.getAmount(),
                    totalSpent,
                    remainingBudget,
                    dailySafeToSpend,
                    (int) remainingDays,
                    periodStart,
                    periodEnd
            ));
        }

        return results;
    }

    // --- Analytical Endpoints ---
    public Map<String, Object> getPulse(String cycle) {
        // Sanitize cycle: normalize null, blank, or "monthly" to current YearMonth YYYY-MM
        if (cycle == null || cycle.isBlank() || cycle.equalsIgnoreCase("monthly") || !cycle.matches("^\\d{4}-\\d{2}$")) {
            cycle = YearMonth.now().toString();
        }

        BigDecimal allowance = getMonthlyAllowance();

        String sql = """
            WITH active_month_budgets AS (
                SELECT DISTINCT ON (category_id) amount
                FROM budgets
                WHERE (to_char(period_start, 'YYYY-MM') <= :cycle AND to_char(period_end, 'YYYY-MM') >= :cycle)
                   OR period_type = 'MONTHLY'
                ORDER BY category_id,
                         CASE WHEN to_char(period_start, 'YYYY-MM') <= :cycle AND to_char(period_end, 'YYYY-MM') >= :cycle THEN 0 ELSE 1 END,
                         updated_at DESC
            )
            SELECT
                (SELECT COALESCE(SUM(balance), 0) FROM finance_accounts) as "totalLiquidReserves",
                (SELECT COALESCE(SUM(amount), 0) FROM active_month_budgets) as "monthlyCap",
                (SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE UPPER(transaction_type) = 'EXPENSE' AND to_char(transaction_at, 'YYYY-MM') = :cycle) as "monthlySpent"
        """;
        Map<String, Object> data = jdbc.queryForMap(sql, new MapSqlParameterSource("cycle", cycle));

        BigDecimal budgetedCap = (BigDecimal) data.get("monthlyCap");
        if (budgetedCap == null) budgetedCap = BigDecimal.ZERO;
        BigDecimal spent = (BigDecimal) data.get("monthlySpent");
        if (spent == null) spent = BigDecimal.ZERO;
        BigDecimal reserves = (BigDecimal) data.get("totalLiquidReserves");
        if (reserves == null) reserves = BigDecimal.ZERO;

        // Overall monthly cap is the configured allowance, or the sum of category budgets if higher
        BigDecimal cap = (allowance != null && allowance.compareTo(BigDecimal.ZERO) > 0)
                ? allowance.max(budgetedCap)
                : (budgetedCap.compareTo(BigDecimal.ZERO) > 0 ? budgetedCap : new BigDecimal("75000"));

        double burnPercentage = cap.compareTo(BigDecimal.ZERO) > 0 ? spent.doubleValue() / cap.doubleValue() * 100 : 0.0;
        double runwayMonths = spent.compareTo(BigDecimal.ZERO) > 0 ? reserves.doubleValue() / spent.doubleValue() : 0.0;

        LocalDate today = LocalDate.now();
        int daysInMonth = today.lengthOfMonth();
        long remainingDays = Math.max(1, daysInMonth - today.getDayOfMonth() + 1);

        // Query daily discretionary expenses recorded today (exclude fixed debt/settlement transfers)
        String todayExpenseSql = """
            SELECT COALESCE(SUM(amount), 0) FROM transactions
            WHERE UPPER(transaction_type) = 'EXPENSE'
              AND transaction_at >= CURRENT_DATE::timestamp
              AND transaction_at < (CURRENT_DATE + INTERVAL '1 day')::timestamp
              AND (linked_entity_type IS NULL OR UPPER(linked_entity_type) != 'COMMITMENT')
              AND description NOT ILIKE '%settlement%'
        """;
        BigDecimal discretionaryExpensesToday = jdbc.queryForObject(todayExpenseSql, new MapSqlParameterSource(), BigDecimal.class);
        if (discretionaryExpensesToday == null) discretionaryExpensesToday = BigDecimal.ZERO;

        List<SafeToSpend> safeList = calculateSafeToSpend();
        BigDecimal safeSpendToday;
        boolean isCapReached = spent.compareTo(cap) >= 0;

        if (isCapReached) {
            safeSpendToday = BigDecimal.ZERO;
        } else if (!safeList.isEmpty()) {
            BigDecimal sumCategorySafe = safeList.stream()
                    .map(SafeToSpend::dailySafeToSpend)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Unallocated portion of the monthly cap (allowance minus category envelopes)
            BigDecimal unallocatedCap = cap.subtract(budgetedCap).max(BigDecimal.ZERO);

            // Calculate unallocated spent prior to today
            String unbudgetedBeforeTodaySql = """
                SELECT COALESCE(SUM(t.amount), 0) FROM transactions t
                WHERE UPPER(t.transaction_type) = 'EXPENSE'
                  AND to_char(t.transaction_at, 'YYYY-MM') = :cycle
                  AND t.transaction_at < CURRENT_DATE::timestamp
                  AND (t.linked_entity_type IS NULL OR UPPER(t.linked_entity_type) != 'COMMITMENT')
                  AND t.description NOT ILIKE '%settlement%'
                  AND (t.category_id IS NULL OR t.category_id NOT IN (
                      SELECT category_id FROM budgets
                      WHERE (period_start <= CURRENT_DATE AND period_end >= CURRENT_DATE)
                         OR period_type = 'MONTHLY'
                  ))
            """;
            BigDecimal unbudgetedBeforeToday = jdbc.queryForObject(unbudgetedBeforeTodaySql,
                    new MapSqlParameterSource("cycle", cycle), BigDecimal.class);
            if (unbudgetedBeforeToday == null) unbudgetedBeforeToday = BigDecimal.ZERO;

            // Unbudgeted spent today
            String unbudgetedTodaySql = """
                SELECT COALESCE(SUM(t.amount), 0) FROM transactions t
                WHERE UPPER(t.transaction_type) = 'EXPENSE'
                  AND t.transaction_at >= CURRENT_DATE::timestamp
                  AND t.transaction_at < (CURRENT_DATE + INTERVAL '1 day')::timestamp
                  AND (t.linked_entity_type IS NULL OR UPPER(t.linked_entity_type) != 'COMMITMENT')
                  AND t.description NOT ILIKE '%settlement%'
                  AND (t.category_id IS NULL OR t.category_id NOT IN (
                      SELECT category_id FROM budgets
                      WHERE (period_start <= CURRENT_DATE AND period_end >= CURRENT_DATE)
                         OR period_type = 'MONTHLY'
                  ))
            """;
            BigDecimal unbudgetedToday = jdbc.queryForObject(unbudgetedTodaySql, new MapSqlParameterSource(), BigDecimal.class);
            if (unbudgetedToday == null) unbudgetedToday = BigDecimal.ZERO;

            BigDecimal unallocatedRemaining = unallocatedCap.subtract(unbudgetedBeforeToday).max(BigDecimal.ZERO);
            BigDecimal unallocatedDailyTarget = unallocatedRemaining.divide(BigDecimal.valueOf(remainingDays), 2, RoundingMode.HALF_UP);
            BigDecimal unallocatedSafeToday = unallocatedDailyTarget.subtract(unbudgetedToday).max(BigDecimal.ZERO);

            safeSpendToday = sumCategorySafe.add(unallocatedSafeToday);
        } else {
            // Allowance/cap based daily safe spend
            String spentBeforeTodaySql = """
                SELECT COALESCE(SUM(amount), 0) FROM transactions
                WHERE UPPER(transaction_type) = 'EXPENSE'
                  AND to_char(transaction_at, 'YYYY-MM') = :cycle
                  AND transaction_at < CURRENT_DATE::timestamp
                  AND (linked_entity_type IS NULL OR UPPER(linked_entity_type) != 'COMMITMENT')
                  AND description NOT ILIKE '%settlement%'
            """;
            BigDecimal spentBeforeToday = jdbc.queryForObject(spentBeforeTodaySql, new MapSqlParameterSource("cycle", cycle), BigDecimal.class);
            if (spentBeforeToday == null) spentBeforeToday = BigDecimal.ZERO;

            BigDecimal remainingCap = cap.subtract(spentBeforeToday).max(BigDecimal.ZERO);
            BigDecimal dailyTarget = remainingCap.divide(BigDecimal.valueOf(remainingDays), 2, RoundingMode.HALF_UP);
            safeSpendToday = dailyTarget.subtract(discretionaryExpensesToday).max(BigDecimal.ZERO);
        }

        Map<String, Object> pulseMap = new LinkedHashMap<>();
        pulseMap.put("cycle", cycle);
        pulseMap.put("safeSpendToday", safeSpendToday);
        pulseMap.put("monthlySpent", spent);
        pulseMap.put("monthlyCap", cap);
        pulseMap.put("monthlyAllowance", allowance);
        pulseMap.put("burnPercentage", burnPercentage);
        pulseMap.put("totalLiquidReserves", reserves);
        pulseMap.put("runwayMonths", runwayMonths);
        pulseMap.put("isCapReached", isCapReached);
        pulseMap.put("capRemaining", cap.subtract(spent).max(BigDecimal.ZERO));
        pulseMap.put("remainingDays", remainingDays);
        pulseMap.put("burnStatus", burnPercentage > 90 ? "Critical" : (burnPercentage > 75 ? "Warning" : "Optimal"));

        return pulseMap;
    }

    public Map<String, Object> getMonthlyReport(String cycle) {
        if (cycle == null || cycle.isBlank() || cycle.equalsIgnoreCase("monthly") || !cycle.matches("^\\d{4}-\\d{2}$")) {
            cycle = YearMonth.now().toString();
        }
        YearMonth ym = YearMonth.parse(cycle);
        LocalDate startOfMonth = ym.atDay(1);
        LocalDate endOfMonth = ym.atEndOfMonth();

        // 1. Overall monthly totals
        String totalsSql = """
            SELECT
                COALESCE(SUM(CASE WHEN UPPER(transaction_type) = 'EXPENSE' THEN amount ELSE 0 END), 0) as "totalExpense",
                COALESCE(SUM(CASE WHEN UPPER(transaction_type) = 'INCOME' THEN amount ELSE 0 END), 0) as "totalIncome",
                COUNT(CASE WHEN UPPER(transaction_type) = 'EXPENSE' THEN 1 END) as "expenseCount",
                COUNT(*) as "totalCount"
            FROM transactions
            WHERE to_char(transaction_at, 'YYYY-MM') = :cycle
        """;
        Map<String, Object> totals = jdbc.queryForMap(totalsSql, new MapSqlParameterSource("cycle", cycle));
        BigDecimal totalExpense = (BigDecimal) totals.get("totalExpense");
        BigDecimal totalIncome = (BigDecimal) totals.get("totalIncome");
        long expenseCount = ((Number) totals.get("expenseCount")).longValue();

        // Monthly Cap (from budgets active in this month, or fallback to allowance)
        String capSql = """
            WITH month_budgets AS (
                SELECT DISTINCT ON (b.category_id)
                       b.amount as budget_amount
                FROM budgets b
                WHERE (b.period_start <= :endOfMonth AND b.period_end >= :startOfMonth)
                   OR b.period_type = 'MONTHLY'
                ORDER BY b.category_id,
                         CASE WHEN b.period_start <= :endOfMonth AND b.period_end >= :startOfMonth THEN 0 ELSE 1 END,
                         b.updated_at DESC
            )
            SELECT COALESCE(SUM(budget_amount), 0) FROM month_budgets
        """;
        BigDecimal budgetedCap = jdbc.queryForObject(capSql,
                new MapSqlParameterSource().addValue("startOfMonth", Date.valueOf(startOfMonth)).addValue("endOfMonth", Date.valueOf(endOfMonth)),
                BigDecimal.class);
        if (budgetedCap == null) budgetedCap = BigDecimal.ZERO;

        BigDecimal allowance = getMonthlyAllowance();
        BigDecimal monthlyCap = (allowance != null && allowance.compareTo(BigDecimal.ZERO) > 0)
                ? allowance.max(budgetedCap)
                : (budgetedCap.compareTo(BigDecimal.ZERO) > 0 ? budgetedCap : new BigDecimal("75000"));

        BigDecimal netSavings = totalIncome.subtract(totalExpense);
        BigDecimal dailyAvgBurn = totalExpense.divide(BigDecimal.valueOf(ym.lengthOfMonth()), 2, RoundingMode.HALF_UP);

        // 2. Categorized expenses with their corresponding budget for this month
        String catReportSql = """
            WITH cat_expenses AS (
                SELECT t.category_id,
                       COALESCE(bc.name, 'Uncategorized / General') as category_name,
                       SUM(t.amount) as spent
                FROM transactions t
                LEFT JOIN budget_categories bc ON t.category_id = bc.id
                WHERE UPPER(t.transaction_type) = 'EXPENSE'
                  AND to_char(t.transaction_at, 'YYYY-MM') = :cycle
                GROUP BY t.category_id, bc.name
            ),
            month_budgets AS (
                SELECT DISTINCT ON (b.category_id)
                       b.category_id,
                       b.name as budget_name,
                       b.amount as budget_amount
                FROM budgets b
                WHERE (b.period_start <= :endOfMonth AND b.period_end >= :startOfMonth)
                   OR b.period_type = 'MONTHLY'
                ORDER BY b.category_id,
                         CASE WHEN b.period_start <= :endOfMonth AND b.period_end >= :startOfMonth THEN 0 ELSE 1 END,
                         b.updated_at DESC
            )
            SELECT
                COALESCE(ce.category_id, mb.category_id) as category_id,
                COALESCE(ce.category_name, (SELECT name FROM budget_categories WHERE id = mb.category_id), 'General') as category_name,
                COALESCE(mb.budget_amount, 0) as budget_amount,
                COALESCE(ce.spent, 0) as spent_amount
            FROM cat_expenses ce
            FULL OUTER JOIN month_budgets mb ON ce.category_id = mb.category_id
            ORDER BY spent_amount DESC, budget_amount DESC
        """;
        List<Map<String, Object>> rawCatBreakdown = jdbc.queryForList(catReportSql,
                new MapSqlParameterSource()
                        .addValue("cycle", cycle)
                        .addValue("startOfMonth", Date.valueOf(startOfMonth))
                        .addValue("endOfMonth", Date.valueOf(endOfMonth)));

        List<Map<String, Object>> categoryBreakdown = new ArrayList<>();
        for (Map<String, Object> row : rawCatBreakdown) {
            BigDecimal budgetAmt = (BigDecimal) row.get("budget_amount");
            BigDecimal spentAmt = (BigDecimal) row.get("spent_amount");
            if (budgetAmt == null) budgetAmt = BigDecimal.ZERO;
            if (spentAmt == null) spentAmt = BigDecimal.ZERO;

            double pctOfBudget = budgetAmt.compareTo(BigDecimal.ZERO) > 0
                    ? spentAmt.multiply(BigDecimal.valueOf(100)).divide(budgetAmt, 2, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;
            double pctOfTotalExpense = totalExpense.compareTo(BigDecimal.ZERO) > 0
                    ? spentAmt.multiply(BigDecimal.valueOf(100)).divide(totalExpense, 2, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            String status = "OPTIMAL";
            if (budgetAmt.compareTo(BigDecimal.ZERO) > 0) {
                if (pctOfBudget >= 100.0) status = "EXCEEDED";
                else if (pctOfBudget >= 85.0) status = "CRITICAL";
                else if (pctOfBudget >= 70.0) status = "WARNING";
            } else {
                status = spentAmt.compareTo(BigDecimal.ZERO) > 0 ? "UNBUDGETED" : "NO_ACTIVITY";
            }

            Map<String, Object> catItem = new LinkedHashMap<>();
            catItem.put("categoryId", row.get("category_id") != null ? row.get("category_id").toString() : null);
            catItem.put("categoryName", row.get("category_name"));
            catItem.put("budgetAmount", budgetAmt);
            catItem.put("spentAmount", spentAmt);
            catItem.put("remainingAmount", budgetAmt.subtract(spentAmt));
            catItem.put("percentOfBudget", pctOfBudget);
            catItem.put("percentOfTotalExpense", pctOfTotalExpense);
            catItem.put("status", status);
            categoryBreakdown.add(catItem);
        }

        // 3. Day by day empirical series across the entire month
        String dailySql = """
            WITH month_days AS (
                SELECT generate_series(:startOfMonth::date, :endOfMonth::date, '1 day'::interval)::date as day_date
            ),
            day_spends AS (
                SELECT transaction_at::date as tx_date,
                       SUM(amount) as spend
                FROM transactions
                WHERE UPPER(transaction_type) = 'EXPENSE'
                  AND to_char(transaction_at, 'YYYY-MM') = :cycle
                GROUP BY tx_date
            )
            SELECT to_char(md.day_date, 'DD') as day,
                   to_char(md.day_date, 'YYYY-MM-DD') as date,
                   to_char(md.day_date, 'Mon DD') as label,
                   COALESCE(ds.spend, 0) as spend
            FROM month_days md
            LEFT JOIN day_spends ds ON md.day_date = ds.tx_date
            ORDER BY md.day_date ASC
        """;
        List<Map<String, Object>> dailyExpenses = jdbc.queryForList(dailySql,
                new MapSqlParameterSource()
                        .addValue("cycle", cycle)
                        .addValue("startOfMonth", Date.valueOf(startOfMonth))
                        .addValue("endOfMonth", Date.valueOf(endOfMonth)));

        // 4. Monthly transactions list
        String txListSql = """
            SELECT t.id,
                   to_char(t.transaction_at, 'YYYY-MM-DD') as date,
                   to_char(t.transaction_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as "transactionAt",
                   COALESCE(t.description, 'Expense') as merchant,
                   COALESCE(t.notes, '') as detail,
                   COALESCE(bc.name, 'General') as category,
                   t.category_id as "categoryId",
                   t.payment_method as "paymentMethod",
                   t.amount,
                   LOWER(t.transaction_type) as type,
                   t.account_id as "accountId",
                   fa.institution as "accountName"
            FROM transactions t
            LEFT JOIN budget_categories bc ON t.category_id = bc.id
            LEFT JOIN finance_accounts fa ON t.account_id = fa.id
            WHERE to_char(t.transaction_at, 'YYYY-MM') = :cycle
            ORDER BY t.transaction_at DESC
        """;
        List<Map<String, Object>> monthTransactions = jdbc.queryForList(txListSql,
                new MapSqlParameterSource("cycle", cycle));

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("cycle", cycle);
        report.put("monthLabel", ym.getMonth().name().substring(0, 1) + ym.getMonth().name().substring(1).toLowerCase() + " " + ym.getYear());
        report.put("daysInMonth", ym.lengthOfMonth());
        report.put("totalExpense", totalExpense);
        report.put("totalIncome", totalIncome);
        report.put("netSavings", netSavings);
        report.put("monthlyCap", monthlyCap);
        report.put("dailyAvgBurn", dailyAvgBurn);
        report.put("expenseCount", expenseCount);
        report.put("categoryBreakdown", categoryBreakdown);
        report.put("dailyExpenses", dailyExpenses);
        report.put("transactions", monthTransactions);

        return report;
    }

    public List<Map<String, Object>> getBurnTrajectory(int days, String cycle) {
        if (cycle == null || cycle.isBlank() || cycle.equalsIgnoreCase("monthly") || !cycle.matches("^\\d{4}-\\d{2}$")) {
            cycle = YearMonth.now().toString();
        }
        String sql = """
            WITH date_series AS (
                SELECT (CURRENT_DATE - (n || ' days')::interval)::date AS series_date
                FROM generate_series(:days - 1, 0, -1) AS n
            ),
            daily_expenses AS (
                SELECT transaction_at::date AS tx_date,
                       SUM(amount) AS total_spend,
                       string_agg(description, ', ') AS notes
                FROM transactions
                WHERE UPPER(transaction_type) = 'EXPENSE'
                  AND transaction_at >= CURRENT_DATE - ((:days - 1) || ' days')::interval
                  AND transaction_at < CURRENT_DATE + INTERVAL '1 day'
                GROUP BY tx_date
            ),
            cap_ref AS (
                SELECT COALESCE(
                    NULLIF((SELECT SUM(amount) FROM budgets WHERE period_start <= CURRENT_DATE AND period_end >= CURRENT_DATE), 0),
                    (SELECT CAST(value AS NUMERIC) FROM app_settings WHERE key = 'monthly_expense_allowance'),
                    75000
                ) / 30.0 AS daily_target
            )
            SELECT to_char(ds.series_date, 'DD') AS day,
                   to_char(ds.series_date, 'YYYY-MM-DD') AS date,
                   to_char(ds.series_date, 'Mon DD') AS label,
                   COALESCE(de.total_spend, 0) AS spend,
                   LEAST(100, GREATEST(4, ROUND((COALESCE(de.total_spend, 0) / NULLIF((SELECT daily_target FROM cap_ref), 0)) * 50))) AS "heightPercent",
                   COALESCE(de.notes, '') AS note,
                   CASE WHEN COALESCE(de.total_spend, 0) > (SELECT daily_target * 1.25 FROM cap_ref) THEN true ELSE false END AS warning
            FROM date_series ds
            LEFT JOIN daily_expenses de ON ds.series_date = de.tx_date
            ORDER BY ds.series_date ASC
        """;
        try {
            return jdbc.queryForList(sql, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("days", days));
        } catch (Exception e) {
            String fallbackSql = """
                SELECT to_char(transaction_at, 'DD') as day,
                       to_char(transaction_at, 'YYYY-MM-DD') as date,
                       SUM(amount) as spend,
                       LEAST(100, ROUND(SUM(amount) / 500 * 10)) as "heightPercent",
                       string_agg(description, ', ') as note,
                       false as warning
                FROM transactions
                WHERE UPPER(transaction_type) = 'EXPENSE'
                  AND transaction_at >= (CURRENT_TIMESTAMP - (:days || ' days')::interval)
                GROUP BY date, day
                ORDER BY date
            """;
            return jdbc.queryForList(fallbackSql, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("days", days));
        }
    }

    public List<Map<String, Object>> getAccounts() {
        return getAccountsList();
    }

    public List<Map<String, Object>> getAccountsList() {
        return jdbc.queryForList(
            "SELECT id as \"accountId\", COALESCE(account_name, institution) as \"accountName\", institution, account_type as \"accountType\", balance, currency, to_char(last_reconciled, 'YYYY-MM-DD HH24:MI:SS') as \"lastReconciled\" FROM finance_accounts ORDER BY balance DESC",
            new org.springframework.jdbc.core.namedparam.MapSqlParameterSource());
    }

    public List<Map<String, Object>> getImpendingCommitments(int windowDays) {
        String sql = """
            SELECT id, name, DATE_PART('day', due_date::timestamp - CURRENT_DATE::timestamp) as "dueDays",
                   to_char(due_date, 'Mon DD, YYYY') as "dueDate",
                   amount, account_id as account, credit_card_id as "creditCardId",
                   CASE WHEN is_urgent = true OR due_date <= CURRENT_DATE + INTERVAL '7 days' THEN true ELSE false END as "isUrgent"
            FROM finance_commitments
            WHERE due_date <= CURRENT_DATE + (:windowDays || ' days')::interval
              AND status = 'PENDING'
              AND amount > 0
            ORDER BY due_date
        """;
        return jdbc.queryForList(sql, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("windowDays", windowDays));
    }

    @Transactional
    public Map<String, Object> settleCommitment(Map<String, Object> body) {
        String commitmentId = (String) body.get("commitmentId");
        String accountId = (String) body.get("sourceAccount");
        Number amount = (Number) body.get("amount");
        BigDecimal settleAmount = amount != null ? BigDecimal.valueOf(amount.doubleValue()) : BigDecimal.ZERO;

        List<Map<String, Object>> commList = jdbc.queryForList(
            "SELECT id, name, credit_card_id, event_id FROM finance_commitments WHERE id = :id::uuid",
            new MapSqlParameterSource("id", commitmentId));

        jdbc.update("UPDATE finance_commitments SET status = 'SETTLED', updated_at = CURRENT_TIMESTAMP WHERE id = :id::uuid",
            new MapSqlParameterSource("id", commitmentId));

        String commName = "Commitment Settlement";
        if (!commList.isEmpty()) {
            Map<String, Object> comm = commList.get(0);
            if (comm.get("name") != null) {
                commName = comm.get("name").toString();
            }
            if (comm.get("event_id") != null) {
                jdbc.update("UPDATE events SET status = 'COMPLETED', updated_at = CURRENT_TIMESTAMP WHERE id = :eventId::uuid",
                    new MapSqlParameterSource("eventId", comm.get("event_id").toString()));
            }
            if (comm.get("credit_card_id") != null) {
                jdbc.update("UPDATE credit_accounts SET current_balance = GREATEST(0, current_balance - :amount), updated_at = CURRENT_TIMESTAMP WHERE id = :cardId::uuid",
                    new MapSqlParameterSource()
                        .addValue("cardId", comm.get("credit_card_id").toString())
                        .addValue("amount", settleAmount));
            }
        }

        UUID sourceAccountUuid = null;
        if (accountId != null && accountId.length() == 36) {
            try {
                sourceAccountUuid = UUID.fromString(accountId);
                jdbc.update("UPDATE finance_accounts SET balance = balance - :amount WHERE id = :id",
                    new MapSqlParameterSource("amount", settleAmount).addValue("id", sourceAccountUuid));
            } catch (Exception ignored) {}
        }

        UUID txId = UUID.randomUUID();
        String desc = body.get("merchant") != null ? body.get("merchant").toString() : (commName + " (Settled)");
        String txSql = """
            INSERT INTO transactions (id, transaction_type, amount, transaction_at, description, notes, linked_module, linked_entity_id, linked_entity_type, account_id, payment_method, credit_card_id)
            VALUES (:id, 'EXPENSE', :amount, CURRENT_TIMESTAMP, :desc, 'Settled commitment', 'FINANCE', :commId, 'COMMITMENT', :accountId, 'NET_BANKING', NULL)
        """;
        jdbc.update(txSql, new MapSqlParameterSource()
            .addValue("id", txId)
            .addValue("amount", settleAmount)
            .addValue("desc", desc)
            .addValue("commId", commitmentId)
            .addValue("accountId", sourceAccountUuid));

        BigDecimal updatedReserves = jdbc.queryForObject("SELECT COALESCE(SUM(balance), 0) FROM finance_accounts",
            new MapSqlParameterSource(), BigDecimal.class);

        return Map.of(
                "status", "SETTLED",
                "transactionId", txId.toString(),
                "settlementTimestamp", Instant.now().toString(),
                "updatedReserves", updatedReserves != null ? updatedReserves : BigDecimal.ZERO
        );
    }

    public Map<String, Object> getPortfolio() {
        // Get portfolio allocations from investments table
        BigDecimal liquidReserves = BigDecimal.ZERO;
        try {
            BigDecimal lr = jdbc.queryForObject(
                    "SELECT COALESCE(SUM(balance), 0) FROM finance_accounts WHERE balance > 0",
                    new org.springframework.jdbc.core.namedparam.MapSqlParameterSource(), BigDecimal.class);
            if (lr != null) liquidReserves = lr;
        } catch (Exception ignored) {}

        // Get investments grouped by asset class
        List<Map<String, Object>> investmentAllocations = new ArrayList<>();
        try {
            String invSql = """
                SELECT asset_class as "assetClass",
                       CASE asset_class
                         WHEN 'EQUITY' THEN 'Equities & Index Funds'
                         WHEN 'DEBT' THEN 'Debt & Fixed Income'
                         WHEN 'GOLD' THEN 'Gold & Commodities'
                         WHEN 'REAL_ESTATE' THEN 'Real Estate'
                         WHEN 'CRYPTO' THEN 'Crypto Assets'
                         ELSE 'Other Assets'
                       END as label,
                       SUM(current_value) as "currentValue",
                       SUM(invested_amount) as "investedAmount",
                       COUNT(*) as count
                FROM investments
                WHERE status != 'REDEEMED'
                GROUP BY asset_class
                ORDER BY SUM(current_value) DESC
            """;
            investmentAllocations = jdbc.queryForList(invSql, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource());
        } catch (Exception ignored) {}

        // Also try portfolio_allocations table
        List<Map<String, Object>> rawAllocations = new ArrayList<>();
        try {
            String allocSql = """
                SELECT id, asset_class as "assetClass", label, current_value as "currentValue",
                       unrealized_gain_percent as "unrealizedGainPercent", risk_rating as "riskRating"
                FROM portfolio_allocations
                ORDER BY current_value DESC
            """;
            rawAllocations = jdbc.queryForList(allocSql, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource());
        } catch (Exception ignored) {}

        // Merge: use investment aggregations if available, otherwise use portfolio_allocations
        List<Map<String, Object>> finalAllocations;
        if (!investmentAllocations.isEmpty()) {
            finalAllocations = investmentAllocations;
        } else {
            finalAllocations = rawAllocations;
        }

        BigDecimal sumAllocations = BigDecimal.ZERO;
        for (Map<String, Object> alloc : finalAllocations) {
            BigDecimal val = (BigDecimal) alloc.get("currentValue");
            if (val != null) sumAllocations = sumAllocations.add(val);
        }

        BigDecimal totalInvestmentsValue = sumAllocations;

        // Total credit liabilities (outstanding debt across all credit lines)
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        try {
            BigDecimal tl = jdbc.queryForObject(
                    "SELECT COALESCE(SUM(current_balance), 0) FROM credit_accounts WHERE current_balance > 0",
                    new org.springframework.jdbc.core.namedparam.MapSqlParameterSource(), BigDecimal.class);
            if (tl != null) totalLiabilities = tl;
        } catch (Exception ignored) {}

        BigDecimal totalAssets = totalInvestmentsValue.add(liquidReserves);
        BigDecimal totalNetWorth = totalAssets.subtract(totalLiabilities);

        // Calculate monthly SIP total
        BigDecimal monthlySip = BigDecimal.ZERO;
        try {
            BigDecimal sipTotal = jdbc.queryForObject(
                    "SELECT COALESCE(SUM(sip_amount), 0) FROM investments WHERE investment_type = 'SIP' AND status = 'ACTIVE'",
                    new org.springframework.jdbc.core.namedparam.MapSqlParameterSource(), BigDecimal.class);
            if (sipTotal != null) monthlySip = sipTotal;
        } catch (Exception ignored) {}

        List<Map<String, Object>> allocations = new ArrayList<>();
        int allocIdx = 1;
        for (Map<String, Object> raw : finalAllocations) {
            Map<String, Object> item = new HashMap<>(raw);
            Object idVal = item.get("id");
            item.put("id", idVal != null ? idVal.toString() : "alloc-" + allocIdx++);

            BigDecimal currentVal = (BigDecimal) item.get("currentValue");
            double pct = 0.0;
            if (totalAssets.compareTo(BigDecimal.ZERO) > 0 && currentVal != null) {
                pct = currentVal.multiply(BigDecimal.valueOf(100))
                        .divide(totalAssets, 2, RoundingMode.HALF_UP)
                        .doubleValue();
            }
            item.put("percentage", pct);
            allocations.add(item);
        }

        BigDecimal monthlyNetChange = BigDecimal.ZERO;
        try {
            String netSql = """
                SELECT COALESCE(SUM(CASE WHEN UPPER(transaction_type) = 'INCOME' THEN amount ELSE -amount END), 0)
                FROM transactions
                WHERE transaction_at >= CURRENT_DATE - INTERVAL '30 days'
            """;
            BigDecimal actualNet = jdbc.queryForObject(netSql, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource(), BigDecimal.class);
            if (actualNet != null) monthlyNetChange = actualNet;
        } catch (Exception ignored) {}

        // Liquid cash portion
        Map<String, Object> liquidSlice = new LinkedHashMap<>();
        liquidSlice.put("id", "liquid-cash");
        liquidSlice.put("assetClass", "LIQUID");
        liquidSlice.put("label", "Liquid Cash & Bank Reserves");
        liquidSlice.put("currentValue", liquidReserves);
        liquidSlice.put("investedAmount", liquidReserves);
        double liquidPct = totalAssets.compareTo(BigDecimal.ZERO) > 0
                ? liquidReserves.multiply(BigDecimal.valueOf(100)).divide(totalAssets, 2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;
        liquidSlice.put("percentage", liquidPct);
        if (!allocations.stream().anyMatch(a -> "liquid-cash".equals(a.get("id")) || "LIQUID".equals(a.get("assetClass")))) {
            allocations.add(liquidSlice);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalAssets", totalAssets);
        response.put("totalLiabilities", totalLiabilities);
        response.put("totalNetWorth", totalNetWorth);
        response.put("totalInvestmentsValue", totalInvestmentsValue);
        response.put("liquidReserves", liquidReserves);
        response.put("monthlySip", monthlySip);
        response.put("currency", "INR");
        response.put("updatedAt", Instant.now().toString());
        response.put("allocations", allocations);
        response.put("monthlyNetChange", monthlyNetChange);
        response.put("investments", getAllInvestments());

        return response;
    }

    public Map<String, Object> getCreditUtilization() {
        List<CreditAccount> cards = creditAccountRepository.findAll();

        BigDecimal totalCreditLimit = BigDecimal.ZERO;
        BigDecimal totalOutstanding = BigDecimal.ZERO;

        for (CreditAccount card : cards) {
            if (card.getCreditLimit() != null) totalCreditLimit = totalCreditLimit.add(card.getCreditLimit());
            if (card.getCurrentBalance() != null) totalOutstanding = totalOutstanding.add(card.getCurrentBalance());
        }

        double aggregateUtilizationPercent = 0.0;
        if (totalCreditLimit.compareTo(BigDecimal.ZERO) > 0) {
            aggregateUtilizationPercent = totalOutstanding.multiply(BigDecimal.valueOf(100))
                    .divide(totalCreditLimit, 2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        String status = "OPTIMAL";
        if (aggregateUtilizationPercent > 50.0) {
            status = aggregateUtilizationPercent > 75.0 ? "CRITICAL" : "WARNING";
        } else if (aggregateUtilizationPercent > 30.0) {
            status = "MODERATE";
        }

        List<Map<String, Object>> cardList = new ArrayList<>();
        for (CreditAccount card : cards) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("cardId", card.getCardId());
            c.put("id", card.getId().toString());
            c.put("cardName", card.getCardName());
            c.put("lastFour", card.getLastFour());
            c.put("network", card.getNetwork());
            c.put("creditLimit", card.getCreditLimit());
            c.put("currentBalance", card.getCurrentBalance());
            c.put("statementClosingDate", card.getStatementClosingDate() != null ? card.getStatementClosingDate().toString() : null);
            c.put("paymentDueDate", card.getPaymentDueDate() != null ? card.getPaymentDueDate().toString() : null);
            c.put("minAmountDue", card.getMinAmountDue());
            c.put("autoPayEnabled", card.isAutoPayEnabled());
            c.put("notes", card.getNotes());

            BigDecimal limit = card.getCreditLimit();
            BigDecimal balance = card.getCurrentBalance();
            double cardUtil = 0.0;
            if (limit != null && limit.compareTo(BigDecimal.ZERO) > 0 && balance != null) {
                cardUtil = balance.multiply(BigDecimal.valueOf(100))
                        .divide(limit, 2, RoundingMode.HALF_UP)
                        .doubleValue();
            }
            c.put("utilizationPercent", cardUtil);
            cardList.add(c);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalCreditLimit", totalCreditLimit);
        response.put("totalOutstanding", totalOutstanding);
        response.put("aggregateUtilizationPercent", aggregateUtilizationPercent);
        response.put("status", status);
        response.put("cards", cardList);

        return response;
    }
}
