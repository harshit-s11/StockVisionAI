package com.stockvision.dto;

import java.math.BigDecimal;

public class HoldingResponse {

    private String symbol;
    private String companyName;
    private Integer quantity;
    private BigDecimal averageBuyPrice;
    private BigDecimal currentPrice;
    private BigDecimal currentValue;
    private BigDecimal unrealizedPnL;
    private BigDecimal allocationPercent;

    public HoldingResponse() {}

    public HoldingResponse(String symbol, String companyName, Integer quantity,
                           BigDecimal averageBuyPrice, BigDecimal currentPrice,
                           BigDecimal currentValue, BigDecimal unrealizedPnL,
                           BigDecimal allocationPercent) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.quantity = quantity;
        this.averageBuyPrice = averageBuyPrice;
        this.currentPrice = currentPrice;
        this.currentValue = currentValue;
        this.unrealizedPnL = unrealizedPnL;
        this.allocationPercent = allocationPercent;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getAverageBuyPrice() { return averageBuyPrice; }
    public void setAverageBuyPrice(BigDecimal averageBuyPrice) { this.averageBuyPrice = averageBuyPrice; }

    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

    public BigDecimal getCurrentValue() { return currentValue; }
    public void setCurrentValue(BigDecimal currentValue) { this.currentValue = currentValue; }

    public BigDecimal getUnrealizedPnL() { return unrealizedPnL; }
    public void setUnrealizedPnL(BigDecimal unrealizedPnL) { this.unrealizedPnL = unrealizedPnL; }

    public BigDecimal getAllocationPercent() { return allocationPercent; }
    public void setAllocationPercent(BigDecimal allocationPercent) { this.allocationPercent = allocationPercent; }
}