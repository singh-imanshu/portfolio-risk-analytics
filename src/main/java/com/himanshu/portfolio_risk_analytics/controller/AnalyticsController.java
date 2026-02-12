package com.himanshu.portfolio_risk_analytics.controller;

import com.himanshu.portfolio_risk_analytics.dto.ApiResponse;
import com.himanshu.portfolio_risk_analytics.dto.AnalysisRequest;
import com.himanshu.portfolio_risk_analytics.dto.AnalysisResponse;
import com.himanshu.portfolio_risk_analytics.dto.RiskMetricsDto;
import com.himanshu.portfolio_risk_analytics.service.RiskService;
import com.himanshu.portfolio_risk_analytics.service.StockDataService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.logging.Logger;
import java.util.logging.Level;

@RestController
@RequestMapping("/api/v1/analytics")
@CrossOrigin(origins = "*")

public class AnalyticsController {
    private static final Logger logger = Logger.getLogger(AnalyticsController.class.getName());
    private final RiskService riskService;
    private final StockDataService stockDataService;
    private final ChatClient chatClient;

    public AnalyticsController(RiskService riskService,
                               StockDataService stockDataService,
                               ChatClient.Builder builder) {
        this.riskService = riskService;
        this.stockDataService = stockDataService;
        this.chatClient = builder.build();
    }

    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<AnalysisResponse>> analyzePortfolio(
            @Valid @RequestBody AnalysisRequest request,
            Authentication authentication) {
        try {
            logger.log(Level.INFO, "Portfolio analysis started for: " + request.getTickers());

            // Validate input
            if (request.getTickers() == null || request.getTickers().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Tickers list cannot be empty", "INVALID_INPUT"));
            }

            // Use equal weights if not provided
            double[] weights = request.getWeights() != null && !request.getWeights().isEmpty()
                    ? request.getWeights().stream().mapToDouble(Double::doubleValue).toArray()
                    : createEqualWeights(request.getTickers().size());

            // Calculate risk metrics
            RiskMetricsDto riskMetrics = riskService.calculateRiskMetrics(
                    request.getTickers(), weights);

            // Generate AI insights
            String aiInsight = generateAIInsight(riskMetrics);

            AnalysisResponse response = AnalysisResponse.builder()
                    .riskMetrics(riskMetrics)
                    .aiInsight(aiInsight)
                    .analyzedAt(LocalDateTime.now())
                    .analysisType(request.getAnalysisType())
                    .build();

            logger.log(Level.INFO, "Portfolio analysis completed successfully");
            return ResponseEntity.ok(ApiResponse.success(response, "Analysis complete"));

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Analysis failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Analysis failed: " + e.getMessage(), "ANALYSIS_FAILED"));
        }
    }

    @PostMapping("/quick-analysis")
    public ResponseEntity<ApiResponse<RiskMetricsDto>> quickAnalysis(
            @Valid @RequestBody AnalysisRequest request) {
        try {
            if (request.getTickers() == null || request.getTickers().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Tickers list cannot be empty", "INVALID_INPUT"));
            }

            double[] weights = request.getWeights() != null && !request.getWeights().isEmpty()
                    ? request.getWeights().stream().mapToDouble(Double::doubleValue).toArray()
                    : createEqualWeights(request.getTickers().size());

            RiskMetricsDto metrics = riskService.calculateRiskMetrics(request.getTickers(), weights);
            return ResponseEntity.ok(ApiResponse.success(metrics, "Quick analysis complete"));

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Quick analysis failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage(), "QUICK_ANALYSIS_FAILED"));
        }
    }

    private double[] createEqualWeights(int size) {
        double[] weights = new double[size];
        double weight = 1.0 / size;
        for (int i = 0; i < size; i++) {
            weights[i] = weight;
        }
        return weights;
    }

    private String generateAIInsight(RiskMetricsDto metrics) {
        try {
            String prompt = String.format(
                    "Analyze this portfolio's risk metrics and provide investment insights:\\n" +
                            "Volatility: %.2f%%\\n" +
                            "Sharpe Ratio: %.2f\\n" +
                            "Beta: %.2f\\n" +
                            "Expected Annual Return: %.2f%%\\n" +
                            "Tickers: %s\\n" +
                            "Provide a brief 2-3 sentence investment recommendation based on these metrics.",
                    metrics.getVolatility() * 100,
                    metrics.getSharpeRatio(),
                    metrics.getBeta(),
                    metrics.getExpectedReturn() * 100,
                    String.join(", ", metrics.getTickers())
            );

            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            logger.log(Level.WARNING, "AI insight generation failed: " + e.getMessage());
            return "Unable to generate AI insights at this time. Please review the risk metrics above.";
        }
    }
}