package com.monday.app.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.app.finance.dto.EmiConversionRequest;
import com.monday.app.finance.dto.EmiConversionResult;
import com.monday.app.finance.entity.BudgetCategory;
import com.monday.app.finance.entity.Transaction;
import com.monday.app.finance.service.FinanceService;
import com.monday.app.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FinanceControllerTest {

    private MockMvc mockMvc;
    private FinanceService financeService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        financeService = mock(FinanceService.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(new FinanceController(financeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getPortfolio_returnsPortfolioResponse() throws Exception {
        Map<String, Object> portfolioData = Map.of(
                "totalNetWorth", new BigDecimal("48250000"),
                "liquidReserves", new BigDecimal("412850"),
                "currency", "INR",
                "updatedAt", "2026-10-14T08:30:00Z",
                "allocations", List.of(
                        Map.of(
                                "id", "alloc-1",
                                "assetClass", "EQUITY",
                                "label", "Public Markets & Indexes",
                                "currentValue", new BigDecimal("28400000"),
                                "percentage", 58.86,
                                "unrealizedGainPercent", 18.4,
                                "riskRating", "MODERATE"
                        )
                ),
                "monthlyNetChange", new BigDecimal("142000")
        );

        when(financeService.getPortfolio()).thenReturn(portfolioData);

        mockMvc.perform(get("/api/v1/finance/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.totalNetWorth").value(48250000))
                .andExpect(jsonPath("$.liquidReserves").value(412850))
                .andExpect(jsonPath("$.allocations[0].assetClass").value("EQUITY"))
                .andExpect(jsonPath("$.monthlyNetChange").value(142000));

        verify(financeService).getPortfolio();
    }

    @Test
    void getCreditUtilization_returnsCreditTelemetry() throws Exception {
        Map<String, Object> card = new java.util.HashMap<>();
        card.put("cardId", "card-amex-plat");
        card.put("cardName", "Amex Platinum Reserve");
        card.put("lastFour", "8910");
        card.put("network", "AMEX");
        card.put("creditLimit", new BigDecimal("1000000"));
        card.put("currentBalance", new BigDecimal("128450"));
        card.put("utilizationPercent", 12.85);
        card.put("statementClosingDate", "2026-10-24");
        card.put("paymentDueDate", "2026-11-08");
        card.put("minAmountDue", new BigDecimal("6420"));
        card.put("autoPayEnabled", true);

        Map<String, Object> creditData = Map.of(
                "totalCreditLimit", new BigDecimal("1500000"),
                "totalOutstanding", new BigDecimal("184500"),
                "aggregateUtilizationPercent", 12.3,
                "status", "OPTIMAL",
                "cards", List.of(card)
        );

        when(financeService.getCreditUtilization()).thenReturn(creditData);

        mockMvc.perform(get("/api/v1/finance/credit-utilization"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPTIMAL"))
                .andExpect(jsonPath("$.totalCreditLimit").value(1500000))
                .andExpect(jsonPath("$.totalOutstanding").value(184500))
                .andExpect(jsonPath("$.cards[0].cardId").value("card-amex-plat"));

        verify(financeService).getCreditUtilization();
    }

    @Test
    void createTransaction_withCrossModuleLinkage_returnsCreatedTransaction() throws Exception {
        UUID txId = UUID.randomUUID();
        Transaction created = new Transaction(
                txId, "EXPENSE", new BigDecimal("4850.00"), Instant.now(),
                null, "AMEX_8910", "XPulse 200 Insurance Renewal", "Notes",
                "VEHICLES", "veh-xpulse-200", "VEHICLE_INSURANCE",
                Instant.now(), Instant.now()
        );

        when(financeService.createTransaction(any(Transaction.class))).thenReturn(created);

        String payload = """
                {
                    "amount": 4850.00,
                    "transactionType": "EXPENSE",
                    "description": "XPulse 200 Insurance Renewal",
                    "paymentMethod": "AMEX_8910",
                    "linkedModule": "VEHICLES",
                    "linkedEntityId": "veh-xpulse-200",
                    "linkedEntityType": "VEHICLE_INSURANCE"
                }
                """;

        mockMvc.perform(post("/api/v1/finance/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(txId.toString()))
                .andExpect(jsonPath("$.linkedModule").value("VEHICLES"))
                .andExpect(jsonPath("$.linkedEntityId").value("veh-xpulse-200"))
                .andExpect(jsonPath("$.linkedEntityType").value("VEHICLE_INSURANCE"));
    }

    @Test
    void createTransaction_withInvalidLinkedModule_returnsValidationError() throws Exception {
        when(financeService.createTransaction(any(Transaction.class)))
                .thenThrow(new IllegalArgumentException("Invalid linkedModule: INVALID_MOD"));

        String payload = """
                {
                    "amount": 100.00,
                    "transactionType": "EXPENSE",
                    "description": "Invalid test",
                    "linkedModule": "INVALID_MOD"
                }
                """;

        mockMvc.perform(post("/api/v1/finance/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(true))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateCategory_success() throws Exception {
        UUID catId = UUID.randomUUID();
        BudgetCategory updated = new BudgetCategory(catId, "Updated Dining", "New Description", "ACTIVE", Instant.now(), Instant.now());
        when(financeService.updateCategory(eq(catId.toString()), any(BudgetCategory.class))).thenReturn(updated);

        String payload = """
                {
                    "name": "Updated Dining",
                    "description": "New Description"
                }
                """;

        mockMvc.perform(put("/api/v1/finance/categories/" + catId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(catId.toString()))
                .andExpect(jsonPath("$.name").value("Updated Dining"))
                .andExpect(jsonPath("$.description").value("New Description"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createTransactionsBulk_withArray_returnsCreatedTransactions() throws Exception {
        UUID tx1 = UUID.randomUUID();
        UUID tx2 = UUID.randomUUID();
        Transaction t1 = new Transaction(tx1, "EXPENSE", new BigDecimal("150.00"), Instant.now(), null, "UPI", "Coffee", null, Instant.now(), Instant.now());
        Transaction t2 = new Transaction(tx2, "EXPENSE", new BigDecimal("450.00"), Instant.now(), null, "UPI", "Books", null, Instant.now(), Instant.now());

        when(financeService.createTransactionsBulk(anyList())).thenReturn(List.of(t1, t2));

        String payload = """
                [
                    {
                        "amount": 150.00,
                        "transactionType": "EXPENSE",
                        "description": "Coffee",
                        "paymentMethod": "UPI"
                    },
                    {
                        "amount": 450.00,
                        "transactionType": "EXPENSE",
                        "description": "Books",
                        "paymentMethod": "UPI"
                    }
                ]
                """;

        mockMvc.perform(post("/api/v1/finance/transactions/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].description").value("Coffee"))
                .andExpect(jsonPath("$[1].description").value("Books"));
    }

    @Test
    void createTransactionsBulk_withObjectWrapper_returnsCreatedTransactions() throws Exception {
        UUID tx1 = UUID.randomUUID();
        Transaction t1 = new Transaction(tx1, "EXPENSE", new BigDecimal("150.00"), Instant.now(), null, "UPI", "Coffee", null, Instant.now(), Instant.now());

        when(financeService.createTransactionsBulk(anyList())).thenReturn(List.of(t1));

        String payload = """
                {
                    "transactions": [
                        {
                            "amount": 150.00,
                            "transactionType": "EXPENSE",
                            "description": "Coffee",
                            "paymentMethod": "UPI"
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/api/v1/finance/transactions/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].description").value("Coffee"));
    }

    @Test
    void convertToEmi_success() throws Exception {
        UUID txId = UUID.randomUUID();
        UUID emiGroupId = UUID.randomUUID();
        UUID inst1 = txId;
        UUID inst2 = UUID.randomUUID();
        Transaction t1 = new Transaction(inst1, "EXPENSE", new BigDecimal("5000.00"), Instant.now(), null, "AMEX", "Laptop (EMI 1/2)", null, Instant.now(), Instant.now());
        Transaction t2 = new Transaction(inst2, "EXPENSE", new BigDecimal("5000.00"), Instant.now(), null, "AMEX", "Laptop (EMI 2/2)", null, Instant.now(), Instant.now());

        EmiConversionResult result = new EmiConversionResult(
                emiGroupId,
                txId,
                2,
                new BigDecimal("10000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("10000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(t1, t2)
        );

        when(financeService.convertToEmi(eq(txId), any(EmiConversionRequest.class))).thenReturn(result);

        String payload = """
                {
                    "tenureMonths": 2,
                    "annualInterestRate": 0.0,
                    "processingFee": 0.0
                }
                """;

        mockMvc.perform(post("/api/v1/finance/transactions/" + txId + "/convert-to-emi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emiGroupId").value(emiGroupId.toString()))
                .andExpect(jsonPath("$.originalTransactionId").value(txId.toString()))
                .andExpect(jsonPath("$.tenureMonths").value(2))
                .andExpect(jsonPath("$.monthlyInstallment").value(5000.00))
                .andExpect(jsonPath("$.installments.length()").value(2));
    }
}
