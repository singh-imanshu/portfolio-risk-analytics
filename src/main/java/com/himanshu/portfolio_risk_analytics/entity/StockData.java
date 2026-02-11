package com.himanshu.portfolio_risk_analytics.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "stock_data")
@CompoundIndexes({
        @CompoundIndex(name = "ticker_market_idx", def = "{'ticker': 1, 'market': 1}"),
        @CompoundIndex(name = "ticker_date_idx", def = "{'ticker': 1, 'dataDate': -1}")
})
public class StockData {
    @Id
    private String id;

    @Indexed
    @NotBlank(message = "Ticker cannot be blank")
    private String ticker;

    @Indexed
    private String market; // "US", "INDIA", etc.

    // Historical daily returns (date -> return percentage)
    // Format: "2024-01-15" -> 0.025 (2.5% return)
    private Map<String, Double> dailyReturns;

    // Latest price data
    @Positive(message = "Current price must be positive")
    private Double currentPrice;

    @Positive(message = "Open price must be positive")
    private Double openPrice;

    @Positive(message = "High price must be positive")
    private Double highPrice;

    @Positive(message = "Low price must be positive")
    private Double lowPrice;

    @Positive(message = "Volume must be positive")
    private Long volume;

    @Builder.Default
    private LocalDateTime dataDate = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime lastUpdatedAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime nextRefreshAt = LocalDateTime.now().plusDays(1);

    private String source = "ALPHA_VANTAGE"; // Data provider

    private boolean isStale;

    public void validate() {
        if (ticker == null || ticker.trim().isEmpty()) {
            throw new IllegalArgumentException("Ticker cannot be null or empty");
        }
        if (dailyReturns == null || dailyReturns.isEmpty()) {
            throw new IllegalArgumentException("Daily returns cannot be null or empty");
        }
    }

    public boolean needsRefresh() {
        return LocalDateTime.now().isAfter(nextRefreshAt);
    }
}