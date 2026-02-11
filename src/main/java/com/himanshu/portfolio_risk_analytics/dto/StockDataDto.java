package com.himanshu.portfolio_risk_analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockDataDto {
    private String id;
    private String ticker;
    private String market;
    private Map<String, Double> dailyReturns;
    private Double currentPrice;
    private Double openPrice;
    private Double highPrice;
    private Double lowPrice;
    private Long volume;
    private LocalDateTime dataDate;
    private LocalDateTime lastUpdatedAt;
    private String source;
    private boolean isStale;
}