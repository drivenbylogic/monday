package com.monday.app.vehicle.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateServiceRecordRequest(
        @JsonProperty("serviceDate") @JsonAlias({"date", "service_date"}) LocalDate serviceDate,
        @JsonProperty("kmAtService") @JsonAlias({"odometerKm", "odometer_km", "km_at_service", "odometer", "km"}) Integer kmAtService,
        @JsonProperty("serviceType") @JsonAlias({"title", "service_type", "type"}) String serviceType,
        @JsonProperty("description") @JsonAlias({"details", "detail"}) String description,
        @JsonProperty("cost") @JsonAlias({"totalCost", "total_cost", "amount", "total"}) BigDecimal cost,
        @JsonProperty("partsCost") @JsonAlias({"parts_cost"}) BigDecimal partsCost,
        @JsonProperty("laborCost") @JsonAlias({"labor_cost"}) BigDecimal laborCost,
        @JsonProperty("performedBy") @JsonAlias({"serviceProvider", "service_provider", "performed_by", "workshop", "vendor"}) String performedBy,
        @JsonProperty("notes") @JsonAlias({"generalNotes", "general_notes"}) String notes,
        @JsonProperty("verified") Boolean verified,
        @JsonProperty("isUpcoming") @JsonAlias({"upcoming", "is_upcoming"}) Boolean isUpcoming,
        @JsonProperty("syncToFinance") @JsonAlias({"recordFinanceTransaction", "sync_to_finance", "record_finance_transaction"}) Boolean syncToFinance,
        @JsonProperty("linkedScheduleId") @JsonAlias({"scheduleId", "linked_schedule_id", "schedule_id"}) String linkedScheduleId,
        @JsonProperty("items") List<CreateServiceItemRequest> items
) {
    public UpdateServiceRecordRequest(
            LocalDate serviceDate,
            Integer kmAtService,
            String serviceType,
            String description,
            BigDecimal cost,
            BigDecimal partsCost,
            BigDecimal laborCost,
            String performedBy,
            String notes,
            Boolean verified,
            Boolean isUpcoming,
            List<CreateServiceItemRequest> items
    ) {
        this(serviceDate, kmAtService, serviceType, description, cost, partsCost, laborCost,
                performedBy, notes, verified, isUpcoming, null, null, items);
    }

    public UUID getParsedLinkedScheduleId() {
        if (linkedScheduleId == null || linkedScheduleId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(linkedScheduleId.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
