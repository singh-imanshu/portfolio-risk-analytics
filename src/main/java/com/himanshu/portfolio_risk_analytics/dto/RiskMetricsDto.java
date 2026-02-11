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
    private Double variance;
    private Double volatility;
    private Double expectedReturn;
    private Double sharpeRatio;
    private Double beta;
    private Map<String, Double> assetVolatilities;
    private Double[][] correlationMatrix;
    private String correlationMatrixAsString;
    private Map<String, Object> riskBreakdown;
}