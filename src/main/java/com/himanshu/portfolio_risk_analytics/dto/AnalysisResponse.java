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
public class AnalysisResponse {
    private String portfolioId;
    private RiskMetricsDto riskMetrics;
    private String aiInsight;
    private String recommendedActions;
    private LocalDateTime analyzedAt;
    private String analysisType;
}