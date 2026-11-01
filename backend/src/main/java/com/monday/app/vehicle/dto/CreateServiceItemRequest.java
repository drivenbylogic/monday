package com.monday.app.vehicle.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateServiceItemRequest(
        @JsonProperty("itemName") @JsonAlias({"name", "item_name", "title"}) String itemName,
        @JsonProperty("category") String category,
        @JsonProperty("brandSpec") @JsonAlias({"brand", "brand_spec", "specification"}) String brandSpec,
        @JsonProperty("quantity") BigDecimal quantity,
        @JsonProperty("unit") String unit,
        @JsonProperty("itemCost") @JsonAlias({"cost", "price", "item_cost"}) BigDecimal itemCost,
        @JsonProperty("replacementIntervalMonths") @JsonAlias({"intervalMonths", "replacement_interval_months"}) Integer replacementIntervalMonths,
        @JsonProperty("replacementIntervalKm") @JsonAlias({"intervalKm", "replacement_interval_km"}) Integer replacementIntervalKm,
        @JsonProperty("notes") String notes
) {}
