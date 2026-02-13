package com.himanshu.portfolio_risk_analytics.controller;

import com.himanshu.portfolio_risk_analytics.dto.ApiResponse;
import com.himanshu.portfolio_risk_analytics.dto.CreateHoldingRequest;
import com.himanshu.portfolio_risk_analytics.dto.UpdateHoldingRequest;
import com.himanshu.portfolio_risk_analytics.dto.HoldingDto;
import com.himanshu.portfolio_risk_analytics.service.HoldingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

@RestController
@RequestMapping("/v1/holdings")
@CrossOrigin(origins = "*")
public class HoldingController {
    private static final Logger logger = Logger.getLogger(HoldingController.class.getName());
    private final HoldingService holdingService;

    public HoldingController(HoldingService holdingService) {
        this.holdingService = holdingService;
    }

    @PostMapping("/{portfolioId}")
    public ResponseEntity<ApiResponse<HoldingDto>> addHolding(
            @PathVariable String portfolioId,
            @Valid @RequestBody CreateHoldingRequest request,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            HoldingDto holding = holdingService.addHolding(portfolioId, userId, request);
            logger.log(Level.INFO, "Holding added: " + holding.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(holding, "Holding added successfully"));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to add holding: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), "HOLDING_ADD_FAILED"));
        }
    }

    @GetMapping("/{portfolioId}")
    public ResponseEntity<ApiResponse<List<HoldingDto>>> getPortfolioHoldings(
            @PathVariable String portfolioId,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            List<HoldingDto> holdings = holdingService.getPortfolioHoldings(portfolioId, userId);
            return ResponseEntity.ok(ApiResponse.success(holdings));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to retrieve holdings: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Portfolio not found", "NOT_FOUND"));
        }
    }

    @PutMapping("/{portfolioId}/{holdingId}")
    public ResponseEntity<ApiResponse<HoldingDto>> updateHolding(
            @PathVariable String portfolioId,
            @PathVariable String holdingId,
            @Valid @RequestBody UpdateHoldingRequest request,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            HoldingDto holding = holdingService.updateHolding(holdingId, portfolioId, userId, request);
            return ResponseEntity.ok(ApiResponse.success(holding, "Holding updated"));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to update holding: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage(), "UPDATE_FAILED"));
        }
    }

    @DeleteMapping("/{portfolioId}/{holdingId}")
    public ResponseEntity<ApiResponse<Void>> deleteHolding(
            @PathVariable String portfolioId,
            @PathVariable String holdingId,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            holdingService.deleteHolding(holdingId, portfolioId, userId);
            logger.log(Level.INFO, "Holding deleted: " + holdingId);
            return ResponseEntity.ok(ApiResponse.success(null, "Holding deleted"));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to delete holding: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage(), "DELETE_FAILED"));
        }
    }

    @PostMapping("/{portfolioId}/refresh-prices")
    public ResponseEntity<ApiResponse<Void>> refreshPrices(
            @PathVariable String portfolioId,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            holdingService.refreshHoldingPrices(portfolioId);
            return ResponseEntity.ok(ApiResponse.success(null, "Prices refreshed"));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to refresh prices: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage(), "REFRESH_FAILED"));
        }
    }
}