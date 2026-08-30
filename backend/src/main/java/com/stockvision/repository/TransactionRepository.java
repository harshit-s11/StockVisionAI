package com.stockvision.repository;

import com.stockvision.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {
    List<TransactionEntity> findTop50ByPortfolioIdOrderByCreatedAtDesc(UUID portfolioId);
    List<TransactionEntity> findByPortfolioIdOrderByCreatedAtDesc(UUID portfolioId);
}