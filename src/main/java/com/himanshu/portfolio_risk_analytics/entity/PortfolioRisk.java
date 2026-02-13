package com.himanshu.portfolio_risk_analytics.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Portfolio Risk Metrics Entity.
 * Contains calculated risk metrics for a portfolio of securities.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioRisk {

    /**
     * List of ticker symbols in the portfolio.
     */
    private List<String> tickers;

    /**
     * Portfolio volatility (standard deviation of returns).
     * Higher values indicate more risk.
     */
    private double volatility;

    /**
     * Value at Risk at 95% confidence level.
     * Worst expected loss with 95% confidence (5% worst case).
     * Expressed as decimal (e.g., -0.05 = 5% loss).
     */
    private double valueAtRisk95;

    /**
     * Conditional VaR at 95% confidence level.
     * Expected loss beyond VaR (average of worst 5% returns).
     * Typically more conservative than VaR.
     */
    private double conditionalVaR95;

    /**
     * Number of securities in the portfolio.
     */
    private int numSecurities;

    /**
     * Timestamp when analysis was performed (milliseconds since epoch).
     */
    private long analysisTimestamp;

    /**
     * Gets a human-readable risk classification.
     *
     * @return Risk level string: LOW, MEDIUM, HIGH, VERY_HIGH
     */
    public String getRiskClassification() {
        if (volatility < 0.10) {
            return "LOW";
        } else if (volatility < 0.20) {
            return "MEDIUM";
        } else if (volatility < 0.30) {
            return "HIGH";
        } else {
            return "VERY_HIGH";
        }
    }
}