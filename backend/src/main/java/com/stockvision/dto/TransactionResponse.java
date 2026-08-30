package com.stockvision.dto;

import com.stockvision.entity.TransactionEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TransactionResponse {

    private UUID id;
    private String symbol;
    private String type;
    private Integer quantity;
    private BigDecimal executionPrice;
    private BigDecimal totalValue;
    private Instant timestamp;

    public TransactionResponse() {}

    public TransactionResponse(UUID id, String symbol, String type, Integer quantity,
                               BigDecimal executionPrice, BigDecimal totalValue, Instant timestamp) {
        this.id = id;
        this.symbol = symbol;
        this.type = type;
        this.quantity = quantity;
        this.executionPrice = executionPrice;
        this.totalValue = totalValue;
        this.timestamp = timestamp;
    }

    public static TransactionResponse fromEntity(TransactionEntity entity) {
        String sym = entity.getStock() != null ? entity.getStock().getSymbol() : "";
        return new TransactionResponse(
            entity.getId(),
            sym,
            entity.getType() != null ? entity.getType().name() : "BUY",
            entity.getQuantity(),
            entity.getExecutionPrice(),
            entity.getTotalValue(),
            entity.getCreatedAt()
        );
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getExecutionPrice() { return executionPrice; }
    public void setExecutionPrice(BigDecimal executionPrice) { this.executionPrice = executionPrice; }

    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}