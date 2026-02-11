package com.himanshu.portfolio_risk_analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisRequest {
    private List<String> tickers;
    private List<Double> weights;
    private String analysisType = "COMPREHENSIVE"; // QUICK, STANDARD, COMPREHENSIVE
}