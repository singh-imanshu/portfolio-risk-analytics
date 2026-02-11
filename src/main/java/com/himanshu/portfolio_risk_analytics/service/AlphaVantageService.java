package com.himanshu.portfolio_risk_analytics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.*;
import java.util.logging.Logger;

@Service
public class AlphaVantageService {

    private static final Logger logger = Logger.getLogger(AlphaVantageService.class.getName());
    private static final int MIN_DATA_POINTS = 5;

    @Value("${app.alphavantage.api-key}")
    private String apiKey;

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper mapper = new ObjectMapper();

    public double[] getDailyReturns(String ticker) {
        if (ticker == null || ticker.trim().isEmpty()) {
            throw new IllegalArgumentException("Ticker cannot be null or empty");
        }

        String url = String.format(
                "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=%s&apikey=%s",
                ticker.trim(), apiKey);

        try {
            logger.info("Fetching data from Alpha Vantage for: " + ticker);

            String response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            if (response == null || response.trim().isEmpty()) {
                throw new RuntimeException("Empty response from Alpha Vantage API");
            }

            JsonNode root = mapper.readTree(response);

            // Check for rate limits
            if (root.has("Note")) {
                String msg = root.get("Note").asText();
                logger.warning("API Rate Limit: " + msg);
                throw new RuntimeException("Alpha Vantage rate limit exceeded: " + msg);
            }

            // Check for information messages
            if (root.has("Information")) {
                String msg = root.get("Information").asText();
                logger.warning("API Information: " + msg);
                throw new RuntimeException("Alpha Vantage API message: " + msg);
            }

            // Check for error messages
            if (root.has("Error Message")) {
                logger.warning("Invalid ticker: " + ticker);
                throw new RuntimeException("Invalid Ticker: " + ticker +
                        ". Try adding .NSE suffix for Indian stocks or .BSE for other markets.");
            }

            // Extract time series data
            JsonNode timeSeries = root.path("Time Series (Daily)");
            if (timeSeries.isMissingNode() || !timeSeries.isObject()) {
                logger.warning("No time series data found for: " + ticker);
                throw new RuntimeException("No historical data available for " + ticker);
            }

            // Extract and sort dates
            List<String> dates = new ArrayList<>();
            timeSeries.fieldNames().forEachRemaining(dates::add);

            if (dates.isEmpty()) {
                throw new RuntimeException("No data points available for " + ticker);
            }

            Collections.sort(dates);

            // Extract closing prices
            List<Double> prices = new ArrayList<>();
            for (String date : dates) {
                double closePrice = timeSeries.path(date).path("4. close").asDouble();
                if (closePrice > 0) {
                    prices.add(closePrice);
                }
            }

            if (prices.size() < MIN_DATA_POINTS) {
                throw new RuntimeException("Insufficient data points for " + ticker +
                        ". Need at least " + MIN_DATA_POINTS);
            }

            // Calculate percentage returns
            double[] returns = new double[prices.size() - 1];
            for (int i = 1; i < prices.size(); i++) {
                double prevPrice = prices.get(i - 1);
                double currPrice = prices.get(i);
                returns[i - 1] = (currPrice - prevPrice) / prevPrice;
            }

            logger.info("Successfully calculated " + returns.length + " returns for " + ticker);
            return returns;

        } catch (Exception e) {
            logger.severe("Failed to fetch data for " + ticker + ": " + e.getMessage());
            throw new RuntimeException("Failed to fetch data for " + ticker + ": " + e.getMessage(), e);
        }
    }
}