package com.himanshu.portfolio_risk_analytics.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "portfolios")
public class Portfolio {
    @Id
    private String id;

    @NotBlank(message = "Portfolio name cannot be blank")
    private String name;

    @Indexed
    @NotBlank(message = "User ID cannot be blank")
    private String userId;

    private String description;

    @Builder.Default
    private List<String> holdingIds = List.of();

    // Portfolio statistics (cached)
    private Double portfolioValue; // Total value in base currency
    private Double totalVolatility;
    private Double expectedReturn;
    private String currency = "USD";

    @Builder.Default
    private boolean isActive = true;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime lastAnalyzedAt = LocalDateTime.now();

    public void validate() {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null or empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Portfolio name cannot be null or empty");
        }
    }
}