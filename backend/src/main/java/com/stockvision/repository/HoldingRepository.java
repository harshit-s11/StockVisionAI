package com.stockvision.repository;

import com.stockvision.entity.HoldingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HoldingRepository extends JpaRepository<HoldingEntity, UUID> {
    List<HoldingEntity> findByPortfolioId(UUID portfolioId);
    Optional<HoldingEntity> findByPortfolioIdAndStockId(UUID portfolioId, UUID stockId);
    Optional<HoldingEntity> findByPortfolioIdAndStockSymbol(UUID portfolioId, String symbol);
}