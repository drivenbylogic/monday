package com.monday.app.agenda.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CommitmentSummaryDto(
        UUID id,
        String name,
        LocalDate dueDate,
        BigDecimal amount,
        UUID accountId,
        String accountName,
        boolean isUrgent,
        String status
) {}
