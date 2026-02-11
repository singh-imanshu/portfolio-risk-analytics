package com.himanshu.portfolio_risk_analytics.service;

import com.himanshu.portfolio_risk_analytics.dto.CreateHoldingRequest;
import com.himanshu.portfolio_risk_analytics.dto.UpdateHoldingRequest;
import com.himanshu.portfolio_risk_analytics.dto.HoldingDto;
import com.himanshu.portfolio_risk_analytics.entity.Portfolio;
import com.himanshu.portfolio_risk_analytics.entity.Holding;
import com.himanshu.portfolio_risk_analytics.repository.PortfolioRepository;
import com.himanshu.portfolio_risk_analytics.repository.HoldingRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import java.util.logging.Level;

@Service
public class HoldingService {
    private static final Logger logger = Logger.getLogger(HoldingService.class.getName());
    private final HoldingRepository holdingRepository;
    private final PortfolioRepository portfolioRepository;
    private final StockDataService stockDataService;

    public HoldingService(HoldingRepository holdingRepository,
                          PortfolioRepository portfolioRepository,
                          StockDataService stockDataService) {
        this.holdingRepository = holdingRepository;
        this.portfolioRepository = portfolioRepository;
        this.stockDataService = stockDataService;
    }

    public HoldingDto addHolding(String portfolioId, String userId, CreateHoldingRequest request) {
        // Verify portfolio exists and belongs to user
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        // Check if holding already exists
        if (holdingRepository.existsByPortfolioIdAndTicker(portfolioId, request.getTicker())) {
            throw new RuntimeException("Holding for ticker " + request.getTicker() + " already exists");
        }

        Holding holding = Holding.builder()
                .portfolioId(portfolioId)
                .ticker(request.getTicker().toUpperCase())
                .market(request.getMarket())
                .quantity(request.getQuantity())
                .purchasePrice(request.getPurchasePrice())
                .notes(request.getNotes())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        holding.validate();

        // Fetch current price from stock data service
        try {
            Double currentPrice = stockDataService.getCurrentPrice(request.getTicker(), request.getMarket());
            holding.setCurrentPrice(currentPrice);
            holding.recalculateMetrics();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to fetch price for " + request.getTicker());
        }

        Holding saved = holdingRepository.save(holding);
        logger.log(Level.INFO, "Holding added: " + saved.getId());
        return mapToDto(saved);
    }

    public HoldingDto updateHolding(String holdingId, String portfolioId, String userId, UpdateHoldingRequest request) {
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        Holding holding = holdingRepository.findByIdAndPortfolioId(holdingId, portfolioId)
                .orElseThrow(() -> new RuntimeException("Holding not found"));

        if (request.getQuantity() != null) {
            holding.setQuantity(request.getQuantity());
        }
        if (request.getPurchasePrice() != null) {
            holding.setPurchasePrice(request.getPurchasePrice());
        }
        if (request.getWeight() != null) {
            holding.setWeight(request.getWeight());
        }
        if (request.getNotes() != null) {
            holding.setNotes(request.getNotes());
        }

        holding.setUpdatedAt(LocalDateTime.now());
        holding.recalculateMetrics();

        Holding saved = holdingRepository.save(holding);
        logger.log(Level.INFO, "Holding updated: " + saved.getId());
        return mapToDto(saved);
    }

    public void deleteHolding(String holdingId, String portfolioId, String userId) {
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        Holding holding = holdingRepository.findByIdAndPortfolioId(holdingId, portfolioId)
                .orElseThrow(() -> new RuntimeException("Holding not found"));

        holdingRepository.delete(holding);
        logger.log(Level.INFO, "Holding deleted: " + holdingId);
    }

    public List<HoldingDto> getPortfolioHoldings(String portfolioId, String userId) {
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        List<Holding> holdings = holdingRepository.findByPortfolioId(portfolioId);
        return holdings.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public void refreshHoldingPrices(String portfolioId) {
        List<Holding> holdings = holdingRepository.findByPortfolioId(portfolioId);
        for (Holding holding : holdings) {
            try {
                Double currentPrice = stockDataService.getCurrentPrice(holding.getTicker(), holding.getMarket());
                holding.setCurrentPrice(currentPrice);
                holding.recalculateMetrics();
                holdingRepository.save(holding);
                logger.log(Level.INFO, "Price refreshed for " + holding.getTicker());
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to refresh price for " + holding.getTicker());
            }
        }
    }

    private HoldingDto mapToDto(Holding holding) {
        return HoldingDto.builder()
                .id(holding.getId())
                .portfolioId(holding.getPortfolioId())
                .ticker(holding.getTicker())
                .market(holding.getMarket())
                .quantity(holding.getQuantity())
                .purchasePrice(holding.getPurchasePrice())
                .currentPrice(holding.getCurrentPrice())
                .weight(holding.getWeight())
                .currentValue(holding.getCurrentValue())
                .gainLoss(holding.getGainLoss())
                .gainLossPercent(holding.getGainLossPercent())
                .notes(holding.getNotes())
                .createdAt(holding.getCreatedAt())
                .updatedAt(holding.getUpdatedAt())
                .lastPricedAt(holding.getLastPricedAt())
                .build();
    }
}