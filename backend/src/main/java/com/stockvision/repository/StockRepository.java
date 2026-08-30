package com.stockvision.repository;

import com.stockvision.entity.StockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockRepository extends JpaRepository<StockEntity, UUID> {
    Optional<StockEntity> findBySymbol(String symbol);
    Optional<StockEntity> findBySymbolIgnoreCase(String symbol);
    boolean existsBySymbol(String symbol);
    boolean existsBySymbolIgnoreCase(String symbol);
}