package com.monday.app.finance.controller;

import com.monday.app.finance.dto.SafeToSpend;
import com.monday.app.finance.entity.Budget;
import com.monday.app.finance.entity.BudgetCategory;
import com.monday.app.finance.entity.Transaction;
import com.monday.app.finance.service.FinanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/finance")
public class FinanceController {

    private final FinanceService financeService;

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
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
    public ResponseEntity<BudgetCategory> updateCategory(@PathVariable UUID id, @Valid @RequestBody BudgetCategory category) {
        return ResponseEntity.ok(financeService.updateCategory(id, category));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
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
    public ResponseEntity<Budget> updateBudget(@PathVariable UUID id, @Valid @RequestBody Budget budget) {
        return ResponseEntity.ok(financeService.updateBudget(id, budget));
    }

    @DeleteMapping("/budgets/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable UUID id) {
        financeService.deleteBudget(id);
        return ResponseEntity.noContent().build();
    }

    // --- Transactions ---
    @PostMapping("/transactions")
    public ResponseEntity<Transaction> createTransaction(@Valid @RequestBody Transaction transaction) {
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.createTransaction(transaction));
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

    // --- Mock Endpoints from Architecture Doc ---

    @GetMapping("/pulse")
    public ResponseEntity<java.util.Map<String, Object>> getPulse(
            @RequestParam(required = false) String cycle) {
        return ResponseEntity.ok(financeService.getPulse(cycle));
    }

    @GetMapping("/burn-trajectory")
    public ResponseEntity<List<java.util.Map<String, Object>>> getBurnTrajectory(
            @RequestParam(required = false, defaultValue = "14") int days,
            @RequestParam(required = false) String cycle) {
        return ResponseEntity.ok(financeService.getBurnTrajectory(days, cycle));
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<java.util.Map<String, Object>>> getAccounts() {
        return ResponseEntity.ok(financeService.getAccounts());
    }

    @GetMapping("/commitments/impending")
    public ResponseEntity<List<java.util.Map<String, Object>>> getImpendingCommitments(
            @RequestParam(required = false, defaultValue = "30") int windowDays) {
        return ResponseEntity.ok(financeService.getImpendingCommitments(windowDays));
    }

    @PostMapping("/commitments/settle")
    public ResponseEntity<java.util.Map<String, Object>> settleCommitment(
            @RequestBody java.util.Map<String, Object> body) {
        return ResponseEntity.ok(financeService.settleCommitment(body));
    }
}
