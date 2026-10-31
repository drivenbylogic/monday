package com.monday.app.finance.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.app.finance.dto.EmiConversionRequest;
import com.monday.app.finance.dto.EmiConversionResult;
import com.monday.app.finance.dto.SafeToSpend;
import com.monday.app.finance.entity.*;
import com.monday.app.finance.service.FinanceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/finance")
public class FinanceController {

    private final FinanceService financeService;
    private final ObjectMapper objectMapper;

    @Autowired
    public FinanceController(FinanceService financeService, ObjectMapper objectMapper) {
        this.financeService = financeService;
        this.objectMapper = objectMapper;
    }

    public FinanceController(FinanceService financeService) {
        this(financeService, new ObjectMapper().findAndRegisterModules());
    }

    // --- Budget Categories ---
    @PostMapping("/categories")
    public ResponseEntity<BudgetCategory> createCategory(@Valid @RequestBody BudgetCategory category) {
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.createCategory(category));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<BudgetCategory>> getAllCategories() {
        return ResponseEntity.ok(financeService.getAllCategories());
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<BudgetCategory> updateCategory(@PathVariable String id, @RequestBody BudgetCategory category) {
        return ResponseEntity.ok(financeService.updateCategory(id, category));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable String id) {
        financeService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    // --- Budgets ---
    @PostMapping("/budgets")
    public ResponseEntity<Budget> createBudget(@Valid @RequestBody Budget budget) {
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.createBudget(budget));
    }

    @GetMapping("/budgets")
    public ResponseEntity<List<Budget>> getAllBudgets() {
        return ResponseEntity.ok(financeService.getAllBudgets());
    }

    @PutMapping("/budgets/{id}")
    public ResponseEntity<Budget> updateBudget(@PathVariable String id, @Valid @RequestBody Budget budget) {
        return ResponseEntity.ok(financeService.updateBudget(id, budget));
    }

    @DeleteMapping("/budgets/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable String id) {
        financeService.deleteBudget(id);
        return ResponseEntity.noContent().build();
    }

    // --- Transactions ---
    @PostMapping("/transactions")
    public ResponseEntity<Transaction> createTransaction(@Valid @RequestBody Transaction transaction) {
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.createTransaction(transaction));
    }

    @PostMapping("/transactions/bulk")
    public ResponseEntity<List<Transaction>> createTransactionsBulk(@RequestBody Object payload) {
        ObjectMapper mapper = (this.objectMapper != null ? this.objectMapper.copy() : new ObjectMapper().findAndRegisterModules())
                .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<Transaction> transactions;
        if (payload instanceof List<?> list) {
            transactions = mapper.convertValue(list, new TypeReference<List<Transaction>>() {});
        } else if (payload instanceof Map<?, ?> map && map.containsKey("transactions")) {
            transactions = mapper.convertValue(map.get("transactions"), new TypeReference<List<Transaction>>() {});
        } else {
            throw new IllegalArgumentException("Payload must be a list of transactions or an object containing a 'transactions' array");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.createTransactionsBulk(transactions));
    }

    @PostMapping("/transactions/{id}/convert-to-emi")
    public ResponseEntity<EmiConversionResult> convertToEmi(
            @PathVariable UUID id,
            @Valid @RequestBody EmiConversionRequest request) {
        return ResponseEntity.ok(financeService.convertToEmi(id, request));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getTransactions(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        if (from != null && to != null) {
            return ResponseEntity.ok(financeService.getTransactionsByDateRange(from, to));
        }
        return ResponseEntity.ok(financeService.getAllTransactions());
    }

    @PutMapping("/transactions/{id}")
    public ResponseEntity<Transaction> updateTransaction(@PathVariable UUID id, @Valid @RequestBody Transaction transaction) {
        return ResponseEntity.ok(financeService.updateTransaction(id, transaction));
    }

    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable UUID id) {
        financeService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }

    // --- Safe to Spend ---
    @GetMapping("/safe-to-spend")
    public ResponseEntity<List<SafeToSpend>> getSafeToSpend() {
        return ResponseEntity.ok(financeService.calculateSafeToSpend());
    }

    // --- Analytical Endpoints ---

    @GetMapping("/pulse")
    public ResponseEntity<Map<String, Object>> getPulse(
            @RequestParam(required = false) String cycle) {
        return ResponseEntity.ok(financeService.getPulse(cycle));
    }

    @GetMapping("/burn-trajectory")
    public ResponseEntity<List<Map<String, Object>>> getBurnTrajectory(
            @RequestParam(required = false, defaultValue = "14") int days,
            @RequestParam(required = false) String cycle) {
        return ResponseEntity.ok(financeService.getBurnTrajectory(days, cycle));
    }

