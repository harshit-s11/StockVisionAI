package com.stockvision.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TradeRequest {

    @NotBlank(message = "Stock symbol is required")
    @Size(max = 20, message = "Stock symbol cannot exceed 20 characters")
    private String symbol;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    public TradeRequest() {}

    public TradeRequest(String symbol, Integer quantity) {
        this.symbol = symbol;
        this.quantity = quantity;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}