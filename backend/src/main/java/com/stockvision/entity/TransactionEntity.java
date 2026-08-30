package com.stockvision.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private PortfolioEntity portfolio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    private StockEntity stock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "execution_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal executionPrice;

    @Column(name = "total_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public TransactionEntity() {}

    public TransactionEntity(PortfolioEntity portfolio, StockEntity stock, TransactionType type, Integer quantity, BigDecimal executionPrice, BigDecimal totalValue) {
        this.portfolio = portfolio;
        this.stock = stock;
        this.type = type;
        this.quantity = quantity;
        this.executionPrice = executionPrice;
        this.totalValue = totalValue;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public PortfolioEntity getPortfolio() { return portfolio; }
    public void setPortfolio(PortfolioEntity portfolio) { this.portfolio = portfolio; }

    public StockEntity getStock() { return stock; }
    public void setStock(StockEntity stock) { this.stock = stock; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getExecutionPrice() { return executionPrice; }
    public void setExecutionPrice(BigDecimal executionPrice) { this.executionPrice = executionPrice; }

    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}