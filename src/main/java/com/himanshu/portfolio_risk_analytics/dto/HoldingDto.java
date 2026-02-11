package com.himanshu.portfolio_risk_analytics.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HoldingDto {
    private String id;
    private String portfolioId;
    private String ticker;
    private String market;
    private Double quantity;
    private Double purchasePrice;
    private Double currentPrice;
    private Double weight;
    private Double currentValue;
    private Double gainLoss;
    private Double gainLossPercent;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastPricedAt;
}