package com.himanshu.portfolio_risk_analytics.repository;

import com.himanshu.portfolio_risk_analytics.model.Portfolio;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface PortfolioRepository extends MongoRepository<Portfolio, String> {

    /**
     * Find all portfolios for a specific user
     */
    List<Portfolio> findByUserId(String userId);

    /**
     * Find a portfolio by userId (assuming one portfolio per user)
     */
    Optional<Portfolio> findFirstByUserId(String userId);

    /**
     * Check if a portfolio exists for a user
     */
    boolean existsByUserId(String userId);
}