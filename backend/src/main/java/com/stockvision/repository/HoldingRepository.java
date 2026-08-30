package com.stockvision.repository;

import com.stockvision.entity.HoldingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HoldingRepository extends JpaRepository<HoldingEntity, UUID> {
    List<HoldingEntity> findByPortfolioId(UUID portfolioId);

    @Query("SELECT h FROM HoldingEntity h JOIN FETCH h.stock WHERE h.portfolio.id = :portfolioId")
    List<HoldingEntity> findByPortfolioIdWithStock(@Param("portfolioId") UUID portfolioId);

    Optional<HoldingEntity> findByPortfolioIdAndStockId(UUID portfolioId, UUID stockId);
    Optional<HoldingEntity> findByPortfolioIdAndStockSymbol(UUID portfolioId, String symbol);
}