package com.himanshu.portfolio_risk_analytics.controller;

import com.himanshu.portfolio_risk_analytics.dto.ApiResponse;
import com.himanshu.portfolio_risk_analytics.dto.CreatePortfolioRequest;
import com.himanshu.portfolio_risk_analytics.dto.UpdatePortfolioRequest;
import com.himanshu.portfolio_risk_analytics.dto.PortfolioDto;
import com.himanshu.portfolio_risk_analytics.service.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

@RestController
@RequestMapping("/api/v1/portfolios")
@CrossOrigin(origins = "*")
public class PortfolioController {
    private static final Logger logger = Logger.getLogger(PortfolioController.class.getName());
    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PortfolioDto>> createPortfolio(
            @Valid @RequestBody CreatePortfolioRequest request,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            PortfolioDto portfolio = portfolioService.createPortfolio(userId, request);
            logger.log(Level.INFO, "Portfolio created: " + portfolio.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(portfolio, "Portfolio created successfully"));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create portfolio: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), "PORTFOLIO_CREATION_FAILED"));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PortfolioDto>>> getUserPortfolios(Authentication authentication) {
        try {
            String userId = authentication.getName();
            List<PortfolioDto> portfolios = portfolioService.getUserPortfolios(userId);
            return ResponseEntity.ok(ApiResponse.success(portfolios, "Portfolios retrieved"));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to retrieve portfolios: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage(), "RETRIEVE_FAILED"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PortfolioDto>> getPortfolio(
            @PathVariable String id,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            PortfolioDto portfolio = portfolioService.getPortfolio(id, userId);
            return ResponseEntity.ok(ApiResponse.success(portfolio));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Portfolio not found: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Portfolio not found", "NOT_FOUND"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PortfolioDto>> updatePortfolio(
            @PathVariable String id,
            @Valid @RequestBody UpdatePortfolioRequest request,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            PortfolioDto portfolio = portfolioService.updatePortfolio(id, userId, request);
            return ResponseEntity.ok(ApiResponse.success(portfolio, "Portfolio updated"));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to update portfolio: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage(), "UPDATE_FAILED"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePortfolio(
            @PathVariable String id,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            portfolioService.deletePortfolio(id, userId);
            logger.log(Level.INFO, "Portfolio deleted: " + id);
            return ResponseEntity.ok(ApiResponse.success(null, "Portfolio deleted"));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to delete portfolio: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage(), "DELETE_FAILED"));
        }
    }
}