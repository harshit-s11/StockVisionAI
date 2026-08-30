package com.stockvision.repository;

import com.stockvision.entity.TransactionEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {
    List<TransactionEntity> findTop50ByPortfolioIdOrderByCreatedAtDesc(UUID portfolioId);

    @Query("SELECT t FROM TransactionEntity t JOIN FETCH t.stock WHERE t.portfolio.id = :portfolioId ORDER BY t.createdAt DESC")
    List<TransactionEntity> findTop50ByPortfolioIdWithStock(@Param("portfolioId") UUID portfolioId, Pageable pageable);

    List<TransactionEntity> findByPortfolioIdOrderByCreatedAtDesc(UUID portfolioId);
}