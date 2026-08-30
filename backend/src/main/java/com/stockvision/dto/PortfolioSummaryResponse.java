package com.stockvision.dto;

import java.math.BigDecimal;

public class PortfolioSummaryResponse {

    private BigDecimal cashBalance;
    private BigDecimal totalInvested;
    private BigDecimal currentHoldingsValue;
    private BigDecimal totalPortfolioValue;
    private BigDecimal unrealizedPnL;
    private BigDecimal unrealizedPnLPercent;
    private BigDecimal portfolioReturnPercent;

    public PortfolioSummaryResponse() {}

    public PortfolioSummaryResponse(BigDecimal cashBalance, BigDecimal totalInvested,
                                    BigDecimal currentHoldingsValue, BigDecimal totalPortfolioValue,
                                    BigDecimal unrealizedPnL, BigDecimal unrealizedPnLPercent,
                                    BigDecimal portfolioReturnPercent) {
        this.cashBalance = cashBalance;
        this.totalInvested = totalInvested;
        this.currentHoldingsValue = currentHoldingsValue;
        this.totalPortfolioValue = totalPortfolioValue;
        this.unrealizedPnL = unrealizedPnL;
        this.unrealizedPnLPercent = unrealizedPnLPercent;
        this.portfolioReturnPercent = portfolioReturnPercent;
    }

    public BigDecimal getCashBalance() { return cashBalance; }
    public void setCashBalance(BigDecimal cashBalance) { this.cashBalance = cashBalance; }

    public BigDecimal getTotalInvested() { return totalInvested; }
    public void setTotalInvested(BigDecimal totalInvested) { this.totalInvested = totalInvested; }

    public BigDecimal getCurrentHoldingsValue() { return currentHoldingsValue; }
    public void setCurrentHoldingsValue(BigDecimal currentHoldingsValue) { this.currentHoldingsValue = currentHoldingsValue; }

    public BigDecimal getTotalPortfolioValue() { return totalPortfolioValue; }
    public void setTotalPortfolioValue(BigDecimal totalPortfolioValue) { this.totalPortfolioValue = totalPortfolioValue; }

    public BigDecimal getUnrealizedPnL() { return unrealizedPnL; }
    public void setUnrealizedPnL(BigDecimal unrealizedPnL) { this.unrealizedPnL = unrealizedPnL; }

    public BigDecimal getUnrealizedPnLPercent() { return unrealizedPnLPercent; }
    public void setUnrealizedPnLPercent(BigDecimal unrealizedPnLPercent) { this.unrealizedPnLPercent = unrealizedPnLPercent; }

    public BigDecimal getPortfolioReturnPercent() { return portfolioReturnPercent; }
    public void setPortfolioReturnPercent(BigDecimal portfolioReturnPercent) { this.portfolioReturnPercent = portfolioReturnPercent; }
}