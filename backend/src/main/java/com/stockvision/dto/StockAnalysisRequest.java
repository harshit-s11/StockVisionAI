package com.stockvision.dto;

import jakarta.validation.constraints.NotBlank;

public class StockAnalysisRequest {

    @NotBlank(message = "Stock symbol cannot be empty")
    private String symbol;

    public StockAnalysisRequest() {}

    public StockAnalysisRequest(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
}