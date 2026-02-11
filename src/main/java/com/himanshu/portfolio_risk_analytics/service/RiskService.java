package com.himanshu.portfolio_risk_analytics.service;

import com.himanshu.portfolio_risk_analytics.dto.RiskMetricsDto;
import com.himanshu.portfolio_risk_analytics.entity.StockData;
import com.himanshu.portfolio_risk_analytics.repository.StockDataRepository;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.Covariance;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

@Service
public class RiskService {
    private static final Logger logger = Logger.getLogger(RiskService.class.getName());
    private static final double RISK_FREE_RATE = 0.02; // 2% annual
    private final StockDataRepository stockDataRepository;

    public RiskService(StockDataRepository stockDataRepository) {
        this.stockDataRepository = stockDataRepository;
    }

    public RiskMetricsDto calculateRiskMetrics(List<String> tickers, double[] weights) {
        if (tickers == null || tickers.isEmpty()) {
            throw new IllegalArgumentException("Tickers list cannot be empty");
        }
        if (weights == null || weights.length == 0) {
            throw new IllegalArgumentException("Weights array cannot be empty");
        }
        if (tickers.size() != weights.length) {
            throw new IllegalArgumentException("Number of weights must match number of tickers");
        }

        // Validate weights sum to 1
        double weightSum = Arrays.stream(weights).sum();
        if (Math.abs(weightSum - 1.0) > 0.01) {
            throw new IllegalArgumentException("Weights must sum to 1.0");
        }

        logger.log(Level.INFO, "Calculating risk metrics for " + tickers.size() + " stocks");

        // Fetch returns data for all tickers
        Map<String, double[]> returnsMap = new HashMap<>();
        for (String ticker : tickers) {
            StockData stockData = stockDataRepository.findByTickerAndMarket(ticker, "US")
                    .orElseThrow(() -> new RuntimeException("No data found for ticker: " + ticker));

            double[] returns = stockData.getDailyReturns().values().stream()
                    .mapToDouble(Double::doubleValue)
                    .toArray();

            if (returns.length < 10) {
                throw new RuntimeException("Insufficient data for " + ticker +
                        ": " + returns.length + " days (need at least 10)");
            }

            returnsMap.put(ticker, returns);
        }

        // Build returns matrix
        String[] tickerArray = tickers.toArray(new String[0]);
        int numStocks = tickerArray.length;

        // Find minimum number of data points across all stocks
        int numDays = returnsMap.values().stream()
                .mapToInt(arr -> arr.length)
                .min()
                .orElse(100);

        logger.log(Level.INFO, "Using " + numDays + " days of data for " + numStocks + " stocks");

        // Build matrix with aligned data
        double[][] data = new double[numDays][numStocks];
        for (int i = 0; i < numStocks; i++) {
            double[] stockRet = returnsMap.get(tickerArray[i]);
            for (int j = 0; j < numDays; j++) {
                data[j][i] = stockRet[j];
            }
        }

        RealMatrix matrix = new Array2DRowRealMatrix(data);

        // Calculate individual asset volatilities
        Map<String, Double> assetVolatilities = new HashMap<>();
        double[][] correlation = null;
        RealMatrix covMatrix = null;

        try {
            // Calculate covariance matrix
            covMatrix = new Covariance(matrix).getCovarianceMatrix();

            // Calculate individual volatilities from covariance diagonal
            for (int i = 0; i < numStocks; i++) {
                double variance = covMatrix.getEntry(i, i);
                double volatility = Math.sqrt(variance) * Math.sqrt(252); // Annualized
                assetVolatilities.put(tickerArray[i], volatility);
                logger.log(Level.INFO, tickerArray[i] + " volatility: " + String.format("%.2f%%", volatility * 100));
            }

            // Calculate correlation matrix only if we have multiple stocks
            if (numStocks > 1) {
                try {
                    correlation = new PearsonsCorrelation(matrix).getCorrelationMatrix().getData();
                    logger.log(Level.INFO, "Correlation matrix calculated successfully");
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Could not calculate correlation matrix: " + e.getMessage());
                    // Create identity correlation matrix as fallback
                    correlation = createIdentityCorrelation(numStocks);
                }
            } else {
                // Single stock: correlation is 1.0
                correlation = new double[][]{{1.0}};
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error calculating covariance/correlation: " + e.getMessage());
            throw new RuntimeException("Failed to calculate risk metrics: " + e.getMessage(), e);
        }

        // Calculate portfolio variance: w^T * Cov * w
        RealVector w = new ArrayRealVector(weights);
        double variance = w.dotProduct(covMatrix.operate(w));
        double volatility = Math.sqrt(variance) * Math.sqrt(252); // Annualized

        // Calculate expected returns
        double expectedReturn = calculateExpectedReturn(data, weights);
        double sharpeRatio = calculateSharpeRatio(expectedReturn, volatility);
        double beta = calculateBeta(data, weights, numStocks);

        logger.log(Level.INFO, "Risk metrics calculated:");
        logger.log(Level.INFO, "  Volatility: " + String.format("%.2f%%", volatility * 100));
        logger.log(Level.INFO, "  Expected Return: " + String.format("%.2f%%", expectedReturn * 100));
        logger.log(Level.INFO, "  Sharpe Ratio: " + String.format("%.2f", sharpeRatio));
        logger.log(Level.INFO, "  Beta: " + String.format("%.2f", beta));

        return RiskMetricsDto.builder()
                .tickers(tickerArray)
                .variance(variance)
                .volatility(volatility)
                .expectedReturn(expectedReturn)
                .sharpeRatio(sharpeRatio)
                .beta(beta)
                .assetVolatilities(assetVolatilities)
                .correlationMatrix(correlation)
                .correlationMatrixAsString(formatMatrix(correlation))
                .build();
    }

    private double calculateExpectedReturn(double[][] data, double[] weights) {
        double[] meanReturns = new double[data[0].length];

        // Calculate mean return for each stock
        for (int i = 0; i < data[0].length; i++) {
            double sum = 0;
            for (int j = 0; j < data.length; j++) {
                sum += data[j][i];
            }
            meanReturns[i] = sum / data.length;
        }

        // Calculate weighted expected return
        double expectedReturn = 0;
        for (int i = 0; i < weights.length; i++) {
            expectedReturn += weights[i] * meanReturns[i];
        }

        return expectedReturn * 252; // Annualized (252 trading days)
    }

    private double calculateSharpeRatio(double expectedReturn, double volatility) {
        if (volatility == 0) {
            return 0;
        }
        return (expectedReturn - RISK_FREE_RATE) / volatility;
    }

    private double calculateBeta(double[][] data, double[] weights, int numStocks) {
        // For single stock: beta = 1.0 (by definition, it moves with itself)
        if (numStocks == 1) {
            return 1.0;
        }

        try {
            RealMatrix matrix = new Array2DRowRealMatrix(data);
            RealMatrix covMatrix = new Covariance(matrix).getCovarianceMatrix();

            // Use first asset as market proxy (market index)
            double marketVariance = covMatrix.getEntry(0, 0);

            if (marketVariance <= 0) {
                logger.log(Level.WARNING, "Market variance is zero or negative, returning beta = 1.0");
                return 1.0;
            }

            double portfolioMarketCov = 0;
            for (int i = 0; i < weights.length; i++) {
                portfolioMarketCov += weights[i] * covMatrix.getEntry(i, 0);
            }

            return portfolioMarketCov / marketVariance;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not calculate beta: " + e.getMessage());
            return 1.0; // Return neutral beta on error
        }
    }

    private double[][] createIdentityCorrelation(int size) {
        double[][] identity = new double[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                identity[i][j] = (i == j) ? 1.0 : 0.0;
            }
        }
        return identity;
    }

    private String formatMatrix(double[][] matrix) {
        if (matrix == null || matrix.length == 0) {
            return "No correlation data";
        }

        StringBuilder sb = new StringBuilder();
        for (double[] row : matrix) {
            for (double val : row) {
                sb.append(String.format("%.4f ", val));
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}