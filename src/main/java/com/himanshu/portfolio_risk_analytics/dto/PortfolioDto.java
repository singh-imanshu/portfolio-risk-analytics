package com.himanshu.portfolio_risk_analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioDto {
    private String id;
    private String name;
    private String description;
    private String userId;
    private List<HoldingDto> holdings;
    private Double portfolioValue;
    private Double totalVolatility;
    private Double expectedReturn;
    private String currency;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastAnalyzedAt;
}