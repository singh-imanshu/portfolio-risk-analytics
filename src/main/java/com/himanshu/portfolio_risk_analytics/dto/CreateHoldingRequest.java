package com.himanshu.portfolio_risk_analytics.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateHoldingRequest {
    @NotBlank(message = "Ticker cannot be blank")
    private String ticker;
    private String market = "US"; // "US", "INDIA"

    @Positive(message = "Quantity must be positive")
    private Double quantity;

    @Positive(message = "Purchase price must be positive")
    private Double purchasePrice;

    private String notes;
}