package com.himanshu.portfolio_risk_analytics.service;

import com.himanshu.portfolio_risk_analytics.model.Portfolio;
import com.himanshu.portfolio_risk_analytics.repository.PortfolioRepository;
import jakarta.annotation.PostConstruct;
import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.Covariance;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.logging.Logger;

@Service
public class RiskService {

    private static final Logger logger = Logger.getLogger(RiskService.class.getName());
    private final PortfolioRepository repository;

    public RiskService(PortfolioRepository repository) {
        this.repository = repository;
    }

    // SEEDER: Runs on startup to populate MongoDB if empty
    @PostConstruct
    public void seedDatabase() {
        try {
            if (repository.count() == 0) {
                Portfolio portfolio = new Portfolio();
                portfolio.setUserId("dev-user");
                portfolio.setHistoricalReturns(Map.of(
                        "RELIANCE", new double[]{0.012, -0.005, 0.008, 0.021, -0.010},
                        "TCS", new double[]{0.005, 0.002, -0.003, 0.007, 0.001}
                ));
                repository.save(portfolio);
                logger.info("✓ MongoDB seeded with dummy stock data.");
            }
        } catch (Exception e) {
            logger.warning("Failed to seed database: " + e.getMessage());
        }
    }

    public Map<String, Object> calculateRisk(Map<String, double[]> returns, double[] weights) {
        // Validate inputs
        if (returns == null || returns.isEmpty()) {
            throw new IllegalArgumentException("Returns map cannot be null or empty");
        }
        if (weights == null || weights.length == 0) {
            throw new IllegalArgumentException("Weights array cannot be null or empty");
        }
        if (returns.size() != weights.length) {
            throw new IllegalArgumentException("Number of weights must match number of tickers");
        }

        String[] tickers = returns.keySet().toArray(new String[0]);
        int numStocks = tickers.length;
        int numDays = returns.get(tickers[0]).length;

        // Validate that all stocks have the same number of data points
        for (String ticker : tickers) {
            if (returns.get(ticker).length != numDays) {
                throw new IllegalArgumentException("All stocks must have the same number of returns");
            }
        }

        // Build data matrix
        double[][] data = new double[numDays][numStocks];
        for (int i = 0; i < numStocks; i++) {
            double[] stockRet = returns.get(tickers[i]);
            for (int j = 0; j < numDays; j++) {
                data[j][i] = stockRet[j];
            }
        }

        RealMatrix matrix = new Array2DRowRealMatrix(data);

        // Calculate correlation matrix
        double[][] correlation = new PearsonsCorrelation(matrix).getCorrelationMatrix().getData();

        // Calculate portfolio variance: w^T * Cov * w
        RealMatrix cov = new Covariance(matrix).getCovarianceMatrix();
        RealVector w = new ArrayRealVector(weights);
        double variance = w.dotProduct(cov.operate(w));
        double volatility = Math.sqrt(variance);

        // Log results
        logger.info("Portfolio calculated - Tickers: " + Arrays.toString(tickers) +
                ", Volatility: " + volatility);

        return Map.of(
                "tickers", tickers,
                "correlation", correlation,
                "variance", variance,
                "volatility", volatility
        );
    }
}