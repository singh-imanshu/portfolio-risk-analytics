package com.himanshu.portfolio_risk_analytics.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePortfolioRequest {
    private String name;
    private String description;
    private String currency;
}