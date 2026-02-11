package com.himanshu.portfolio_risk_analytics.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@Document(collection = "portfolios")
public class Portfolio {
    @Id
    private String id;

    private String userId;

    // Map of Ticker -> Array of daily returns (e.g., "RELIANCE" -> [0.02, -0.01, ...])
    // Each value represents the percentage change for that trading day
    private Map<String, double[]> historicalReturns;

    public Portfolio(String userId, Map<String, double[]> historicalReturns) {
        this.userId = userId;
        this.historicalReturns = historicalReturns;
    }

    public void validate() {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null or empty");
        }
        if (historicalReturns == null || historicalReturns.isEmpty()) {
            throw new IllegalArgumentException("historicalReturns cannot be null or empty");
        }
        for (Map.Entry<String, double[]> entry : historicalReturns.entrySet()) {
            if (entry.getValue() == null || entry.getValue().length == 0) {
                throw new IllegalArgumentException("All stocks must have return data");
            }
        }
    }
}