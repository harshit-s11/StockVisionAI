package com.stockvision.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "holdings", uniqueConstraints = {
    @UniqueConstraint(name = "uq_holdings_port_stock", columnNames = {"portfolio_id", "stock_id"})
})
public class HoldingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private PortfolioEntity portfolio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    private StockEntity stock;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "average_buy_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal averageBuyPrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public HoldingEntity() {}

    public HoldingEntity(PortfolioEntity portfolio, StockEntity stock, Integer quantity, BigDecimal averageBuyPrice) {
        this.portfolio = portfolio;
        this.stock = stock;
        this.quantity = quantity;
        this.averageBuyPrice = averageBuyPrice;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public PortfolioEntity getPortfolio() { return portfolio; }
    public void setPortfolio(PortfolioEntity portfolio) { this.portfolio = portfolio; }

    public StockEntity getStock() { return stock; }
    public void setStock(StockEntity stock) { this.stock = stock; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getAverageBuyPrice() { return averageBuyPrice; }
    public void setAverageBuyPrice(BigDecimal averageBuyPrice) { this.averageBuyPrice = averageBuyPrice; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}