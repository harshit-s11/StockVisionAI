package com.stockvision.controller;

import com.stockvision.dto.StockResponse;
import com.stockvision.service.MarketDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stocks")
public class MarketDataController {

    private final MarketDataService marketDataService;

    public MarketDataController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @GetMapping
    public ResponseEntity<List<StockResponse>> getAllStocks() {
        List<StockResponse> stocks = marketDataService.getAllStocks();
        return ResponseEntity.ok(stocks);
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<StockResponse> getStockBySymbol(@PathVariable String symbol) {
        StockResponse stock = marketDataService.getStockBySymbol(symbol);
        return ResponseEntity.ok(stock);
    }
}