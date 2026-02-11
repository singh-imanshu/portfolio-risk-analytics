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
    private static final long API_CALL_DELAY_MS = 13000; // 13 seconds for free tier (5 calls/min)
    private static final int CACHE_SIZE_LIMIT = 500;

    @Value("${app.alphavantage.api-key:demo}")
    private String apiKey;

    private final StockDataRepository stockDataRepository;
    private final RestClient restClient;
    private final ObjectMapper mapper;
    private final Map<String, StockData> memoryCache;
    private long lastApiCallTime = 0;

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

        // Fetch from API with rate limiting
        logger.log(Level.INFO, "Fetching from Alpha Vantage: " + cacheKey);
        enforceRateLimit();

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

    private void enforceRateLimit() {
        long now = System.currentTimeMillis();
        long timeSinceLastCall = now - lastApiCallTime;

        if (timeSinceLastCall < API_CALL_DELAY_MS) {
            long sleepTime = API_CALL_DELAY_MS - timeSinceLastCall;
            logger.log(Level.INFO, "Rate limiting: waiting " + sleepTime + "ms");
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastApiCallTime = System.currentTimeMillis();
    }

    private StockData fetchFromAlphaVantage(String ticker, String market) {
        if (ticker == null || ticker.trim().isEmpty()) {
            throw new IllegalArgumentException("Ticker cannot be null or empty");
        }

        // Adjust ticker for market
        String adjustedTicker = ticker.trim().toUpperCase();
        if ("INDIA".equalsIgnoreCase(market) && !adjustedTicker.contains(".")) {
            adjustedTicker = adjustedTicker + ".NSE";
        }

        // Try compact first, then full if needed
        StockData stockData = tryFetchWithOutputSize(adjustedTicker, market, "compact");

        if (stockData == null || stockData.getDailyReturns().size() < MIN_DATA_POINTS) {
            logger.log(Level.WARNING, "Compact output insufficient for " + adjustedTicker +
                    ", retrying with full output");
            enforceRateLimit();
            stockData = tryFetchWithOutputSize(adjustedTicker, market, "full");
        }

        if (stockData == null || stockData.getDailyReturns().isEmpty()) {
            throw new RuntimeException("No data available for " + ticker +
                    ". Ensure the ticker is valid and has historical data.");
        }

        return stockData;
    }

    private StockData tryFetchWithOutputSize(String adjustedTicker, String market, String outputSize) {
        String url = String.format(
                "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=%s&outputsize=%s&apikey=%s",
                adjustedTicker, outputSize, apiKey);

        try {
            logger.log(Level.INFO, "Fetching " + outputSize + " data for: " + adjustedTicker);

            String response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            if (response == null || response.trim().isEmpty()) {
                throw new RuntimeException("Empty response from Alpha Vantage API");
            }

            return parseAlphaVantageResponse(response, adjustedTicker, market, url);

        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to fetch with " + outputSize + " output: " + e.getMessage());
            return null;
        }
    }

    private StockData parseAlphaVantageResponse(String response, String adjustedTicker,
                                                String market, String url) throws Exception {
        JsonNode root = mapper.readTree(response);

        // Check for rate limits
        if (root.has("Note")) {
            String note = root.get("Note").asText();
            logger.log(Level.WARNING, "Alpha Vantage rate limit: " + note);
            throw new RuntimeException("API rate limit reached. Free tier: 5 requests/minute. " + note);
        }

        if (root.has("Information")) {
            String info = root.get("Information").asText();
            logger.log(Level.WARNING, "API Information: " + info);
            throw new RuntimeException("API message: " + info);
        }

        // Check for error messages
        if (root.has("Error Message")) {
            String error = root.get("Error Message").asText();
            logger.log(Level.WARNING, "API Error for " + adjustedTicker + ": " + error);
            throw new RuntimeException("Invalid ticker or API error: " + error);
        }

        // Extract time series data
        JsonNode timeSeries = root.path("Time Series (Daily)");
        if (timeSeries.isMissingNode() || !timeSeries.isObject()) {
            logger.log(Level.WARNING, "No time series data found for: " + adjustedTicker);
            logger.log(Level.INFO, "Response preview: " + response.substring(0, Math.min(500, response.length())));
            throw new RuntimeException("No historical data available for " + adjustedTicker);
        }

        // Parse data
        List<String> dates = new ArrayList<>();
        timeSeries.fieldNames().forEachRemaining(dates::add);
        Collections.sort(dates);

        if (dates.isEmpty()) {
            throw new RuntimeException("No data points available for " + adjustedTicker);
        }

        logger.log(Level.INFO, "Found " + dates.size() + " dates for " + adjustedTicker);

        // Extract prices and calculate returns
        Map<String, Double> dailyReturns = new LinkedHashMap<>();
        List<Double> prices = new ArrayList<>();

        for (String date : dates) {
            double closePrice = timeSeries.path(date).path("4. close").asDouble();
            if (closePrice > 0) {
                prices.add(closePrice);
            }
        }

        if (prices.size() < MIN_DATA_POINTS) {
            throw new RuntimeException("Insufficient valid data points for " + adjustedTicker +
                    ". Found: " + prices.size() + ", Need: " + MIN_DATA_POINTS);
        }

        // Calculate returns
        for (int i = 1; i < prices.size(); i++) {
            double ret = (prices.get(i) - prices.get(i - 1)) / prices.get(i - 1);
            dailyReturns.put(dates.get(i), ret);
        }

        // Get latest price data (most recent date)
        String latestDate = dates.get(dates.size() - 1);
        JsonNode latestData = timeSeries.path(latestDate);

        double closePrice = latestData.path("4. close").asDouble();
        double openPrice = latestData.path("1. open").asDouble();
        double highPrice = latestData.path("2. high").asDouble();
        double lowPrice = latestData.path("3. low").asDouble();
        long volume = latestData.path("5. volume").asLong();

        StockData stockData = StockData.builder()
                .ticker(adjustedTicker.split("\\.")[0]) // Remove market suffix for storage
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

        logger.log(Level.INFO, "Successfully parsed data for " + adjustedTicker +
                " with " + dailyReturns.size() + " daily returns");
        return stockData;
    }

    public void refreshStaleData() {
        List<StockData> staleData = stockDataRepository.findStaleData(LocalDateTime.now());
        for (StockData data : staleData) {
            try {
                enforceRateLimit();
                StockData refreshed = fetchFromAlphaVantage(data.getTicker(), data.getMarket());
                stockDataRepository.save(refreshed);
                logger.log(Level.INFO, "Refr    eshed data for " + data.getTicker());
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to refresh " + data.getTicker() + ": " + e.getMessage());
            }
        }
    }
}