    @GetMapping("/reports/monthly")
    public ResponseEntity<Map<String, Object>> getMonthlyReport(
            @RequestParam(required = false) String cycle) {
        return ResponseEntity.ok(financeService.getMonthlyReport(cycle));
    }

    // --- Accounts CRUD ---
    @PostMapping("/accounts")
    public ResponseEntity<FinanceAccount> createAccount(@Valid @RequestBody FinanceAccount account) {
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.createAccount(account));
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<Map<String, Object>>> getAccounts() {
        return ResponseEntity.ok(financeService.getAccounts());
    }

    @PutMapping("/accounts/{id}")
    public ResponseEntity<FinanceAccount> updateAccount(@PathVariable String id, @Valid @RequestBody FinanceAccount account) {
        return ResponseEntity.ok(financeService.updateAccount(id, account));
    }

    @DeleteMapping("/accounts/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable String id) {
        financeService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }

    // --- Commitments ---
    @GetMapping("/commitments/impending")
    public ResponseEntity<List<Map<String, Object>>> getImpendingCommitments(
            @RequestParam(required = false, defaultValue = "30") int windowDays) {
        return ResponseEntity.ok(financeService.getImpendingCommitments(windowDays));
    }

    @PostMapping("/commitments/settle")
    public ResponseEntity<Map<String, Object>> settleCommitment(
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(financeService.settleCommitment(body));
    }

    // --- Net Worth & Investment Portfolio Tracking ---
    @GetMapping("/portfolio")
    public ResponseEntity<Map<String, Object>> getPortfolio() {
        return ResponseEntity.ok(financeService.getPortfolio());
    }

    // --- Credit Card Lines CRUD & Metrics ---
    @PostMapping("/credit-cards")
    public ResponseEntity<CreditAccount> createCreditCard(@Valid @RequestBody CreditAccount card) {
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.createCreditAccount(card));
    }

    @GetMapping("/credit-cards")
    public ResponseEntity<List<CreditAccount>> getAllCreditCards() {
        return ResponseEntity.ok(financeService.getAllCreditAccounts());
    }

    @PutMapping("/credit-cards/{id}")
    public ResponseEntity<CreditAccount> updateCreditCard(@PathVariable UUID id, @Valid @RequestBody CreditAccount card) {
        return ResponseEntity.ok(financeService.updateCreditAccount(id, card));
    }

    @DeleteMapping("/credit-cards/{id}")
    public ResponseEntity<Void> deleteCreditCard(@PathVariable UUID id) {
        financeService.deleteCreditAccount(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/credit-utilization")
    public ResponseEntity<Map<String, Object>> getCreditUtilization() {
        return ResponseEntity.ok(financeService.getCreditUtilization());
    }

    // --- Investments CRUD ---
    @PostMapping("/investments")
    public ResponseEntity<Investment> createInvestment(@Valid @RequestBody Investment investment) {
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.createInvestment(investment));
    }

    @GetMapping("/investments")
    public ResponseEntity<List<Investment>> getAllInvestments() {
        return ResponseEntity.ok(financeService.getAllInvestments());
    }

    @PutMapping("/investments/{id}")
    public ResponseEntity<Investment> updateInvestment(@PathVariable UUID id, @Valid @RequestBody Investment investment) {
        return ResponseEntity.ok(financeService.updateInvestment(id, investment));
    }

    @DeleteMapping("/investments/{id}")
    public ResponseEntity<Void> deleteInvestment(@PathVariable UUID id) {
        financeService.deleteInvestment(id);
        return ResponseEntity.noContent().build();
    }

    // --- Monthly Expense Allowance Settings ---
    @GetMapping("/settings/monthly-allowance")
    public ResponseEntity<Map<String, Object>> getMonthlyAllowance() {
        return ResponseEntity.ok(Map.of("monthlyAllowance", financeService.getMonthlyAllowance()));
    }

    @PutMapping("/settings/monthly-allowance")
    public ResponseEntity<Map<String, Object>> updateMonthlyAllowance(@RequestBody Map<String, Object> body) {
        Object val = body.get("monthlyAllowance") != null ? body.get("monthlyAllowance") : body.get("amount");
        BigDecimal amount;
        if (val instanceof Number) {
            amount = BigDecimal.valueOf(((Number) val).doubleValue());
        } else if (val != null) {
            amount = new BigDecimal(val.toString());
        } else {
            amount = new BigDecimal("75000");
        }
        BigDecimal updated = financeService.setMonthlyAllowance(amount);
        return ResponseEntity.ok(Map.of("monthlyAllowance", updated));
    }
}
