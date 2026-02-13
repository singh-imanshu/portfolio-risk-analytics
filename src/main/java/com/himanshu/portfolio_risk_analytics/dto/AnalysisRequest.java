package com.himanshu.portfolio_risk_analytics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * FIXED: Added proper input validation constraints
 * - Limits number of tickers (prevents memory explosion)
 * - Validates weights sum to 1.0
 * - Prevents abuse
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisRequest {

    /**
     * List of stock tickers to analyze.
     * FIXED: Limited to max 20 tickers (was unlimited)
     * Min 1, Max 20 to prevent DoS via memory exhaustion
     */
    @NotNull(message = "Tickers list cannot be null")
    @Size(min = 1, max = 20, message = "Must provide between 1 and 20 tickers")
    private List<String> tickers;

    /**
     * Portfolio weights (must sum to 1.0).
     * FIXED: Added validation
     */
    @Size(min = 1, max = 20, message = "Weights must match tickers count (1-20)")
    private List<Double> weights;

    /**
     * Type of analysis: QUICK, STANDARD, COMPREHENSIVE
     */
    @NotBlank(message = "Analysis type cannot be blank")
    private String analysisType;

    /**
     * Validate that weights sum to approximately 1.0 (with 0.01 tolerance)
     */
    public void validateWeights() {
        if (weights != null && !weights.isEmpty()) {
            if (tickers.size() != weights.size()) {
                throw new IllegalArgumentException(
                        "Number of weights must match number of tickers. " +
                                "Got " + weights.size() + " weights for " + tickers.size() + " tickers"
                );
            }

            double sum = weights.stream().mapToDouble(Double::doubleValue).sum();
            if (Math.abs(sum - 1.0) > 0.01) {
                throw new IllegalArgumentException(
                        "Weights must sum to 1.0 (±0.01 tolerance). Got sum: " + sum
                );
            }

            // Validate each weight is positive
            for (Double weight : weights) {
                if (weight <= 0 || weight > 1.0) {
                    throw new IllegalArgumentException(
                            "Each weight must be between 0 (exclusive) and 1.0 (inclusive). Got: " + weight
                    );
                }
            }
        }
    }

    /**
     * Validate that all tickers are non-empty strings
     */
    public void validateTickers() {
        if (tickers != null) {
            for (String ticker : tickers) {
                if (ticker == null || ticker.trim().isEmpty()) {
                    throw new IllegalArgumentException("Ticker cannot be null or empty");
                }

                if (!ticker.matches("^[A-Z0-9\\.]{1,10}$")) {
                    throw new IllegalArgumentException(
                            "Invalid ticker format: " + ticker +
                                    ". Expected 1-10 alphanumeric characters (may include dot for market suffix)"
                    );
                }
            }
        }
    }
}