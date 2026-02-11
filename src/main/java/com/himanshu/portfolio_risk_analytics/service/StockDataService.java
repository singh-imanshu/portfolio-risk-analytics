package com.himanshu.portfolio_risk_analytics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.himanshu.portfolio_risk_analytics.entity.StockData;
import com.himanshu.portfolio_risk_analytics.repository.StockDataRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.logging.Level;
@Service
public class StockDataService {
    private static final Logger logger = Logger.getLogger(StockDataService.class.getName());
    private static final int MIN_DATA_POINTS = 10;
    private static final long API_CALL_DELAY_MS = 13000; // 13 seconds to respect 5 calls/minute limit
    private static final int CACHE_SIZE_LIMIT = 500;
    @Value("${app.alphavantage.api-key}")
    private String apiKey;

    private final StockDataRepository stockDataRepository;
    private final RestClient restClient;
    private final ObjectMapper mapper;
    private final Map<String, StockData> memoryCache;

    public StockDataService(StockDataRepository stockDataRepository) {
        this.stockDataRepository = stockDataRepository;
        this.restClient = RestClient.create();
        this.mapper = new ObjectMapper();
        this.memoryCache = new ConcurrentHashMap<>();
    }

    public Double getCurrentPrice(String ticker, String market) {
        StockData stockData = getStockData(ticker, market);
        return stockData.getCurrentPrice();
    }

    public double[] getDailyReturns(String ticker, String market) {
        StockData stockData = getStockData(ticker, market);
        return stockData.getDailyReturns().values().stream()
                .mapToDouble(Double::doubleValue)
                .toArray();
    }

    public StockData getStockData(String ticker, String market) {
        // Check memory cache first
        String cacheKey = ticker + "_" + market;
        if (memoryCache.containsKey(cacheKey)) {
            StockData cached = memoryCache.get(cacheKey);
            if (!cached.needsRefresh()) {
                logger.log(Level.FINE, "Cache hit for " + cacheKey);
                return cached;
            }
        }

        // Check MongoDB
        Optional<StockData> dbData = stockDataRepository.findByTickerAndMarket(ticker, market);
        if (dbData.isPresent() && !dbData.get().needsRefresh()) {
            logger.log(Level.FINE, "Database hit for " + cacheKey);
            memoryCache.put(cacheKey, dbData.get());
            return dbData.get();
        }

        // Fetch from API
        logger.log(Level.INFO, "Fetching from Alpha Vantage: " + cacheKey);
        StockData stockData = fetchFromAlphaVantage(ticker, market);

        // Save to database
        StockData saved = stockDataRepository.save(stockData);

        // Update memory cache
        if (memoryCache.size() >= CACHE_SIZE_LIMIT) {
            memoryCache.clear(); // Simple eviction policy
        }
        memoryCache.put(cacheKey, saved);

        return saved;
    }

    private StockData fetchFromAlphaVantage(String ticker, String market) {
        if (ticker == null || ticker.trim().isEmpty()) {
            throw new IllegalArgumentException("Ticker cannot be null or empty");
        }

        // Adjust ticker for market
        String adjustedTicker = ticker.trim().toUpperCase();
        if ("INDIA".equalsIgnoreCase(market) && !adjustedTicker.contains(".")) {
            adjustedTicker = adjustedTicker + ".NSE"; // Default to NSE for Indian stocks
        }

        String url = String.format(
                "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=%s&apikey=%s&outputsize=full",
                adjustedTicker, apiKey);

        try {
            logger.log(Level.INFO, "Fetching data from Alpha Vantage for: " + adjustedTicker);

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
                throw new RuntimeException("Alpha Vantage rate limit: " + root.get("Note").asText());
            }

            if (root.has("Error Message")) {
                throw new RuntimeException("Invalid ticker: " + adjustedTicker);
            }

            // Extract time series data
            JsonNode timeSeries = root.path("Time Series (Daily)");
            if (timeSeries.isMissingNode()) {
                throw new RuntimeException("No historical data for " + adjustedTicker);
            }

            // Parse data
            List<String> dates = new ArrayList<>();
            timeSeries.fieldNames().forEachRemaining(dates::add);
            Collections.sort(dates);

            if (dates.size() < MIN_DATA_POINTS) {
                throw new RuntimeException("Insufficient data points");
            }

            // Calculate returns
            Map<String, Double> dailyReturns = new LinkedHashMap<>();
            List<Double> prices = new ArrayList<>();

            for (String date : dates) {
                double closePrice = timeSeries.path(date).path("4. close").asDouble();
                if (closePrice > 0) {
                    prices.add(closePrice);
                }
            }

            for (int i = 1; i < prices.size(); i++) {
                double ret = (prices.get(i) - prices.get(i - 1)) / prices.get(i - 1);
                dailyReturns.put(dates.get(i), ret);
            }

            // Get latest price data
            Map.Entry<String, JsonNode> latestEntry = timeSeries.fields().next();
            JsonNode latestDate = latestEntry.getValue();
            double closePrice = latestDate.path("4. close").asDouble();
            double openPrice = latestDate.path("1. open").asDouble();
            double highPrice = latestDate.path("2. high").asDouble();
            double lowPrice = latestDate.path("3. low").asDouble();
            long volume = latestDate.path("5. volume").asLong();

            StockData stockData = StockData.builder()
                    .ticker(ticker.toUpperCase())
                    .market(market)
                    .dailyReturns(dailyReturns)
                    .currentPrice(closePrice)
                    .openPrice(openPrice)
                    .highPrice(highPrice)
                    .lowPrice(lowPrice)
                    .volume(volume)
                    .dataDate(LocalDateTime.now())
                    .lastUpdatedAt(LocalDateTime.now())
                    .nextRefreshAt(LocalDateTime.now().plusDays(1))
                    .source("ALPHA_VANTAGE")
                    .isStale(false)
                    .build();

            logger.log(Level.INFO, "Successfully fetched data for " + ticker);
            return stockData;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to fetch data: " + e.getMessage());
            throw new RuntimeException("Failed to fetch data for " + ticker, e);
        }
    }

    public void refreshStaleData() {
        List<StockData> staleData = stockDataRepository.findStaleData(LocalDateTime.now());
        for (StockData data : staleData) {
            try {
                StockData refreshed = fetchFromAlphaVantage(data.getTicker(), data.getMarket());
                stockDataRepository.save(refreshed);
                logger.log(Level.INFO, "Refreshed data for " + data.getTicker());
                Thread.sleep(API_CALL_DELAY_MS); // Rate limiting
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to refresh " + data.getTicker());
            }
        }
    }
}