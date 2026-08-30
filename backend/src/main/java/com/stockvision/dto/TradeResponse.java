package com.stockvision.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TradeResponse {

    private UUID transactionId;
    private String symbol;
    private String type;
    private Integer quantity;
    private BigDecimal executionPrice;
    private BigDecimal totalValue;
    private BigDecimal cashBalance;
    private Integer holdingQuantity;
    private BigDecimal averageBuyPrice;
    private Instant timestamp;

    public TradeResponse() {}

    public TradeResponse(UUID transactionId, String symbol, String type, Integer quantity,
                         BigDecimal executionPrice, BigDecimal totalValue, BigDecimal cashBalance,
                         Integer holdingQuantity, BigDecimal averageBuyPrice, Instant timestamp) {
        this.transactionId = transactionId;
        this.symbol = symbol;
        this.type = type;
        this.quantity = quantity;
        this.executionPrice = executionPrice;
        this.totalValue = totalValue;
        this.cashBalance = cashBalance;
        this.holdingQuantity = holdingQuantity;
        this.averageBuyPrice = averageBuyPrice;
        this.timestamp = timestamp;
    }

    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }

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

    public BigDecimal getCashBalance() { return cashBalance; }
    public void setCashBalance(BigDecimal cashBalance) { this.cashBalance = cashBalance; }

    public Integer getHoldingQuantity() { return holdingQuantity; }
    public void setHoldingQuantity(Integer holdingQuantity) { this.holdingQuantity = holdingQuantity; }

    public BigDecimal getAverageBuyPrice() { return averageBuyPrice; }
    public void setAverageBuyPrice(BigDecimal averageBuyPrice) { this.averageBuyPrice = averageBuyPrice; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}