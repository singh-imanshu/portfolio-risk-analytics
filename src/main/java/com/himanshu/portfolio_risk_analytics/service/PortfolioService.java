package com.himanshu.portfolio_risk_analytics.service;

import com.himanshu.portfolio_risk_analytics.dto.CreatePortfolioRequest;
import com.himanshu.portfolio_risk_analytics.dto.UpdatePortfolioRequest;
import com.himanshu.portfolio_risk_analytics.dto.PortfolioDto;
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
public class PortfolioService {
    private static final Logger logger = Logger.getLogger(PortfolioService.class.getName());
    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;

    public PortfolioService(PortfolioRepository portfolioRepository,
                            HoldingRepository holdingRepository) {
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
    }

    public PortfolioDto createPortfolio(String userId, CreatePortfolioRequest request) {
        Portfolio portfolio = Portfolio.builder()
                .name(request.getName())
                .description(request.getDescription())
                .userId(userId)
                .currency(request.getCurrency())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        portfolio.validate();
        Portfolio saved = portfolioRepository.save(portfolio);
        logger.log(Level.INFO, "Portfolio created: " + saved.getId());
        return mapToDto(saved);
    }

    public PortfolioDto getPortfolio(String portfolioId, String userId) {
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        List<Holding> holdings = holdingRepository.findByPortfolioId(portfolioId);
        return mapToDtoWithHoldings(portfolio, holdings);
    }

    public List<PortfolioDto> getUserPortfolios(String userId) {
        List<Portfolio> portfolios = portfolioRepository.findByUserId(userId);
        return portfolios.stream()
                .map(p -> {
                    List<Holding> holdings = holdingRepository.findByPortfolioId(p.getId());
                    return mapToDtoWithHoldings(p, holdings);
                })
                .collect(Collectors.toList());
    }

    public PortfolioDto updatePortfolio(String portfolioId, String userId, UpdatePortfolioRequest request) {
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        if (request.getName() != null) {
            portfolio.setName(request.getName());
        }
        if (request.getDescription() != null) {
            portfolio.setDescription(request.getDescription());
        }
        if (request.getCurrency() != null) {
            portfolio.setCurrency(request.getCurrency());
        }

        portfolio.setUpdatedAt(LocalDateTime.now());
        Portfolio saved = portfolioRepository.save(portfolio);
        logger.log(Level.INFO, "Portfolio updated: " + saved.getId());
        return mapToDto(saved);
    }

    public void deletePortfolio(String portfolioId, String userId) {
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        // Delete all holdings
        List<Holding> holdings = holdingRepository.findByPortfolioId(portfolioId);
        holdingRepository.deleteAll(holdings);

        // Delete portfolio
        portfolioRepository.delete(portfolio);
        logger.log(Level.INFO, "Portfolio deleted: " + portfolioId);
    }

    private PortfolioDto mapToDto(Portfolio portfolio) {
        return PortfolioDto.builder()
                .id(portfolio.getId())
                .name(portfolio.getName())
                .description(portfolio.getDescription())
                .userId(portfolio.getUserId())
                .portfolioValue(portfolio.getPortfolioValue())
                .totalVolatility(portfolio.getTotalVolatility())
                .expectedReturn(portfolio.getExpectedReturn())
                .currency(portfolio.getCurrency())
                .isActive(portfolio.isActive())
                .createdAt(portfolio.getCreatedAt())
                .updatedAt(portfolio.getUpdatedAt())
                .lastAnalyzedAt(portfolio.getLastAnalyzedAt())
                .build();
    }

    private PortfolioDto mapToDtoWithHoldings(Portfolio portfolio, List<Holding> holdings) {
        List<HoldingDto> holdingDtos = holdings.stream()
                .map(this::holdingToDto)
                .collect(Collectors.toList());

        return PortfolioDto.builder()
                .id(portfolio.getId())
                .name(portfolio.getName())
                .description(portfolio.getDescription())
                .userId(portfolio.getUserId())
                .holdings(holdingDtos)
                .portfolioValue(portfolio.getPortfolioValue())
                .totalVolatility(portfolio.getTotalVolatility())
                .expectedReturn(portfolio.getExpectedReturn())
                .currency(portfolio.getCurrency())
                .isActive(portfolio.isActive())
                .createdAt(portfolio.getCreatedAt())
                .updatedAt(portfolio.getUpdatedAt())
                .lastAnalyzedAt(portfolio.getLastAnalyzedAt())
                .build();
    }

    private HoldingDto holdingToDto(Holding holding) {
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