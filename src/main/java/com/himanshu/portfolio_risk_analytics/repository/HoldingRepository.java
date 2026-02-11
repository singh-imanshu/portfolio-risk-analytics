package com.himanshu.portfolio_risk_analytics.repository;
import com.himanshu.portfolio_risk_analytics.entity.Holding;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface HoldingRepository extends MongoRepository<Holding, String> {

    List<Holding> findByPortfolioId(String portfolioId);
    Optional<Holding> findByIdAndPortfolioId(String id, String portfolioId);
    Optional<Holding> findByPortfolioIdAndTicker(String portfolioId, String ticker);
    boolean existsByPortfolioIdAndTicker(String portfolioId, String ticker);
}
