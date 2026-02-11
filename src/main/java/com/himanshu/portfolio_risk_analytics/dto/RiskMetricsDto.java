package com.himanshu.portfolio_risk_analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskMetricsDto {
    private String[] tickers;
    private double variance;
    private double volatility;
    private double expectedReturn;
    private double sharpeRatio;
    private double beta;
    private Map<String, Double> assetVolatilities;
    private double[][] correlationMatrix;
    private String correlationMatrixAsString;
    private Map<String, Object> riskBreakdown;
}