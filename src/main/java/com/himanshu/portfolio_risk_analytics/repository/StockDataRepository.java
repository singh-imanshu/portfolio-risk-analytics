package com.himanshu.portfolio_risk_analytics.repository;

import com.himanshu.portfolio_risk_analytics.entity.StockData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;

@Repository public interface StockDataRepository extends MongoRepository<StockData, String> { Optional<StockData> findByTickerAndMarket(String ticker, String market); List<StockData> findByIsStale(boolean isStale);

    @Query("{'nextRefreshAt': {$lt: ?0}}")
    List<StockData> findStaleData(LocalDateTime now);

    Optional<StockData> findFirstByTickerAndMarketOrderByLastUpdatedAtDesc(String ticker, String market);
}