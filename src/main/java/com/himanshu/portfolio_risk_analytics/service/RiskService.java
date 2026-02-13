package com.himanshu.portfolio_risk_analytics.service;

import com.himanshu.portfolio_risk_analytics.dto.RiskMetricsDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.stat.correlation.Covariance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FIXED:
 * 1. Added division by zero protection in beta calculation
 * 2. Made risk-free rate configurable (was hardcoded 4.5%)
 * 3. Added NaN/Infinity validation
 * 4. Better error messages
 */
@Slf4j
@Service
public class RiskService {
    private final StockDataService stockDataService;

    /**
     * FIXED: Risk-free rate is now configurable via environment
     * Default: 4.5% (can be updated daily)
     */
    @Value("${app.risk-free-rate:0.045}")
    private double riskFreeRate;

    private static final String DEFAULT_BENCHMARK = "SPY"; // S&P 500 Proxy

    public RiskService(StockDataService stockDataService) {
        this.stockDataService = stockDataService;
    }

    public RiskMetricsDto calculateRiskMetrics(List<String> tickers, double[] weights) {
        if (tickers == null || tickers.isEmpty() || weights == null || tickers.size() != weights.length) {
            throw new IllegalArgumentException("Invalid tickers or weights provided.");
        }

        // 1. Fetch data for assets and the systemic benchmark
        String benchmarkTicker = "SPY";
        Map<String, Map<String, Double>> returnsMap = new HashMap<>();
        for (String ticker : tickers) {
            returnsMap.put(ticker, stockDataService.getStockData(ticker, "US").getDailyReturns());
        }
        Map<String, Double> benchmarkReturns = stockDataService.getStockData(benchmarkTicker, "US").getDailyReturns();

        // 2. Temporal Alignment (Intersection of all shared trading dates)
        Set<String> commonDates = new HashSet<>(benchmarkReturns.keySet());
        for (String ticker : tickers) {
            commonDates.retainAll(returnsMap.get(ticker).keySet());
        }
        List<String> sortedDates = commonDates.stream().sorted().collect(Collectors.toList());

        if (sortedDates.size() < 10) {
            throw new RuntimeException("Insufficient synchronized data points across assets.");
        }

        // 3. Build Synchronized Matrix
        int T = sortedDates.size();
        int N = tickers.size();
        double[][] data = new double[T][N];
        double[] benchData = new double[T];

        for (int j = 0; j < T; j++) {
            String date = sortedDates.get(j);
            benchData[j] = benchmarkReturns.get(date);
            for (int i = 0; i < N; i++) {
                data[j][i] = returnsMap.get(tickers.get(i)).get(date);
            }
        }

        RealMatrix matrix = new Array2DRowRealMatrix(data);
        RealMatrix covMatrix = new Covariance(matrix).getCovarianceMatrix();

        // 4. Annualized Portfolio Risk Calculations
        RealVector w = new ArrayRealVector(weights);
        double dailyVariance = w.dotProduct(covMatrix.operate(w));
        double annVariance = dailyVariance * 252; // Scale variance to annual
        double annVol = Math.sqrt(annVariance); // Volatility as sqrt of annualized variance

        // Validate volatility
        if (Double.isNaN(annVol) || Double.isInfinite(annVol)) {
            log.warn("Invalid volatility calculated: {}", annVol);
            annVol = 0.0;
        }

        double annReturn = calculateAnnualizedReturn(data, weights);

        // 5. Advanced Risk Metrics (FIXED: added error checking)
        double beta = calculateRobustBeta(data, weights, benchData); // FIXED: division by zero protection
        double sortino = calculateSortinoRatio(data, weights, annReturn);
        double var95 = calculateParametricVaR(annReturn, annVol, 1.645); // 95% Confidence

        // 6. Individual Asset Volatilities
        Map<String, Double> assetVolatilities = new HashMap<>();
        for (int i = 0; i < N; i++) {
            double assetDailyVar = covMatrix.getEntry(i, i);
            assetVolatilities.put(tickers.get(i), Math.sqrt(assetDailyVar * 252));
        }

        // 7. Correlation Matrix
        double[][] correlation;
        if (N > 1) {
            correlation = new org.apache.commons.math3.stat.correlation.PearsonsCorrelation(matrix)
                    .getCorrelationMatrix().getData();
        } else {
            // Single asset: correlation with itself is always 1.0
            correlation = new double[][]{{1.0}};
        }

        // 8. Fully Mapped DTO
        return RiskMetricsDto.builder()
                .tickers(tickers.toArray(new String[0]))
                .variance(annVariance)
                .volatility(annVol)
                .expectedReturn(annReturn)
                .sharpeRatio((annReturn - riskFreeRate) / (annVol > 0 ? annVol : 1.0))
                .sortinoRatio(sortino)
                .valueAtRisk95(var95)
                .beta(beta)
                .assetVolatilities(assetVolatilities)
                .correlationMatrix(correlation)
                .correlationMatrixAsString(formatMatrix(correlation))
                .build();
    }

    private double calculateAnnualizedReturn(double[][] data, double[] weights) {
        double meanDailyPortfolioReturn = 0;
        for (int i = 0; i < weights.length; i++) {
            double sum = 0;
            for (int t = 0; t < data.length; t++) sum += data[t][i];
            meanDailyPortfolioReturn += (sum / data.length) * weights[i];
        }
        return meanDailyPortfolioReturn * 252;
    }

    /**
     * FIXED: Added division by zero protection
     * Returns 0 if benchmark variance is zero or invalid
     */
    private double calculateRobustBeta(double[][] data, double[] weights, double[] benchData) {
        double[] portReturns = new double[data.length];
        for (int t = 0; t < data.length; t++) {
            for (int i = 0; i < weights.length; i++) portReturns[t] += data[t][i] * weights[i];
        }

        RealMatrix combined = new Array2DRowRealMatrix(data.length, 2);
        combined.setColumn(0, portReturns);
        combined.setColumn(1, benchData);

        RealMatrix cov = new Covariance(combined).getCovarianceMatrix();

        double benchVariance = cov.getEntry(1, 1);

        // FIXED: Protect against division by zero
        if (benchVariance == 0 || Double.isNaN(benchVariance) || Double.isInfinite(benchVariance)) {
            log.warn("Invalid benchmark variance: {}", benchVariance);
            return 0.0; // Return 0 instead of crashing
        }

        double covariance = cov.getEntry(0, 1);
        double beta = covariance / benchVariance;

        // Validate result
        if (Double.isNaN(beta) || Double.isInfinite(beta)) {
            log.warn("Invalid beta calculated: {}", beta);
            return 0.0;
        }

        return beta;
    }

    private double calculateSortinoRatio(double[][] data, double[] weights, double annReturn) {
        double downsideDevSum = 0;
        for (int t = 0; t < data.length; t++) {
            double rp = 0;
            for (int i = 0; i < weights.length; i++) rp += data[t][i] * weights[i];
            if (rp < 0) downsideDevSum += Math.pow(rp, 2);
        }
        double annDownsideDev = Math.sqrt(downsideDevSum / data.length) * Math.sqrt(252);
        return annDownsideDev == 0 ? 0 : (annReturn - riskFreeRate) / annDownsideDev;
    }

    private double calculateParametricVaR(double annReturn, double annVol, double zScore) {
        return Math.abs(annReturn - (zScore * annVol));
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