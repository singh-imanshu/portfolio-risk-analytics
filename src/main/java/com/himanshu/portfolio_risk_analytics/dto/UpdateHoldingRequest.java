package com.himanshu.portfolio_risk_analytics.dto;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateHoldingRequest {
    private Double quantity;
    @Positive(message = "Purchase price must be positive")
    private Double purchasePrice;

    private Double weight;
    private String notes;
}