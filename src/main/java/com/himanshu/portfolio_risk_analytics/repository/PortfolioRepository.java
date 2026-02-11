package com.himanshu.portfolio_risk_analytics.repository;
import com.himanshu.portfolio_risk_analytics.entity.Portfolio;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface PortfolioRepository extends MongoRepository<Portfolio, String> {

    List<Portfolio> findByUserId(String userId);
    Optional<Portfolio> findByIdAndUserId(String id, String userId);
    boolean existsByIdAndUserId(String id, String userId);
    List<Portfolio> findByUserIdAndIsActive(String userId, boolean isActive);
}
