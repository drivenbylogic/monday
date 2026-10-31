package com.monday.app.finance.dto;

import com.monday.app.finance.entity.Transaction;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record EmiConversionResult(
        UUID emiGroupId,
        UUID originalTransactionId,
        int tenureMonths,
        BigDecimal principalAmount,
        BigDecimal monthlyInstallment,
        BigDecimal totalPayable,
        BigDecimal totalInterest,
        BigDecimal processingFee,
        List<Transaction> installments
) {}

