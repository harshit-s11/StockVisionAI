package com.stockvision.dto;

import com.stockvision.entity.StockEntity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public class StockResponse {

    private String symbol;
    private String companyName;
    private String sector;
    private BigDecimal currentPrice;
    private BigDecimal previousClose;
    private BigDecimal change;
    private BigDecimal changePercent;
    private String dataStatus = "DEMO";
    private Instant updatedAt;

    public StockResponse() {}

    public StockResponse(String symbol, String companyName, String sector, BigDecimal currentPrice,
                         BigDecimal previousClose, BigDecimal change, BigDecimal changePercent,
                         String dataStatus, Instant updatedAt) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.sector = sector;
        this.currentPrice = currentPrice;
        this.previousClose = previousClose;
        this.change = change;
        this.changePercent = changePercent;
        this.dataStatus = dataStatus != null ? dataStatus : "DEMO";
        this.updatedAt = updatedAt;
    }

    public static StockResponse fromEntity(StockEntity stock) {
        BigDecimal current = stock.getCurrentPrice().setScale(2, RoundingMode.HALF_UP);
        BigDecimal previous = stock.getPreviousClose().setScale(2, RoundingMode.HALF_UP);
        BigDecimal change = current.subtract(previous).setScale(2, RoundingMode.HALF_UP);
        BigDecimal changePercent;
        if (previous.compareTo(BigDecimal.ZERO) > 0) {
            changePercent = change
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
        } else {
            changePercent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return new StockResponse(
            stock.getSymbol(),
            stock.getCompanyName(),
            stock.getSector(),
            current,
            previous,
            change,
            changePercent,
            "DEMO",
            stock.getUpdatedAt()
        );
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

    public BigDecimal getPreviousClose() { return previousClose; }
    public void setPreviousClose(BigDecimal previousClose) { this.previousClose = previousClose; }

    public BigDecimal getChange() { return change; }
    public void setChange(BigDecimal change) { this.change = change; }

    public BigDecimal getChangePercent() { return changePercent; }
    public void setChangePercent(BigDecimal changePercent) { this.changePercent = changePercent; }

    public String getDataStatus() { return dataStatus; }
    public void setDataStatus(String dataStatus) { this.dataStatus = dataStatus; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}