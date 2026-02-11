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

        // Fetch returns data for all tickers
        Map<String, double[]> returnsMap = new HashMap<>();
        for (String ticker : tickers) {
            StockData stockData = stockDataRepository.findByTickerAndMarket(ticker, "US")
                    .orElseThrow(() -> new RuntimeException("No data found for ticker: " + ticker));

            double[] returns = stockData.getDailyReturns().values().stream()
                    .mapToDouble(Double::doubleValue)
                    .toArray();
            returnsMap.put(ticker, returns);
        }

        // Build returns matrix
        String[] tickerArray = tickers.toArray(new String[0]);
        int numStocks = tickerArray.length;
        int numDays = returnsMap.get(tickerArray[0]).length;

        double[][] data = new double[numDays][numStocks];
        for (int i = 0; i < numStocks; i++) {
            double[] stockRet = returnsMap.get(tickerArray[i]);
            for (int j = 0; j < numDays; j++) {
                data[j][i] = stockRet[j];
            }
        }

        RealMatrix matrix = new Array2DRowRealMatrix(data);

        // Calculate correlation matrix
        double[][] correlation = new PearsonsCorrelation(matrix).getCorrelationMatrix().getData();

        // Calculate covariance matrix
        RealMatrix covMatrix = new Covariance(matrix).getCovarianceMatrix();

        // Calculate individual asset volatilities
        Map<String, Double> assetVolatilities = new HashMap<>();
        for (int i = 0; i < numStocks; i++) {
            double variance = covMatrix.getEntry(i, i);
            double volatility = Math.sqrt(variance) * Math.sqrt(252); // Annualized
            assetVolatilities.put(tickerArray[i], volatility);
        }

        // Calculate portfolio variance: w^T * Cov * w
        RealVector w = new ArrayRealVector(weights);
        double variance = w.dotProduct(covMatrix.operate(w));
        double volatility = Math.sqrt(variance) * Math.sqrt(252); // Annualized

        // Calculate expected returns
        double expectedReturn = calculateExpectedReturn(data, weights);
        double sharpeRatio = (expectedReturn - RISK_FREE_RATE) / volatility;
        double beta = calculateBeta(data, weights);

        logger.log(Level.INFO, "Risk metrics calculated - Volatility: " + volatility);

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
        for (int i = 0; i < data[0].length; i++) {
            double sum = 0;
            for (int j = 0; j < data.length; j++) {
                sum += data[j][i];
            }
            meanReturns[i] = sum / data.length;
        }

        double expectedReturn = 0;
        for (int i = 0; i < weights.length; i++) {
            expectedReturn += weights[i] * meanReturns[i];
        }
        return expectedReturn * 252; // Annualized
    }

    private double calculateBeta(double[][] data, double[] weights) {
        // Beta = Cov(portfolio, market) / Var(market)
        // For simplicity, treating first asset as proxy for market
        RealMatrix matrix = new Array2DRowRealMatrix(data);
        RealMatrix covMatrix = new Covariance(matrix).getCovarianceMatrix();

        double marketVariance = covMatrix.getEntry(0, 0);
        double portfolioMarketCov = 0;

        for (int i = 0; i < weights.length; i++) {
            portfolioMarketCov += weights[i] * covMatrix.getEntry(i, 0);
        }

        return portfolioMarketCov / marketVariance;
    }

    private String formatMatrix(double[][] matrix) {
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