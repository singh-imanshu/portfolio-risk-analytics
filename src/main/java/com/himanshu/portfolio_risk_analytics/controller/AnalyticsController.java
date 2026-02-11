package com.himanshu.portfolio_risk_analytics.controller;

import com.himanshu.portfolio_risk_analytics.service.AlphaVantageService;
import com.himanshu.portfolio_risk_analytics.service.RiskService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private static final Logger logger = Logger.getLogger(AnalyticsController.class.getName());
    private static final int API_DELAY_MS = 13000; // 13 seconds to stay under 5 calls/minute

    private final RiskService riskService;
    private final AlphaVantageService alphaVantageService;
    private final ChatClient chatClient;

    public AnalyticsController(RiskService riskService,
                               AlphaVantageService alphaVantageService,
                               ChatClient.Builder builder) {
        this.riskService = riskService;
        this.alphaVantageService = alphaVantageService;
        this.chatClient = builder.build();
    }

    @PostMapping("/report")
    public ResponseEntity<Map<String, Object>> getFullReport(@RequestBody List<String> tickers) {
        // Validate input
        if (tickers == null || tickers.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tickers list cannot be empty"));
        }

        try {
            Map<String, double[]> returnsMap = new HashMap<>();

            // Fetch data with rate limiting
            for (int i = 0; i < tickers.size(); i++) {
                String ticker = tickers.get(i);
                logger.info("Fetching data for ticker: " + ticker);

                returnsMap.put(ticker, alphaVantageService.getDailyReturns(ticker));

                // Add delay between requests (except after the last one)
                if (i < tickers.size() - 1) {
                    Thread.sleep(API_DELAY_MS);
                }
            }

            // Equal weight portfolio
            double[] weights = new double[tickers.size()];
            Arrays.fill(weights, 1.0 / tickers.size());

            // Calculate risk metrics
            Map<String, Object> metrics = riskService.calculateRisk(returnsMap, weights);

            // Get AI insights
            String prompt = "Analyze these portfolio statistics and provide investment insights: " + metrics.toString();
            String aiInsight = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            // Build response
            Map<String, Object> response = new HashMap<>(metrics);
            response.put("ai_insight", aiInsight);

            return ResponseEntity.ok(response);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning("Request interrupted: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Request was interrupted"));
        } catch (Exception e) {
            logger.severe("Error generating report: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Failed to generate report: " + e.getMessage()));
        }
    }
}