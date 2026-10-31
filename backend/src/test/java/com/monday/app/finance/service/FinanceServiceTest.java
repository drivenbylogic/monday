package com.monday.app.finance.service;

import com.monday.app.event.repository.EventRepository;
import com.monday.app.finance.dto.EmiConversionRequest;
import com.monday.app.finance.dto.EmiConversionResult;
import com.monday.app.finance.entity.BudgetCategory;
import com.monday.app.finance.entity.CreditAccount;
import com.monday.app.finance.entity.Transaction;
import com.monday.app.finance.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FinanceServiceTest {

    private TransactionRepository transactionRepository;
    private BudgetRepository budgetRepository;
    private BudgetCategoryRepository categoryRepository;
    private FinanceAccountRepository accountRepository;
    private CreditAccountRepository creditAccountRepository;
    private InvestmentRepository investmentRepository;
    private EventRepository eventRepository;
    private NamedParameterJdbcTemplate jdbc;

    private FinanceService financeService;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        budgetRepository = mock(BudgetRepository.class);
        categoryRepository = mock(BudgetCategoryRepository.class);
        accountRepository = mock(FinanceAccountRepository.class);
        creditAccountRepository = mock(CreditAccountRepository.class);
        investmentRepository = mock(InvestmentRepository.class);
        eventRepository = mock(EventRepository.class);
        jdbc = mock(NamedParameterJdbcTemplate.class);

        financeService = new FinanceService(
                transactionRepository,
                budgetRepository,
                categoryRepository,
                accountRepository,
                creditAccountRepository,
                investmentRepository,
                eventRepository,
                jdbc
        );
    }

    @Test
    void updateCategory_preservesExistingStatusWhenNullProvided() {
        UUID catId = UUID.randomUUID();
        BudgetCategory existing = new BudgetCategory(catId, "Old Name", "Old Desc", "ACTIVE", Instant.now(), Instant.now());
        when(categoryRepository.findById(catId)).thenReturn(Optional.of(existing));
        when(categoryRepository.update(any(BudgetCategory.class))).thenAnswer(inv -> inv.getArgument(0));

        BudgetCategory updateRequest = new BudgetCategory();
        updateRequest.setName("New Name");
        updateRequest.setDescription("New Desc");
        // status is null

        BudgetCategory result = financeService.updateCategory(catId, updateRequest);

        assertNotNull(result);
        assertEquals("New Name", result.getName());
        assertEquals("New Desc", result.getDescription());
        assertEquals("ACTIVE", result.getStatus(), "Status must not be overwritten with null");
        verify(categoryRepository).update(existing);
    }

    @Test
    void createTransactionsBulk_savesAllTransactions() {
        Transaction t1 = new Transaction();
        t1.setAmount(new BigDecimal("100.00"));
        t1.setTransactionType("EXPENSE");
        t1.setDescription("Item 1");

        Transaction t2 = new Transaction();
        t2.setAmount(new BigDecimal("200.00"));
        t2.setTransactionType("EXPENSE");
        t2.setDescription("Item 2");

        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());
        when(creditAccountRepository.findAll()).thenReturn(Collections.emptyList());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Transaction> created = financeService.createTransactionsBulk(List.of(t1, t2));

        assertEquals(2, created.size());
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void convertToEmi_breaksUpCreditCardTransactionIntoInstallments() {
        UUID txId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();

        CreditAccount card = new CreditAccount();
        card.setId(cardId);
        card.setCardName("Amex Platinum");
        card.setLastFour("1234");
        card.setCurrentBalance(new BigDecimal("60000.00"));

        Transaction original = new Transaction();
        original.setId(txId);
        original.setTransactionType("EXPENSE");
        original.setAmount(new BigDecimal("60000.00"));
        original.setDescription("New Laptop");
        original.setCreditCardId(cardId);
        original.setPaymentMethod("Credit Card •• 1234");
        original.setTransactionAt(Instant.now());

        when(transactionRepository.findById(txId)).thenReturn(Optional.of(original));
        when(creditAccountRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(creditAccountRepository.findAll()).thenReturn(List.of(card));
        when(transactionRepository.update(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // When updating transaction, it checks commitments
        when(jdbc.queryForList(anyString(), any(org.springframework.jdbc.core.namedparam.MapSqlParameterSource.class)))
                .thenReturn(Collections.emptyList());

        EmiConversionRequest request = new EmiConversionRequest(6, BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now());

        EmiConversionResult result = financeService.convertToEmi(txId, request);

        assertNotNull(result);
        assertEquals(6, result.tenureMonths());
        assertEquals(new BigDecimal("60000.00"), result.principalAmount());
        assertEquals(new BigDecimal("10000.00"), result.monthlyInstallment());
        assertEquals(new BigDecimal("60000.00"), result.totalPayable());
        assertEquals(BigDecimal.ZERO, result.totalInterest());
        assertTrue(result.installments().isEmpty());

        // Verify original transaction was deleted/reverted from transactions
        verify(transactionRepository).deleteById(txId);
        // Verify all 6 installments were recorded as impending commitments, NOT premature transactions
        verify(transactionRepository, never()).save(argThat(t -> "EMI_INSTALLMENT".equals(t.getLinkedEntityType())));
        verify(jdbc, times(6)).update(contains("INSERT INTO finance_commitments"), any(org.springframework.jdbc.core.namedparam.MapSqlParameterSource.class));
    }

    @Test
    void convertToEmi_withInterestAndProcessingFee_calculatesAccurately() {
        UUID txId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();

        CreditAccount card = new CreditAccount();
        card.setId(cardId);
        card.setCardName("HDFC Regalia");
        card.setLastFour("5678");

        Transaction original = new Transaction();
        original.setId(txId);
        original.setTransactionType("EXPENSE");
        original.setAmount(new BigDecimal("12000.00"));
        original.setDescription("Smartphone");
        original.setCreditCardId(cardId);
        original.setTransactionAt(Instant.now());

        when(transactionRepository.findById(txId)).thenReturn(Optional.of(original));
        when(creditAccountRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(creditAccountRepository.findAll()).thenReturn(List.of(card));
        when(transactionRepository.update(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jdbc.queryForList(anyString(), any(org.springframework.jdbc.core.namedparam.MapSqlParameterSource.class)))
                .thenReturn(Collections.emptyList());

        EmiConversionRequest request = new EmiConversionRequest(3, new BigDecimal("15.0"), new BigDecimal("199.00"), LocalDate.now());

        EmiConversionResult result = financeService.convertToEmi(txId, request);

        assertNotNull(result);
        assertEquals(3, result.tenureMonths());
        assertEquals(new BigDecimal("12000.00"), result.principalAmount());
        assertTrue(result.monthlyInstallment().compareTo(new BigDecimal("4000.00")) > 0);
        assertTrue(result.totalInterest().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(new BigDecimal("199.00"), result.processingFee());
        // All installments and fee are recorded as impending commitments (0 premature transactions)
        assertTrue(result.installments().isEmpty());
        verify(transactionRepository).deleteById(txId);
        verify(jdbc, times(4)).update(contains("INSERT INTO finance_commitments"), any(org.springframework.jdbc.core.namedparam.MapSqlParameterSource.class));
    }

    @Test
    void convertToEmi_failsWhenNotExpenseOrNotCreditCard() {
        UUID txId = UUID.randomUUID();
        Transaction incomeTx = new Transaction();
        incomeTx.setId(txId);
        incomeTx.setTransactionType("INCOME");
        incomeTx.setAmount(new BigDecimal("50000.00"));

        when(transactionRepository.findById(txId)).thenReturn(Optional.of(incomeTx));

        assertThrows(IllegalArgumentException.class, () ->
                financeService.convertToEmi(txId, new EmiConversionRequest(3, null, null, null))
        );
    }
}

