package com.himanshu.portfolio_risk_analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

/**
 * Data Transfer Object for comprehensive portfolio risk analytics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskMetricsDto {
    private String[] tickers;

    // Core Risk & Return Metrics
    private double variance;
    private double volatility; // Annualized standard deviation
    private double expectedReturn; // Annualized expected log return

    // Performance Ratios
    private double sharpeRatio; // Risk-adjusted return
    private double sortinoRatio; // Downside-adjusted return

    // Risk Exposure Metrics
    private double beta; // Systemic risk sensitivity relative to benchmark
    private double valueAtRisk95; // Parametric VaR at 95% confidence

    // Statistical Breakdowns
    private Map<String, Double> assetVolatilities;
    private double[][] correlationMatrix;
    private String correlationMatrixAsString;
    private Map<String, Object> riskBreakdown;
}