package com.himanshu.portfolio_risk_analytics.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePortfolioRequest {
    @NotBlank(message = "Portfolio name cannot be blank")
    private String name;
    private String description;
    private String currency = "USD";
}