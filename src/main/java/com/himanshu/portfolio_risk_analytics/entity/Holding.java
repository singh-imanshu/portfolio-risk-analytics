package com.himanshu.portfolio_risk_analytics.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "holdings")
public class Holding {
    @Id
    private String id;

    @Indexed
    @NotBlank(message = "Portfolio ID cannot be blank")
    private String portfolioId;

    @NotBlank(message = "Ticker cannot be blank")
    private String ticker;

    private String market = "US"; // "US", "INDIA", "GLOBAL"

    @Positive(message = "Quantity must be positive")
    private Double quantity;

    @Positive(message = "Purchase price must be positive")
    private Double purchasePrice;

    private Double currentPrice;

    @Builder.Default
    @Positive(message = "Weight must be between 0 and 1")
    private Double weight = 0.0;

    private Double currentValue;

    private Double gainLoss;

    private Double gainLossPercent;

    private String notes;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime lastPricedAt = LocalDateTime.now();

    public void validate() {
        if (portfolioId == null || portfolioId.trim().isEmpty()) {
            throw new IllegalArgumentException("Portfolio ID cannot be null or empty");
        }
        if (ticker == null || ticker.trim().isEmpty()) {
            throw new IllegalArgumentException("Ticker cannot be null or empty");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (purchasePrice == null || purchasePrice <= 0) {
            throw new IllegalArgumentException("Purchase price must be positive");
        }
    }

    public void recalculateMetrics() {
        if (currentPrice != null && currentPrice > 0) {
            this.currentValue = quantity * currentPrice;
            this.gainLoss = currentValue - (quantity * purchasePrice);
            this.gainLossPercent = (gainLoss / (quantity * purchasePrice)) * 100;
            this.lastPricedAt = LocalDateTime.now();
        }
    }
}